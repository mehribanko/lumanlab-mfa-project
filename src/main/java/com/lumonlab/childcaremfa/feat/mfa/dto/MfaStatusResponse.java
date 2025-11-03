package com.lumonlab.childcaremfa.feat.mfa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaStatusResponse {

    private Boolean mfaEnabled;
    private Boolean mfaEnforced;
    private String message;
}
