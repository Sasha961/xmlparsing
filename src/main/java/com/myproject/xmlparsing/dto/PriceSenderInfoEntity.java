package com.myproject.xmlparsing.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.myproject.xmlparsing.PriceFile;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "price_sender_info")
public class PriceSenderInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "vendor_id")
    private String vendorId;
    @Column(name = "name")
    private String name;
    @Column(name = "email")
    private String email;
    @Column(name = "admin_code")
    private String adminCode;
    @Column(name = "view_code")
    private String viewCode;
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    @Column(name = "file_path")
    private String filePath;
    @Column(name = "current_state")
    private String currentState;
    @Column(name = "price_currency")
    private String priceCurrency;
    @Column(name = "userapi_account_id")
    private String userApiAccountId;
    @Convert(attributeName = "price_table_refs", converter = StringArrayType.class)
    @Column(name = "price_table_refs", columnDefinition = "text[]")
    private String[] priceTableRefs;
    @Column(name = "classification")
    private String classificationListJsonString;
    @Column(name = "configurations")
    private String configurationsJsonString;
    @Column(name = "email_identification")
    private Boolean emailIdentification;

    @OneToOne(mappedBy = "priceSenderInfoEntity")
    @JsonManagedReference
    @JsonIgnore
    @ToString.Exclude
    public PriceFile priceFile;
}
