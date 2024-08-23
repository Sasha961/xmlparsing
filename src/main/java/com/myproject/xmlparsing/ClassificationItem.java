package com.myproject.xmlparsing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationItem {

    @Schema(defaultValue = "store")
    private String priceHeader;
    @Schema(defaultValue = "BRAND")
    private String category;

    public String getPriceHeader() {
        return priceHeader.trim();
    }

    public static ClassificationItem fromHeader(String header) {
        ClassificationItem cli = new ClassificationItem();
        cli.setPriceHeader(header);
        return cli;
    }
}
