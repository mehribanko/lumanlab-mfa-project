package com.lumonlab.childcaremfa.feat.token.service;

import com.lumonlab.childcaremfa.feat.security.jwt.JwtTokenProvider;
import com.lumonlab.childcaremfa.feat.token.entity.RefreshToken;
import com.lumonlab.childcaremfa.feat.token.repo.RTokenRepository;
import com.lumonlab.childcaremfa.feat.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final RTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .build();

        refreshTokenRepository.save(refreshToken);

        return refreshToken.toBuilder().tokenHash(rawToken).build();
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, HttpServletRequest request) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(rawToken);

        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .tokenHash(tokenHash)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .rotatedFrom(oldToken)
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .build();

        refreshTokenRepository.save(newToken);

        oldToken.revoke();
        refreshTokenRepository.save(oldToken);

        log.info("사용자를 위한 rotated 새로 고침 토큰: {}", oldToken.getUser().getId());

        return newToken.toBuilder().tokenHash(rawToken).build();
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        log.info("사용자 모든 토큰이 취소됨: {}", userId);
    }

    @Transactional(readOnly = true)
    public RefreshToken findValidToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(rawToken)
                .filter(RefreshToken::isValid)
                .orElse(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}