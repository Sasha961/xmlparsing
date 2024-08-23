package com.myproject.xmlparsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.xmlparsing.dto.Configuration;
import com.myproject.xmlparsing.dto.ConfigurationType;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.List;

@Slf4j
public class ParseXmlHeadersAndSave implements Runnable {

    public Thread thread;
    private final String link;
    private final UserEntity psi;
    private final PriceSenderService priceSenderService;
    private final XmlDataProcessorService xmlDataProcessorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParseXmlHeadersAndSave(String link,
                                  UserEntity psi,
                                  PriceSenderService priceSenderService,
                                  XmlDataProcessorService xmlDataProcessorService) {
        this.link = link;
        this.psi = psi;
        this.xmlDataProcessorService = xmlDataProcessorService;
        this.priceSenderService = priceSenderService;
        this.thread = new Thread(this, "ParseXmlHeadersAndSave for adminCode: " + psi.getAdminCode());
        log.debug("Constructed {}", thread.getName());
    }

    @Override
    public void run() {
        log.info("start parse xml headers");
        try {
            List<Configuration> confs = List.of(objectMapper.readValue(psi.getConfigurationsJsonString(), Configuration[].class));
            String offstingJson = confs
                    .stream()
                    .filter(c -> c.getType().equals(ConfigurationType.Offsting))
                    .findFirst()
                    .orElseThrow()
                    .getValue();
            List<Integer> offsting = List.of(objectMapper.readValue(offstingJson, Integer[].class));
            List<String> headers = xmlDataProcessorService.getPriceHeaders(new URL(link).openStream(), offsting);
            List<ClassificationItem> classificationItems = headers
                    .stream()
                    .map(ClassificationItem::fromHeader)
                    .toList();
            String classificationJson = objectMapper.writeValueAsString(classificationItems);
            priceSenderService.setClassification(psi.getAdminCode(), classificationJson);
            priceSenderService.updateState(psi.getAdminCode(), PriceState.AWAITS_CLASSIFICATION);
            log.info("finished parse xml headers");
        } catch (Exception e) {
            log.error("Can't parse xml headers with exception: {} ", e.getMessage());
        }
    }
}
