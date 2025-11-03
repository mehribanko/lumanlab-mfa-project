package com.lumonlab.childcaremfa.feat.social.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleTokenApprove {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Google id 토큰 검증 및 사용자 정보
     */
    public GoogleUserInfo validateToken(String idToken) {
        try {
            // Google로 토큰 확인
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode tokenInfo = objectMapper.readTree(response);

            //  확인 (client id)
            String audience = tokenInfo.get("aud").asText();
            if (!googleClientId.equals(audience)) {
                throw new RuntimeException("잘못된 토큰 오디언스");
            }

            // 토큰이 만료되지 않았는지 확인
            long exp = tokenInfo.get("exp").asLong();
            if (System.currentTimeMillis() / 1000 > exp) {
                throw new RuntimeException("토큰 만료 되었습니다.");
            }

            // 유저 정보
            String googleUserId = tokenInfo.get("sub").asText();
            String email = tokenInfo.get("email").asText();
            String name = tokenInfo.has("name") ? tokenInfo.get("name").asText() : null;
            boolean emailVerified = tokenInfo.get("email_verified").asBoolean();

            if (!emailVerified) {
                throw new RuntimeException("이메일이 확인되지 않았습니다.");
            }

            return GoogleUserInfo.builder()
                    .googleUserId(googleUserId)
                    .email(email)
                    .name(name)
                    .emailVerified(emailVerified)
                    .build();

        } catch (Exception e) {
            log.error("Google 토큰 검증 실패", e);
            throw new RuntimeException("잘못된 Google 토큰: " + e.getMessage());
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GoogleUserInfo {
        private String googleUserId;
        private String email;
        private String name;
        private boolean emailVerified;
    }
}