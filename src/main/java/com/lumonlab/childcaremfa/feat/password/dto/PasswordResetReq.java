package com.lumonlab.childcaremfa.feat.password.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetReq {

    @NotBlank(message = "엑세스 토큰 필요합니다.")
    private String token;

    @NotBlank(message = "새 비밀번호 입력 필요합니다.")
    @Size(min = 8, max = 15, message = "비밀 번호가 8 ~ 15 사이 글자 수로 구성되어야 합니다!")
    private String newPassword;
}