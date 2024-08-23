package com.myproject.xmlparsing;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class PriceSenderService {

    private final PriceSenderInfoRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public Optional<UserEntity> getPriceSenderInfoEntity(String adminCode) {
        return repository.findPriceSenderInfoEntityByAdminCode(adminCode);
    }

    public void savePriceSenderInfoEntity(UserEntity psi) {
        repository.save(psi);
    }

    @Transactional
    public void setClassification(String adminCode, String classificationJson) throws NoSuchElementException {
        Optional<UserEntity> psiOptional =
                repository.findPriceSenderInfoEntityByAdminCode(adminCode);
        if (psiOptional.isEmpty()) throw new NoSuchElementException("No psi with this adminCode");
        UserEntity psi = psiOptional.get();
        psi.setClassificationListJsonString(classificationJson);
        repository.save(psi);
    }

    @Transactional
    public void updateState(String adminCode, PriceState priceState) throws NoSuchElementException {
        Optional<UserEntity> psiOptional =
                repository.findPriceSenderInfoEntityByAdminCode(adminCode);
        if (psiOptional.isEmpty()) throw new NoSuchElementException("No psi with this adminCode");
        UserEntity psi = psiOptional.get();
        psi.setCurrentState(priceState.state);
        if (priceState.state.equals(PriceState.ERROR_EMPTY.state)) psi.setPriceTableRefs(new String[]{});
        repository.save(psi);
    }
}
