package com.lumonlab.childcaremfa.feat.mfa.controller;

import com.lumonlab.childcaremfa.feat.mfa.dto.MfaSetupResponse;
import com.lumonlab.childcaremfa.feat.mfa.dto.MfaStatusResponse;
import com.lumonlab.childcaremfa.feat.mfa.dto.MfaVerifyReq;
import com.lumonlab.childcaremfa.feat.mfa.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {

    private final MfaService mfaService;

    /**
     * 현재 유저 mfa 설정/비설정 여부 체크
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        MfaStatusResponse response = mfaService.getMfaStatus(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * mfa 설정/ QR 코드 생성
     */
    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaSetupResponse> setupMfa(
            Authentication authentication,
            HttpServletRequest request) {
        Long userId = (Long) authentication.getPrincipal();

        MfaSetupResponse response = mfaService.setupMfa(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     *  mfa 체크/ 인증
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaStatusResponse> verifyAndEnableMfa(
            @Valid @RequestBody MfaVerifyReq verifyRequest,
            Authentication authentication,
            HttpServletRequest request) {
        Long userId = (Long) authentication.getPrincipal();

        MfaStatusResponse response = mfaService.verifyAndEnableMfa(
                userId, verifyRequest.getCode(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * mfa 끄기
     */
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaStatusResponse> disableMfa(
            @Valid @RequestBody MfaVerifyReq verifyRequest,
            Authentication authentication,
            HttpServletRequest request) {
        Long userId = (Long) authentication.getPrincipal();

        MfaStatusResponse response = mfaService.disableMfa(
                userId, verifyRequest.getCode(), request);
        return ResponseEntity.ok(response);
    }
}
