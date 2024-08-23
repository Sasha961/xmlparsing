package com.myproject.xmlparsing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {
    private ConfigurationType type;
    private String value;
    private String category;
}
