package com.lumonlab.childcaremfa.feat.password.service;

import com.lumonlab.childcaremfa.feat.audit.entity.AuditStatus;
import com.lumonlab.childcaremfa.feat.audit.service.AuditService;
import com.lumonlab.childcaremfa.feat.password.dto.PasswordChangeReq;
import com.lumonlab.childcaremfa.feat.password.dto.PasswordResetReq;
import com.lumonlab.childcaremfa.feat.password.dto.PasswordResponse;
import com.lumonlab.childcaremfa.feat.password.repo.PasswordResetRepository;
import com.lumonlab.childcaremfa.feat.token.entity.PasswordResetToken;
import com.lumonlab.childcaremfa.feat.user.entity.User;
import com.lumonlab.childcaremfa.feat.user.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${password-reset.token-expiration}")
    private Long tokenExpiration;

    /**
     * 인증된 사용자 비밀번호 변경
     */
    @Transactional
    public PasswordResponse changePassword(
            Long userId,
            PasswordChangeReq request,
            HttpServletRequest httpRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            auditService.logAuthEvent(user, "PASSWORD_CHANGE_FAILED", AuditStatus.FAILURE,
                    httpRequest, Map.of("reason", "현재 비밀번호가 잘못되었습니다"));
            throw new RuntimeException("현재 비밀번호가 잘못되었습니다.");
        }

        // 새 비밀번호 확인
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 비밀번호 업데이트
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 확인 이메일 보내기
        emailService.sendPasswordChangedEmail(user.getEmail());

        auditService.logAuthEvent(user, "PASSWORD_CHANGED", AuditStatus.SUCCESS,
                httpRequest, Map.of());

        log.info("사용자 비밀번호 변경 완료: {}", userId);

        return PasswordResponse.builder()
                .success(true)
                .message("사용자 비밀번호 변경되었습니다.")
                .build();
    }

    /**
     *비밀번호 재설정 요청 / 토큰과 함께 이메일 전송
     */
    @Transactional
    public PasswordResponse forgotPassword(String email, HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.warn("존재하지 않는 이메일에 대한 비밀번호 재설정 요청:: {}", email);
            return PasswordResponse.builder()
                    .success(true)
                    .message("비밀번호 재설정 링크가 전송되었습니다.")
                    .build();
        }
        
        tokenRepository.deleteAllByUserId(user.getId());

        // reset token 생성
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenExpiration / 1000);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(resetToken);

        // email 전송
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        auditService.logAuthEvent(user, "PASSWORD_RESET_REQUESTED", AuditStatus.SUCCESS,
                httpRequest, Map.of());

        log.info("비밀번호 재설정 토큰: {}", user.getId());

        return PasswordResponse.builder()
                .success(true)
                .message("비밀번호 재설정 링크가 전송되었습니다.")
                .build();
    }

    /**
     * 이메일 토큰을 사용해서 비밀번호 재설정
     */
    @Transactional
    public PasswordResponse resetPassword(
            PasswordResetReq request,
            HttpServletRequest httpRequest) {

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("유효하지 않거나 만료된 토큰"));

        // token 확인
        if (!resetToken.isValid()) {
            if (resetToken.isExpired()) {
                throw new RuntimeException("재설정 토큰이 만료되었습니다. 새 토큰을 요청해 주세요.");
            }
            if (resetToken.isUsed()) {
                throw new RuntimeException("재설정 토큰이 이미 사용되었습니다.");
            }
        }

        User user = resetToken.getUser();

        // password 업데이트
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.resetFailedAttempts();
        user.unlock();
        userRepository.save(user);

        // 이미 사용됨!
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // 확인 이메일 보내기
        emailService.sendPasswordChangedEmail(user.getEmail());


        auditService.logAuthEvent(user, "PASSWORD_RESET_COMPLETED", AuditStatus.SUCCESS,
                httpRequest, Map.of());

        log.info("사용자 비밀번호 재설정이 완료되었습니다:: {}", user.getId());

        return PasswordResponse.builder()
                .success(true)
                .message("비밀번호가 재설정되었습니다. 이제 새 비밀번호로 로그인할 수 있습니다.")
                .build();
    }

    /**
     * reset 토큰 검증
     */
    @Transactional(readOnly = true)
    public PasswordResponse validateResetToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null || !resetToken.isValid()) {
            return PasswordResponse.builder()
                    .success(false)
                    .message("유효하지 않거나 만료된 토큰")
                    .build();
        }

        return PasswordResponse.builder()
                .success(true)
                .message("토큰 사용 가능")
                .build();
    }

    /**
     * 만료된 토큰을 정리하는 작업 / 새벽 2시
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredAndUsedTokens(LocalDateTime.now());
        log.info("이미 사용된 / 만료된 재설정 토큰을 정리했습니다!!!");
    }
}