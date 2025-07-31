package com.nuevo.zatca.dto;

import lombok.Data;

import java.lang.reflect.Array;
import java.util.List;

@Data
public class ZatcaOnboardingComplianceDto {
    private String binarySecurityToken;
    private String secret;
    private String requestId;
    private String dispositionMessage;
    private List<String> errors;
}
