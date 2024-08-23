package com.myproject.xmlparsing;

import com.myproject.xmlparsing.dto.Configuration;
import com.myproject.xmlparsing.dto.PriceDto;
import com.myproject.xmlparsing.dto.PriceHeader;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class XmlDataProcessorService {

    private final SAXBuilder saxBuilder;

    public XmlDataProcessorService() {
        this.saxBuilder = new SAXBuilder();
    }

    /**
     * @param offsting Список, где индекс отвечает за вложеность, а значение за шаг в xml документе
     */
    public List<String> getPriceHeaders(InputStream inputStream, List<Integer> offsting) throws IOException {
        try {
            Document doc = saxBuilder.build(inputStream);
            List<Element> elements = getItemsToParse(doc, offsting);
            return elements.stream().map(Element::getName).toList();
        } catch (IOException | JDOMException exception) {
            log.error("parse fail from getPriceHeaders: {}", exception.getMessage());
            throw new NumberFormatException("Can't get xml priceHeaders");
        } finally {
            inputStream.close();
        }
    }

    public List<PriceDto> parsePriceDtos(InputStream inputStream,
                                         List<Integer> offsting,
                                         List<ClassificationItem> classificationItems,
                                         Optional<List<Configuration>> configurations,
                                         String currency) throws IOException {
        try {
            Document doc = saxBuilder.build(inputStream);
            List<Element> elements = getItemsToParse(doc, offsting);
            List<PriceDto> priceDtosList = new ArrayList<>();
            for (Element element : elements) {
                //1 offer
                PriceDto priceDto = new PriceDto();
                priceDto.setCurrency(currency);
                priceDto = fillPriceDto(priceDto, element, classificationItems);
                if (configurations.isPresent()) {
                    for (Configuration configuration : configurations.get()) {
//                        priceDto = applyConf(configuration, priceDto);
                    }
                }
                priceDtosList.add(priceDto);
            }
            return priceDtosList;
        } catch (IOException | JDOMException exception) {
            log.error("parse fail from parsePriceDtos: {}", exception.getMessage());
            throw new NumberFormatException("Can't get xml priceData");
        } finally {
            inputStream.close();
        }
    }

    private List<Element> getItemsToParse(Document doc, List<Integer> offsting) {
        List<Element> elements = doc.getRootElement().getChildren();
        for (Integer step : offsting) {
            elements = elements.get(step).getChildren();
        }
        return elements;
    }

    private PriceDto fillPriceDto(PriceDto priceDto, Element element, List<ClassificationItem> cli) {
        for (Element priceDtoPart : element.getChildren()) {
            String priceDtoPartValue = priceDtoPart.getValue();
            String item = "TRASH";
            for (ClassificationItem classificationItem : cli) {
                if (classificationItem.getPriceHeader().equals(priceDtoPart.getName())) {
                    item = classificationItem.getCategory();
                }
            }
            switch (PriceHeader.valueOf(item.toUpperCase())) {
                case PRICE -> priceDto.setPrice(priceDtoPartValue);
                case BRAND -> priceDto.setBrand(priceDtoPartValue);
                case ARTICLE -> priceDto.setArticle(priceDtoPartValue);
                case CURRENCY -> priceDto.setCurrency(priceDtoPartValue);
                case DELIVERY -> priceDto.setDelivery(priceDtoPartValue);
                case MULTIPLICITY -> priceDto.setMultiplicity(priceDtoPartValue);
                case NOTES -> priceDto.setNotes(priceDtoPartValue);
                case PARTNAME -> priceDto.setPartname(priceDtoPartValue);
                case PHOTO -> priceDto.setPhoto(priceDtoPartValue);
                case QUANTITY -> priceDto.setQuantity(priceDtoPartValue);
                case STATUS -> priceDto.setStatus(priceDtoPartValue);
                case WAREHOUSE -> priceDto.setWarehouse(priceDtoPartValue);
                default ->  item.toString();
            }
        }
        return priceDto;
    }
}
