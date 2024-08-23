package com.myproject.xmlparsing;

import com.myproject.xmlparsing.dto.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.myproject.xmlparsing.PriceState.AWAITS_CLASSIFICATION;
import static com.myproject.xmlparsing.PriceState.CHANGED;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(profiles = {"local"})
class XmlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PriceSenderService priceSenderService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private PriceSenderInfoRepository priceSenderInfoRepository;

    void registerPriceXml() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/xml/register")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(
                                        "https://zhilichev.ru/downloads/interauto/farpost/pricelist.xml",
                                        "[0,1]",
                                        "s.panin@qwep.ru",
                                        "RUB",
                                        "MockTest_e960a2fc1b3eab366d08ee38b1bad0cfbf1ec3ae")))
                        .header("X-auth",
                                "02ED402A-7E9A-4E23-95A2-7AC2087EA142"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Awaitility
                .await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(Duration.ofMinutes(15))
                .with()
                .pollInterval(Duration.ofMinutes(1))
                .until(() -> {
                    UserEntity psi1 = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
                    return psi1.getCurrentState().equals(AWAITS_CLASSIFICATION.state);
                });
        UserEntity psiFinal = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
        assert !psiFinal.getVendorId().isBlank();
        assert !psiFinal.getName().isBlank();
        assert !psiFinal.getEmail().isBlank();
        assert !psiFinal.getFilePath().isBlank();
        assert !psiFinal.getClassificationListJsonString().isBlank();
        assert psiFinal.getPriceCurrency().equals("RUB");
    }

    public void addConfiguration() throws Exception {
        UserEntity psi = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
        List<Configuration> list = List.of(objectMapper.readValue(psi.getConfigurationsJsonString(), Configuration[].class));
        List<Configuration> newList = new ArrayList<>();
        newList.addAll(list);

        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("link", "https://zhilichev.ru/downloads/interauto/farpost/pricelist.xml");
        parameters.add("price-currency", "RUB");
        parameters.add("vendor-name", "MockTest_e960a2fc1b3eab366d08ee38b1bad0cfbf1ec3ae");
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/v3/price/configuration")
                        .header("X-Admin-Code", "B4413FE121DC664")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newList))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Awaitility
                .await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(Duration.ofMinutes(15))
                .with()
                .pollInterval(Duration.ofMinutes(1))
                .until(() -> {
                    UserEntity psi1 = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
                    return !psi1.getConfigurationsJsonString().isBlank();
                });
        UserEntity psiFinal = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
        assert !psiFinal.getConfigurationsJsonString().isBlank();
    }

    void addClassification() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        UserEntity psi = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
        List<ClassificationItem> classificationItemsPsi =
                List.of(objectMapper.readValue(psi.getClassificationListJsonString(), ClassificationItem[].class));
        log.info(classificationItemsPsi.toString());
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("link", "https://zhilichev.ru/downloads/interauto/farpost/pricelist.xml");
        parameters.add("price-currency", "RUB");
        parameters.add("vendor-name", "MockTest_e960a2fc1b3eab366d08ee38b1bad0cfbf1ec3ae");
        parameters.add("X-Admin-Code", "B4413FE121DC664");

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/xml/classification")
                        .params(parameters)
                        .header("X-Admin-Code", "B4413FE121DC664")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classificationItemsPsi)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Awaitility
                .await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(Duration.ofMinutes(15))
                .with()
                .pollInterval(Duration.ofMinutes(1))
                .until(() -> {
                    UserEntity psi1 = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
                    List<ClassificationItem> classificationItemsPsi1 =
                            List.of(objectMapper.readValue(psi1.getClassificationListJsonString(), ClassificationItem[].class));
                    return !classificationItemsPsi1.get(0).getCategory().isBlank();
                });

        Awaitility
                .await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(Duration.ofMinutes(15))
                .with()
                .pollInterval(Duration.ofMinutes(1))
                .until(() -> {
                    UserEntity psi1 = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
                    return psi1.getCurrentState().equals(CHANGED.state);
                });
    }

    void updateXml() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        UserEntity psi = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
        List<ClassificationItem> classificationItemsPsi =
                List.of(objectMapper.readValue(psi.getClassificationListJsonString(), ClassificationItem[].class));
        log.info(classificationItemsPsi.toString());

        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("link", "https://zhilichev.ru/downloads/interauto/farpost/pricelist.xml");
        parameters.add("price-currency", "RUB");
        parameters.add("vendor-name", "MockTest_e960a2fc1b3eab366d08ee38b1bad0cfbf1ec3ae");
        parameters.add(HttpHeaders.AUTHORIZATION, "e960a2fc1b3eab366d08ee38b1bad0cfbf1ec3ae");
        parameters.add("X-Admin-Code", "B4413FE121DC664");

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/xml/update")
                        .params(parameters)
                        .header("X-Admin-Code", "B4413FE121DC664")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classificationItemsPsi)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Awaitility
                .await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(Duration.ofMinutes(15))
                .with()
                .pollInterval(Duration.ofMinutes(1))
                .until(() -> {
                    UserEntity psi1 = priceSenderService.getPriceSenderInfoEntity("B4413FE121DC664").get();
                    return psi1.getCurrentState().equals(CHANGED.state);
                });
    }

    @Test
    void testXml() throws Exception {
        priceSenderInfoRepository.deleteAll();
        registerPriceXml();
        addConfiguration();
        addClassification();
        updateXml();
        priceSenderInfoRepository.deleteAll();
    }
}
