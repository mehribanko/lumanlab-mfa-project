package com.lumonlab.childcaremfa.feat.audit.service;

import com.lumonlab.childcaremfa.feat.audit.entity.AuditLog;
import com.lumonlab.childcaremfa.feat.audit.entity.AuditStatus;
import com.lumonlab.childcaremfa.feat.audit.repo.AuditLogRepository;
import com.lumonlab.childcaremfa.feat.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void logAuthEvent(User user, String action, AuditStatus status,
                             HttpServletRequest request, Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .status(status)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
        log.info("로그 생성: action={}, user={}, status={}", action, user.getId(), status);
    }

    @Async
    @Transactional
    public void logFailedLogin(String email, String reason, HttpServletRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .action("LOGIN_FAILED")
                .status(AuditStatus.FAILURE)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .details(Map.of("email", email, "reason", reason))
                .build();

        auditLogRepository.save(auditLog);
        log.warn("로그인 접속 시도가 실패했습니다: {}", email);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}