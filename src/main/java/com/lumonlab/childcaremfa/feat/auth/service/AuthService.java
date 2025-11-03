package com.lumonlab.childcaremfa.feat.auth.service;

import com.lumonlab.childcaremfa.feat.audit.entity.AuditStatus;
import com.lumonlab.childcaremfa.feat.audit.service.AuditService;
import com.lumonlab.childcaremfa.feat.auth.dto.AuthResponse;
import com.lumonlab.childcaremfa.feat.auth.dto.LoginReq;
import com.lumonlab.childcaremfa.feat.auth.dto.RegisterReq;
import com.lumonlab.childcaremfa.feat.mfa.service.MfaService;
import com.lumonlab.childcaremfa.feat.security.jwt.JwtTokenProvider;
import com.lumonlab.childcaremfa.feat.token.entity.RefreshToken;
import com.lumonlab.childcaremfa.feat.token.service.TokenService;
import com.lumonlab.childcaremfa.feat.user.entity.UserStatus;
import com.lumonlab.childcaremfa.feat.user.entity.User;
import com.lumonlab.childcaremfa.feat.user.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final MfaService mfaService;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 15;

    @Transactional
    public AuthResponse register(RegisterReq request, HttpServletRequest httpRequest) {
        // 이메일 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 가입된 이메일 입니다!");
        }

        // 유저 생성
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .mfaEnabled(false)
                .build();

        // role
        user.addRole(request.getRole());

     
        user = userRepository.save(user);

        // 로그 기록
        auditService.logAuthEvent(user, "USER_REGISTERED", AuditStatus.SUCCESS,
                httpRequest, Map.of("role", request.getRole()));

        boolean mfaEnforced = mfaService.isMfaEnforced(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRoles());
        RefreshToken refreshToken = tokenService.createRefreshToken(user, httpRequest);

        log.info("유저 생성이 완료 되었습니다: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getTokenHash())
                .tokenType("Bearer")
                .expiresIn(900L)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .mfaEnabled(user.getMfaEnabled())
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginReq request, HttpServletRequest httpRequest) {
        // 유저 조회
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // 유저 계정이 차단 안되어 있는지 체크
        if (user.isLocked()) {
            auditService.logFailedLogin(request.getEmail(), "차단 된 계정", httpRequest);
            throw new RuntimeException("계정이 차단되었습니다. 나중에 다시 시도해주세요!");
        }

        // suspended 계정인지 체크
        if (user.getStatus() == UserStatus.SUSPENDED) {
            auditService.logFailedLogin(request.getEmail(), "계정이 보류되었습니다.", httpRequest);
            throw new RuntimeException("계정이 보류되었습니다.");
        }

        // pwd 체크
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, httpRequest);
            throw new RuntimeException("입력 값이 잘 못 되었습니다!");
        }

        boolean mfaEnforced = mfaService.isMfaEnforced(user);
        if (mfaEnforced && !user.getMfaEnabled()) {
            auditService.logAuthEvent(user, "LOGIN_BLOCKED_MFA_NOT_SETUP", AuditStatus.FAILURE,
                    httpRequest, Map.of("reason", "MFA 인증이 필수 입니다."));
            throw new RuntimeException("MFA 인증이 필수 입니다. MFA를 먼저 설정해주세요!");
        }

        if (user.getMfaEnabled()) {
            if (request.getMfaCode() == null || request.getMfaCode().isEmpty()) {
                return AuthResponse.builder()
                        .mfaRequired(true)
                        .mfaEnabled(true)
                        .userId(user.getId())
                        .email(user.getEmail())
                        .message("MFA 인증이 필요합니다!")
                        .build();
            }

            // MFA code 체크
            boolean mfaValid = mfaService.verifyMfaCode(user, request.getMfaCode());
            if (!mfaValid) {
                auditService.logAuthEvent(user, "LOGIN_FAILED_INVALID_MFA", AuditStatus.FAILURE,
                        httpRequest, Map.of("reason", "MFA code 잘 못 되었습니다!"));
                throw new RuntimeException("MFA code 잘 못 되었습니다!");
            }
        }


        // 로그인 후에!
        user.resetFailedAttempts();
        user.updateLastLogin();
        userRepository.save(user);

        // token 생성~
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRoles());
        RefreshToken refreshToken = tokenService.createRefreshToken(user, httpRequest);

        auditService.logAuthEvent(user, "USER_LOGIN", AuditStatus.SUCCESS,
                httpRequest, Map.of("method", "email_password"));

        log.info("로그인 완료:  {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getTokenHash())
                .tokenType("Bearer")
                .expiresIn(900L)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .mfaEnabled(user.getMfaEnabled())
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue, HttpServletRequest httpRequest) {
        // refresh token
        RefreshToken oldToken = tokenService.findValidToken(refreshTokenValue);
        if (oldToken == null) {
            throw new RuntimeException("Refresh 토큰이 만료 되었습니다!");
        }

        User user = oldToken.getUser();

        // refresh token 로테이트
        RefreshToken newRefreshToken = tokenService.rotateRefreshToken(oldToken, httpRequest);

        // AT 생성
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRoles());


        auditService.logAuthEvent(user, "TOKEN_REFRESHED", AuditStatus.SUCCESS,
                httpRequest, Map.of());

        log.info("토큰 제발급 완료: {}", user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getTokenHash())
                .tokenType("Bearer")
                .expiresIn(900L)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .mfaEnabled(user.getMfaEnabled())
                .build();
    }

    @Transactional
    public void logout(Long userId, HttpServletRequest httpRequest) {

        tokenService.revokeAllUserTokens(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 찾을 수 없습니다!"));

        auditService.logAuthEvent(user, "USER_LOGOUT", AuditStatus.SUCCESS,
                httpRequest, Map.of());

        log.info("로그아웃 완료: {}", userId);
    }

    private void handleFailedLogin(User user, HttpServletRequest request) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.lock(LOCK_TIME_MINUTES);
            log.warn(" 로그인이 여러번 실패해서 계정이 잠시 차단되었습니다: {}", user.getEmail());

            auditService.logAuthEvent(user, "ACCOUNT_LOCKED", AuditStatus.SUCCESS,
                    request, Map.of("reason", "로그인 여러번 실패했습니다."));
        }

        userRepository.save(user);
        auditService.logFailedLogin(user.getEmail(), "입력 값이 잘 못 되었습니다.", request);
    }
}