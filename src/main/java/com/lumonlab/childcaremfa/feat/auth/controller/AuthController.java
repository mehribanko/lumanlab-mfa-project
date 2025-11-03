package com.lumonlab.childcaremfa.feat.auth.controller;

import com.lumonlab.childcaremfa.feat.auth.dto.AuthResponse;
import com.lumonlab.childcaremfa.feat.auth.dto.LoginReq;
import com.lumonlab.childcaremfa.feat.auth.dto.RefreshTokenReq;
import com.lumonlab.childcaremfa.feat.auth.dto.RegisterReq;
import com.lumonlab.childcaremfa.feat.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterReq request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginReq request,
            HttpServletRequest httpRequest) {
        log.info("로그인 접속 시도 email {}", request.getEmail());
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenReq request,
            HttpServletRequest httpRequest) {
        log.info("Refresh 토큰 발생 시도");
        AuthResponse response = authService.refresh(request.getRefreshToken(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("로그아웃 접속 시도 user: {}", userId);
        authService.logout(userId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}