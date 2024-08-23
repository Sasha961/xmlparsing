package com.myproject.xmlparsing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceSenderInfoRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findPriceSenderInfoEntityByAdminCode(String adminCode);
    Optional<UserEntity> findPriceSenderInfoEntityByVendorId(String uuid);

    default boolean isPriceProcessed(Optional<String> vendorId) {
        if (vendorId.isPresent()) {
            Optional<UserEntity> psi = findPriceSenderInfoEntityByVendorId(vendorId.get());
            return psi.filter(priceSenderInfoEntity -> priceSenderInfoEntity.getPriceTableRefs().length > 0).isPresent();
        } else {
            return false;
        }
    }
}
