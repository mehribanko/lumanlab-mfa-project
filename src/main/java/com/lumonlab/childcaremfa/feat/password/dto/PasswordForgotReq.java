package com.lumonlab.childcaremfa.feat.password.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordForgotReq {

    @NotBlank(message = "이메일 입력이 필수 입니다!")
    @Email(message = "잘 못 된 이메일 방식")
    private String email;
}
