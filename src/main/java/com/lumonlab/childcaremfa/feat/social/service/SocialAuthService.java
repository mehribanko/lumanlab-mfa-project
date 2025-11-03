package com.lumonlab.childcaremfa.feat.social.service;

import com.lumonlab.childcaremfa.feat.audit.entity.AuditStatus;
import com.lumonlab.childcaremfa.feat.audit.service.AuditService;
import com.lumonlab.childcaremfa.feat.auth.dto.AuthResponse;
import com.lumonlab.childcaremfa.feat.security.jwt.JwtTokenProvider;
import com.lumonlab.childcaremfa.feat.social.dto.SocialAccResponse;
import com.lumonlab.childcaremfa.feat.token.entity.RefreshToken;
import com.lumonlab.childcaremfa.feat.token.service.TokenService;
import com.lumonlab.childcaremfa.feat.user.entity.*;
import com.lumonlab.childcaremfa.feat.user.repo.SocialAccountRepository;
import com.lumonlab.childcaremfa.feat.user.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final GoogleTokenApprove googleTokenValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuditService auditService;

    /**
     * Google 로그인 / 등록
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken, HttpServletRequest request) {
        // goole token 확인
        GoogleTokenApprove.GoogleUserInfo googleUser = googleTokenValidator.validateToken(idToken);

        // 소셜 계정이 있는지 확인
        Optional<SocialAccount> existingAccount = socialAccountRepository
                .findByProviderAndProviderUserId(SocialAuthProvider.GOOGLE, googleUser.getGoogleUserId());

        User user;
        boolean isNewUser = false;

        if (existingAccount.isPresent()) {
            user = existingAccount.get().getUser();
            log.info("기존 Google 사용자 로그인: {}", user.getEmail());

        } else {
            // New user - check if email already exists
            Optional<User> existingUser = userRepository.findByEmail(googleUser.getEmail());

            if (existingUser.isPresent()) {
                // 새 사용자 / 이메일이 이미 존재하는지 확인
                user = existingUser.get();
                linkGoogleAccount(user, googleUser);
                log.info("기존 사용자와 연결된 Google 계정: {}", user.getEmail());

            } else {
                // 새로운 계정 생성
                user = createUserFromGoogle(googleUser);
                isNewUser = true;
                log.info("Google 계정으로 새 사용자 생성: {}", user.getEmail());
            }
        }

        // 계정 상태 확인
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new RuntimeException("계정이 일시 정지되었습니다.");
        }
        
        user.updateLastLogin();
        userRepository.save(user);

        // 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRoles());
        RefreshToken refreshToken = tokenService.createRefreshToken(user, request);

        auditService.logAuthEvent(user,
                isNewUser ? "USER_REGISTERED_GOOGLE" : "USER_LOGIN_GOOGLE",
                AuditStatus.SUCCESS,
                request,
                Map.of("provider", "GOOGLE", "googleUserId", googleUser.getGoogleUserId()));

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

    /**
     * google 계정을 기존 로그인 사용자와 연결
     */
    @Transactional
    public SocialAccResponse linkGoogleAccountToCurrentUser(
            Long userId,
            String idToken,
            HttpServletRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // google token 확인
        GoogleTokenApprove.GoogleUserInfo googleUser = googleTokenValidator.validateToken(idToken);

        // 다른 사용자와 연결되어 있는지 확인
        Optional<SocialAccount> existing = socialAccountRepository
                .findByProviderAndProviderUserId(SocialAuthProvider.GOOGLE, googleUser.getGoogleUserId());

        if (existing.isPresent() && !existing.get().getUser().getId().equals(userId)) {
            throw new RuntimeException("이 Google 계정은 이미 다른 사용자와 연결되어 있습니다.");
        }

        // 계정 연결
        SocialAccount socialAccount = linkGoogleAccount(user, googleUser);

        auditService.logAuthEvent(user, "GOOGLE_ACCOUNT_LINKED", AuditStatus.SUCCESS,
                request, Map.of("googleUserId", googleUser.getGoogleUserId()));

        return mapToResponse(socialAccount);
    }

    /**
     * 사용자 소셜 계정 다 가져오기
     */
    @Transactional(readOnly = true)
    public List<SocialAccResponse> getLinkedAccounts(Long userId) {
        return socialAccountRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 소셜 계정 해제
     */
    @Transactional
    public void unlinkSocialAccount(Long userId, Long socialAccountId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        SocialAccount socialAccount = socialAccountRepository.findById(socialAccountId)
                .orElseThrow(() -> new RuntimeException("소셜 계정을 찾을 수 없습니다."));

        if (!socialAccount.getUser().getId().equals(userId)) {
            throw new RuntimeException("이 소셜 계정은 사용자의 계정이 아닙니다");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new RuntimeException("링크를 해제할 수 없습니다! 먼저 비밀번호를 설정해야 합니다.");
        }

        socialAccountRepository.delete(socialAccount);

        auditService.logAuthEvent(user, "SOCIAL_ACCOUNT_UNLINKED", AuditStatus.SUCCESS,
                request, Map.of("provider", socialAccount.getProvider()));

        log.info("사용자 소셜 계정 헤제: {}", userId);
    }



    private User createUserFromGoogle(GoogleTokenApprove.GoogleUserInfo googleUser) {
        User user = User.builder()
                .email(googleUser.getEmail())
                .status(UserStatus.ACTIVE)
                .mfaEnabled(false)
                .build();

        user.addRole(Role.PARENT);

        user = userRepository.save(user);

        // 소셜 계정 링크 만들기
        SocialAccount socialAccount = SocialAccount.builder()
                .user(user)
                .provider(SocialAuthProvider.GOOGLE)
                .providerUserId(googleUser.getGoogleUserId())
                .email(googleUser.getEmail())
                .displayName(googleUser.getName())
                .build();

        socialAccountRepository.save(socialAccount);

        return user;
    }

    private SocialAccount linkGoogleAccount(User user, GoogleTokenApprove.GoogleUserInfo googleUser) {
        SocialAccount socialAccount = SocialAccount.builder()
                .user(user)
                .provider(SocialAuthProvider.GOOGLE)
                .providerUserId(googleUser.getGoogleUserId())
                .email(googleUser.getEmail())
                .displayName(googleUser.getName())
                .build();

        return socialAccountRepository.save(socialAccount);
    }

    private SocialAccResponse mapToResponse(SocialAccount account) {
        return SocialAccResponse.builder()
                .id(account.getId())
                .provider(account.getProvider())
                .email(account.getEmail())
                .displayName(account.getDisplayName())
                .linkedAt(account.getLinkedAt())
                .build();
    }
}