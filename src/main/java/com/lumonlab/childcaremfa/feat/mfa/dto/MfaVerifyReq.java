package com.lumonlab.childcaremfa.feat.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyReq {

    @NotBlank(message = "MFA 코드 입력이 필요합니다!")
    @Pattern(regexp = "^[0-9]{6}$", message = "MFA 코드 6 숫자 이상이어야 합니다.")
    private String code;
}