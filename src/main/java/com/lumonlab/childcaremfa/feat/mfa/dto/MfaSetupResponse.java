package com.lumonlab.childcaremfa.feat.mfa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupResponse {

    private String secret;
    private String qrCodeUrl;
    private String qrCodeImage;
    private String issuer;
    private String accountName;
    private String message;
}
