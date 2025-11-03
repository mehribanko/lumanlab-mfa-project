package com.lumonlab.childcaremfa.feat.mfa.service;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lumonlab.childcaremfa.common.config.MfaProperties;
import com.lumonlab.childcaremfa.feat.audit.entity.AuditStatus;
import com.lumonlab.childcaremfa.feat.audit.service.AuditService;
import com.lumonlab.childcaremfa.feat.mfa.dto.MfaSetupResponse;
import com.lumonlab.childcaremfa.feat.mfa.dto.MfaStatusResponse;
import com.lumonlab.childcaremfa.feat.user.entity.Role;
import com.lumonlab.childcaremfa.feat.user.entity.User;
import com.lumonlab.childcaremfa.feat.user.repo.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final UserRepository userRepository;
    private final MfaProperties mfaProperties;
    private final AuditService auditService;

    /**
     * MFA 코드 생성  / QR code
     */
    @Transactional
    public MfaSetupResponse setupMfa(Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // MFA 체크
        if (user.getMfaEnabled()) {
            throw new RuntimeException("MFA 이미 설정 되어 있습니다.");
        }

        // 시크릿 생성
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        // 코드 저장 / 인증 no
        user.setMfaSecret(secret);
        userRepository.save(user);

        // qr code url 생성
        String accountName = user.getEmail();
        String qrCodeUrl = generateQrCodeUrl(secret, accountName);

        // qr image 생성
        String qrCodeImage = generateQrCodeImage(qrCodeUrl);

        auditService.logAuthEvent(user, "MFA_SETUP_INITIATED", AuditStatus.SUCCESS,
                request, Map.of("email", user.getEmail()));

        log.info("MFA 설정 완료 : {}", userId);

        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .qrCodeImage(qrCodeImage)
                .issuer(mfaProperties.getIssuer())
                .accountName(accountName)
                .message("인증 앱 (Google Authenticator 등)으로 QR 코드를 스캔하세요 " +
                        "그리고 나서 코드로 확인하여 설정을 완료합니다.")
                .build();
    }

    /**
     * TOTP 코드 확인 및 MFA 활성화
     */
    @Transactional
    public MfaStatusResponse verifyAndEnableMfa(Long userId, String code, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 비밀이 존재하는지 확인
        if (user.getMfaSecret() == null || user.getMfaSecret().isEmpty()) {
            throw new RuntimeException("MFA가 설정되지 않았습니다. 먼저 /mfa/setup에 접속하세요");
        }

        // 코드 확인
        boolean isValid = verifyCode(user.getMfaSecret(), code);

        if (!isValid) {
            auditService.logAuthEvent(user, "MFA_VERIFICATION_FAILED", AuditStatus.FAILURE,
                    request, Map.of("reason", "Invalid code"));
            throw new RuntimeException("잘못된 MFA 코드");
        }

        // MFA 활성화
        user.setMfaEnabled(true);
        userRepository.save(user);

        auditService.logAuthEvent(user, "MFA_ENABLED", AuditStatus.SUCCESS,
                request, Map.of("email", user.getEmail()));

        log.info("사용자를 위한 MFA 활성화: {}", userId);

        return MfaStatusResponse.builder()
                .mfaEnabled(true)
                .mfaEnforced(isMfaEnforced(user))
                .message("MFA가 활성화되었습니다")
                .build();
    }

    /**
     * 로그인시 사용되는 TOTP 코드 확인
     */
    public boolean verifyMfaCode(User user, String code) {
        if (user.getMfaSecret() == null || user.getMfaSecret().isEmpty()) {
            return false;
        }
        return verifyCode(user.getMfaSecret(), code);
    }

    /**
     * MFA 비활성화 /보안을 위해 현재 MFA 코드 필요
     */
    @Transactional
    public MfaStatusResponse disableMfa(Long userId, String code, HttpServletRequest request) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (!user.getMfaEnabled()) {
            throw new RuntimeException("MFA가 활성화되지 않았습니다.");
        }

        // 사용자의 role 대해 MFA가 적용되는지 확인
        if (isMfaEnforced(user)) {
            throw new RuntimeException("MFA는 ADMIN 및 MASTER roles에 대해 비활성화할 수 없습니다.");
        }

        // 비활성화하기 전에 현재 코드 확인
        boolean isValid = verifyCode(user.getMfaSecret(), code);
        if (!isValid) {
            auditService.logAuthEvent(user, "MFA_DISABLE_FAILED", AuditStatus.FAILURE,
                    request, Map.of("reason", "Invalid code"));
            throw new RuntimeException("잘못된 MFA 코드");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        // Log audit event
        auditService.logAuthEvent(user, "MFA_DISABLED", AuditStatus.SUCCESS,
                request, Map.of("email", user.getEmail()));

        log.info("사용자가 MFA를 사용할 수 없도록 설정됨: {}", userId);

        return MfaStatusResponse.builder()
                .mfaEnabled(false)
                .mfaEnforced(false)
                .message("MFA가 비활성화되었습니다.")
                .build();
    }

    /**
     * 사용자 MFA 상태 확인
     */
    @Transactional(readOnly = true)
    public MfaStatusResponse getMfaStatus(Long userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        boolean enforced = isMfaEnforced(user);

        return MfaStatusResponse.builder()
                .mfaEnabled(user.getMfaEnabled())
                .mfaEnforced(enforced)
                .message(enforced ? "MFA는 admin role에 필수적입니다" :
                        user.getMfaEnabled() ? "MFA가 활성화됨" : "MFA가 비활성화됨")
                .build();
    }


    /**
     * role에 따라 사용자 MFA가 적용되는지 확인
     */
    public boolean isMfaEnforced(User user) {
        return user.hasRole(Role.ADMIN) || user.hasRole(Role.MASTER);
    }

    /**
     * TOTP code 확인
     */
    private boolean verifyCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        return verifier.isValidCode(secret, code);
    }

    /**
     * QR code URL 생성
     */
    private String generateQrCodeUrl(String secret, String accountName) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                mfaProperties.getIssuer(),
                accountName,
                secret,
                mfaProperties.getIssuer(),
                mfaProperties.getDigits(),
                mfaProperties.getPeriod()
        );
    }

    /**
     * QR 이미지 생성 / Base64
     */
    private String generateQrCodeImage(String qrCodeUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 300, 300);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (WriterException | IOException e) {
            log.error("QR 코드 생성 실패", e);
            throw new RuntimeException("QR 코드 생성 실패", e);
        }
    }
}