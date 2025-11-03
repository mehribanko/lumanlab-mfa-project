package com.lumonlab.childcaremfa.feat.password.controller;

import com.lumonlab.childcaremfa.feat.password.dto.PasswordChangeReq;
import com.lumonlab.childcaremfa.feat.password.dto.PasswordForgotReq;
import com.lumonlab.childcaremfa.feat.password.dto.PasswordResetReq;
import com.lumonlab.childcaremfa.feat.password.dto.PasswordResponse;
import com.lumonlab.childcaremfa.feat.password.service.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final PasswordService passwordService;

    /**
     * 인증된 사용자 비밀번호 변경
     */
    @PostMapping("/change")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PasswordResponse> changePassword(
            @Valid @RequestBody PasswordChangeReq request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId = (Long) authentication.getPrincipal();

        PasswordResponse response = passwordService.changePassword(userId, request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 재설정 요청 / 토큰과 함께 이메일 전송
     */
    @PostMapping("/resetpwd")
    public ResponseEntity<PasswordResponse> forgotPassword(
            @Valid @RequestBody PasswordForgotReq request,
            HttpServletRequest httpRequest) {

        PasswordResponse response = passwordService.forgotPassword(request.getEmail(), httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 토큰을 사용해서 비밀번호 재설정
     */
    @PostMapping("/reset")
    public ResponseEntity<PasswordResponse> resetPassword(
            @Valid @RequestBody PasswordResetReq request,
            HttpServletRequest httpRequest) {

        log.info("Reset password request");

        PasswordResponse response = passwordService.resetPassword(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * reset 토큰 검증
     */
    @GetMapping("/reset/validate")
    public ResponseEntity<PasswordResponse> validateResetToken(
            @RequestParam String token) {

        PasswordResponse response = passwordService.validateResetToken(token);
        return ResponseEntity.ok(response);
    }
}