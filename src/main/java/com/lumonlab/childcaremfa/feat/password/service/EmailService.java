package com.lumonlab.childcaremfa.feat.password.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${password-reset.base-url}")
    private String baseUrl;

    /**
     * 비밀번호 재설정 이메일 보내기
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetLink = baseUrl + "/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("비밀번호 재설정 요청 - 루먼랩");
            message.setText(buildPasswordResetEmailBody(resetLink));

            mailSender.send(message);
            log.info("비밀번호 재설정 이메일 전송: {}", toEmail);

        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일을 보내지 못했습니다: {}", toEmail, e);
            throw new RuntimeException("이메일 전송 실패");
        }
    }

    /**
     * 비밀번호 변경 확인 이메일 보내기
     */
    @Async
    public void sendPasswordChangedEmail(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Changed - LumanLab Childcare");
            message.setText(buildPasswordChangedEmailBody());

            mailSender.send(message);
            log.info("비밀번호 변경 확인 메시지 전송: {}", toEmail);

        } catch (Exception e) {
            log.error("비밀번호 변경 이메일을 보내지 못했습니다: {}", toEmail, e);
        }
    }

    private String buildPasswordResetEmailBody(String resetLink) {
        return """
                안녕하세요,
                
                루만랩 육아 계정의 비밀번호 재설정을 요청하셨습니다.
                
                비밀번호를 재설정하려면 아래 링크를 클릭하세요:

                %s
                
                이 링크는 1시간 후에 만료됩니다.
                

                루먼랩 드림,
                
                """.formatted(resetLink);
    }

    private String buildPasswordChangedEmailBody() {
        return """
                안녕하세요,
                
                비밀번호가 성공적으로 변경되었습니다.

                루먼랩 드림,
                """;
    }
}