package com.myproject.xmlparsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.xmlparsing.dto.Configuration;
import com.myproject.xmlparsing.dto.ConfigurationType;
import com.myproject.xmlparsing.dto.PriceDto;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;

@Slf4j
public class SaveNewClassificationAndUpdatePriceData implements Runnable {

    public Thread thread;
    private final ObjectMapper objectMapper;
    private final UserEntity psi;
    private final PriceSenderService priceSenderService;
    private List<ClassificationItem> newClassificationItems;
    private final XmlDataProcessorService xmlDataProcessorService;

    public SaveNewClassificationAndUpdatePriceData(String adminCode,
                                                   PriceSenderService priceSenderService,
                                                   UserEntity psi,
                                                   List<ClassificationItem> classification,
                                                   XmlDataProcessorService xmlDataProcessorService) {
        this.priceSenderService = priceSenderService;
        this.newClassificationItems = classification;
        this.psi = psi;
        this.xmlDataProcessorService = xmlDataProcessorService;
        objectMapper = new ObjectMapper();
        this.thread = new Thread(this, "GetXmlHeaders for adminCode: " + adminCode);
        log.debug("Constructed {}", thread.getName());
    }

    @Override
    public void run() {
        log.info("start {}", thread.getName());
        priceSenderService.updateState(psi.getAdminCode(), PriceState.IN_PROGRESS);
        try {
            List<ClassificationItem> classificationItemsPsi =
                    List.of(objectMapper.readValue(psi.getClassificationListJsonString(), ClassificationItem[].class));
            if (classificationItemsPsi.parallelStream().anyMatch(clitem -> clitem.getCategory() == null)
            ) throw new Exception("classification doesn't match");
            log.debug("ThreadName: {}, classification: {}", thread.getName(), classificationItemsPsi);
            List<Configuration> confs = List.of(objectMapper.readValue(psi.getConfigurationsJsonString(), Configuration[].class));
            String offstingJson = confs
                    .stream()
                    .filter(c -> c.getType().equals(ConfigurationType.Offsting))
                    .findFirst()
                    .orElseThrow()
                    .getValue();

            List<Integer> offsting = new ArrayList<>(List.of(objectMapper.readValue(offstingJson, Integer[].class)));
            if (offsting.size() > 1) {
                offsting.remove(offsting.size() - 1);
            }
            List<PriceDto> priceDtos = xmlDataProcessorService.parsePriceDtos(
                    new URL(psi.getFilePath()).openStream(),
                    offsting,
                    newClassificationItems,
                    Optional.of(confs),
                    psi.getPriceCurrency());

            if (priceDtos.isEmpty()) {
                log.error("list price dto is empty with {} {}", psi.getEmail(), psi.getAdminCode());
                priceSenderService.updateState(psi.getAdminCode(), PriceState.ERROR_EMPTY);
                return;
            }
        } catch (Exception exception) {
            priceSenderService.updateState(psi.getAdminCode(), PriceState.ERROR);
            log.error(exception.getMessage());
        }
        log.info("Finished {}", thread.getName());
    }
}
