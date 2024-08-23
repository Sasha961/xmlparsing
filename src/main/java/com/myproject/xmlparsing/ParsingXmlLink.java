package com.myproject.xmlparsing;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ParsingXmlLink implements Runnable {

    private final RedisTemplate<String, String> redisTemplate;
    private final String link;
    public Thread thread;
    private final UUID uuid;
    private XMLStreamWriter streamWriter;

    public ParsingXmlLink(@Qualifier("redis.internal") RedisTemplate<String, String> redisTemplate,
                          String link,
                          UUID uuid) {
        this.redisTemplate = redisTemplate;
        this.link = link;
        this.uuid = uuid;
        this.thread = new Thread(this, "ParsingXmlLinkTreadFor" + link);
    }

    @SneakyThrows
    @Override
    public void run() {
        log.info("parsing start");
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(new URL(link).openStream());
        StringWriter stringOut = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        streamWriter = factory.createXMLStreamWriter(stringOut);
        streamWriter.writeStartDocument();
        streamWriter.writeStartElement(document.getRootElement().getName());
        getParedDownXml(document.getRootElement().getChildren());
        streamWriter.writeEndElement();
        streamWriter.writeEndDocument();
        log.info("short xml (packed link): " + streamWriter.toString());
        log.info("size of string loaded into radish: {}", stringOut.toString().length());
        log.info("final option for redis: {}", stringOut);
        if (stringOut.toString().isBlank()) {
            redisTemplate.opsForValue().set(uuid.toString(), "blank", 60, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().set(uuid.toString(), stringOut.toString(), 60, TimeUnit.MINUTES);
        }
        stringOut.close();
        log.info("parsing end");
    }

    private void getParedDownXml(List<Element> elements) throws XMLStreamException {
        int size = Math.min(elements.size(), 5);
        for (int i = 0; i < size; i++) {
            if (elements.get(i).getChildren() != null && !elements.get(i).getChildren().isEmpty()) {
                streamWriter.writeStartElement(elements.get(i).getName());
                getParedDownXml(elements.get(i).getChildren());
                streamWriter.writeEndElement();
            } else {
                streamWriter.writeStartElement(elements.get(i).getName());
                streamWriter.writeCharacters(elements.get(i).getValue());
                streamWriter.writeEndElement();
            }
        }
    }
}
