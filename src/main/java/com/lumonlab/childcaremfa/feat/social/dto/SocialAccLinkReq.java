package com.lumonlab.childcaremfa.feat.social.dto;


import com.lumonlab.childcaremfa.feat.user.entity.SocialAuthProvider;
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
public class SocialAccLinkReq {

    @NotNull(message = "Provider 필요합니다.")
    private SocialAuthProvider provider;

    @NotBlank(message = "ID 토큰이 필요합니다.")
    private String idToken;
}