package com.myproject.xmlparsing.dto;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {

    private String accountId;
    private String adminCode;
    private String viewCode;
    private UUID userId;
}
