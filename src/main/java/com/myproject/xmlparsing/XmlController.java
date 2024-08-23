package com.myproject.xmlparsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.xmlparsing.dto.Configuration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlbeans.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.myproject.xmlparsing.PriceState.*;
import static com.myproject.xmlparsing.dto.ConfigurationType.Offsting;

@Slf4j
@Validated
@RestController
@RequestMapping("/xml")
public class XmlController {

    public static final String BEARER = "Bearer || ";

    private final ObjectMapper objectMapper;
    private final PriceSenderService priceSenderService;
    private final XmlDataProcessorService xmlDataProcessorService;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    public XmlController(ObjectMapper objectMapper,
                         PriceSenderService priceSenderService,
                         XmlDataProcessorService xmlDataProcessorService,
                         @Qualifier("redis.internal") RedisTemplate<String, String> redisTemplate, UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.priceSenderService = priceSenderService;
        this.xmlDataProcessorService = xmlDataProcessorService;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/register")
    @Operation(summary = "Регистрация нового прайс-листа в формате XML-ссылки.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новый прайс-лист зарегистрирован в QWEP и ожидает классификацию."),
            @ApiResponse(responseCode = "400", description = "Запрос выполнен с ошибками.")})
    public ResponseEntity<Void> register(@RequestBody UserDto userDto,
                                         @RequestHeader("X-auth") String bearerToken) {
        if (userDto.link().isEmpty()) {
            log.error("link is empty");
            return ResponseEntity.badRequest().build();
        }
        try {
            UserEntity user = userRepository.findByAccountId(userDto.userName());
            UserEntity psi = new UserEntity();
            psi.setFilePath(userDto.link());
            psi.setConfigurationsJsonString(
                    objectMapper.writeValueAsString(List.of(
                            new Configuration(Offsting, userDto.offsting(), ""))));
            psi.setName(userDto.userName() + " (price)");
            psi.setEmail(userDto.userEmail());
            psi.setAdminCode(user.getAdminCode());
            psi.setClassificationListJsonString("[ ]");
            psi.setPriceTableRefs(new String[]{});
            psi.setViewCode(user.getViewCode());
            psi.setPriceCurrency(userDto.currency());
            psi.setCurrentState(REGISTERED.state);
            psi.setEmailIdentification(false);
            priceSenderService.savePriceSenderInfoEntity(psi);
            log.info("Added price sender with {}", user);

            ParseXmlHeadersAndSave pxhas = new ParseXmlHeadersAndSave(
                    userDto.link(),
                    psi,
                    priceSenderService,
                    xmlDataProcessorService);
            pxhas.thread.start();
        } catch (Exception e) {
            log.info("registration failed in xml controller: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } finally {
            ThreadLocalUtil.clearAllThreadLocals();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/update")
    @Operation(summary = "Обновление зарегистрированного в QWEP прайс-листа.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Обновленный прайс-лист загружен и отправлен на обработку."),
            @ApiResponse(responseCode = "400", description = "Запрос выполнен с ошибками.")})
    public UniversalResponseDto update(
            @RequestHeader("X-Admin-Code") @Size(min = 15, max = 15) String adminCode) {
        log.info("begin to update price from adminCode '{}'", adminCode);
        Optional<UserEntity> userEntity = priceSenderService.getPriceSenderInfoEntity(adminCode);
        if (userEntity.isPresent() && userEntity.get().getCurrentState().equals(IN_PROGRESS.state)) {
            return new UniversalResponseDto(false, IN_PROGRESS_TRY_LATER.toString());
        }
        if (userEntity.isPresent() && userEntity.get().getCurrentState().equals(AWAITS_CLASSIFICATION.state)) {
            return new UniversalResponseDto(false, "Price awaits classification, try later");
        }
        try {
            List<ClassificationItem> getClassification =
                    List.of(objectMapper.readValue(userEntity.get().getClassificationListJsonString(), ClassificationItem[].class));
            SaveNewClassificationAndUpdatePriceData sncapp =
                    new SaveNewClassificationAndUpdatePriceData(
                            adminCode,
                            priceSenderService,
                            userEntity.get(),
                            getClassification,
                            xmlDataProcessorService
                    );
            sncapp.thread.start();
            return new UniversalResponseDto(true, "Price starts updating");
        } catch (IllegalArgumentException exception) {
            return new UniversalResponseDto(false, IN_PROGRESS_TRY_LATER.toString());
        } catch (IOException exception) {
            return new UniversalResponseDto(false, "Price can't be updated right now");
        } finally {
            ThreadLocalUtil.clearAllThreadLocals();
        }
    }

    @PostMapping("/classification")
    @Operation(summary = "Получение текущей классификации прайс-листа.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Классификация прайс-листа получена."),
            @ApiResponse(responseCode = "400", description = "Запрос выполнен с ошибками.")})
    public ResponseEntity<Void> setClassification(
            @RequestHeader("X-Admin-Code") @Size(min = 15, max = 15) String adminCode,
            @RequestBody List<ClassificationItem> newClassification) {
        Optional<UserEntity> psiOpt = priceSenderService.getPriceSenderInfoEntity(adminCode);
        if (psiOpt.isPresent() && psiOpt.get().getCurrentState().equals(IN_PROGRESS.state)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            SaveNewClassificationAndUpdatePriceData getClassificationAndSave =
                    new SaveNewClassificationAndUpdatePriceData(
                            adminCode,
                            priceSenderService,
                            psiOpt.get(),
                            newClassification,
                            xmlDataProcessorService
                    );
            getClassificationAndSave.thread.start();
        } catch (Exception exception) {
            log.info("exception: {}, class: {}", exception.getMessage(), this.getClass());
            return ResponseEntity.badRequest().build();
        } finally {
            ThreadLocalUtil.clearAllThreadLocals();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/parse-xml")
    @Operation(summary = "парсинг xml ссылки в xml разметку.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно."),
            @ApiResponse(responseCode = "400", description = "Запрос выполнен с ошибками.")})
    public String parseXmlLink(@RequestParam(value = "link") String link) {
        try {
            UUID uuid = UUID.randomUUID();
            ParsingXmlLink parsingXmlLink = new ParsingXmlLink(redisTemplate, link, uuid);
            redisTemplate.opsForValue().set(uuid.toString(), "not finished", 60, TimeUnit.MINUTES);
            parsingXmlLink.thread.start();
            return uuid.toString();
        } catch (Exception e) {
            log.error("parsing xml fail with exception: {}", e.getMessage());
            return "parsing xml fail";
        }
    }

    @PostMapping("/get-xml")
    @Operation(summary = "получить xml разметку по uuid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно."),
            @ApiResponse(responseCode = "400", description = "Запрос выполнен с ошибками.")})
    public String getXml(@RequestParam(value = "uuid") String uuid) {
        try {
            if (Objects.requireNonNull(redisTemplate.opsForValue().get(uuid)).equals("not finished")) {
                return "parsing is not finished";
            }
            return redisTemplate.opsForValue().get(uuid);
        } catch (Exception e) {
            log.error("get xml fail: {}", e.getMessage());
            return "getting xml failed";
        }
    }
}