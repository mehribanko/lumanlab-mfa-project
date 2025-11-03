package com.lumonlab.childcaremfa.feat.social.controller;

import com.lumonlab.childcaremfa.feat.auth.dto.AuthResponse;
import com.lumonlab.childcaremfa.feat.social.dto.SocialAccResponse;
import com.lumonlab.childcaremfa.feat.social.service.SocialAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/social")
@RequiredArgsConstructor
@Slf4j
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    /**
     * Google 로그인 / 등록
     * Client Google 로그인에서 얻은 google id 토큰을 보내야 함!
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String idToken = request.get("idToken");
        if (idToken == null || idToken.isEmpty()) {
            throw new RuntimeException("ID 토큰이 필요합니다.");
        }

        AuthResponse response = socialAuthService.loginWithGoogle(idToken, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자에게 google 계정 연결
     */
    @PostMapping("/google/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SocialAccResponse> linkGoogleAccount(
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId = (Long) authentication.getPrincipal();
        String idToken = request.get("idToken");

        if (idToken == null || idToken.isEmpty()) {
            throw new RuntimeException("ID 토큰이 필요합니다.");
        }

        SocialAccResponse response = socialAuthService
                .linkGoogleAccountToCurrentUser(userId, idToken, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 연결된 social 계정 가져오기
     */
    @GetMapping("/accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SocialAccResponse>> getLinkedAccounts(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        List<SocialAccResponse> accounts = socialAuthService.getLinkedAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * 소셜 계정 연결 해제
     */
    @DeleteMapping("/accounts/{accountId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlinkSocialAccount(
            @PathVariable Long accountId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId = (Long) authentication.getPrincipal();

        socialAuthService.unlinkSocialAccount(userId, accountId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}