package com.lumonlab.childcaremfa.feat.auth.dto;

import com.lumonlab.childcaremfa.feat.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterReq {

    @NotBlank(message = "이메일 입력이 필수 입니다!")
    @Email(message = "잘 못 된 이메일 방식")
    private String email;

    @NotBlank(message = "비밀 번호가 필수 입니다!")
    @Size(min = 8, max = 15, message = "비밀 번호가 8 ~ 15 사이 글자 수로 구성되어야 합니다!")
    private String password;

    @NotNull(message = "Role 입력이 필수 입니다!")
    private Role role;
}