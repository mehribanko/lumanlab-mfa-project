package com.lumonlab.childcaremfa.feat.social.dto;

import com.lumonlab.childcaremfa.feat.user.entity.SocialAuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginReq {

    @NotNull(message = "Provider 필요합니다.")
    private SocialAuthProvider provider;

    @NotNull(message = "Provider id 필요합니다.")
    private String providerUserId;

    @Email(message = "잘 못 된 이메일 형태입니다.")
    private String email;

    private String displayName;

    @NotBlank(message = "ID 토큰이 필요합니다")
    private String idToken;
}