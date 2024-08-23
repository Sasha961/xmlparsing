package com.myproject.xmlparsing.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Validate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceDto {

    private Long id;
    private String[] tableRef;
    private String brand;
    private String article;
    private String partname;
    private String quantity;
    private String multiplicity;
    private String delivery;
    private String status;
    private String warehouse;
    private String price;
    private String notes;
    private String photo;
    private String currency;

    @JsonIgnore
    public boolean isValid() throws IllegalArgumentException {
        try {
            Validate.notBlank(price);
            Validate.notBlank(article);
            Validate.notBlank(brand);
            Validate.notBlank(quantity);
            Validate.notBlank(partname);
            return true;
        } catch (AssertionError | NullPointerException | IllegalArgumentException ignored) {
            return false;
        }
    }
}
