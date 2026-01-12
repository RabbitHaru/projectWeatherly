package me.shinsunyoung.projectweatherly.member.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;



@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendPasswordResetVerificationCode(String toEmail, String verificationCode, String username) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("expiryMinutes", 10);

            String htmlContent = templateEngine.process("email/password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Weatherly] 비밀번호 재설정 인증번호");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("비밀번호 재설정 인증번호 이메일 발송 완료: {}", toEmail);

        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    @Async
    public void sendPasswordChangedNotification(String toEmail, String username) {
        try {
            Context context = new Context();
            context.setVariable("username", username);

            String htmlContent = templateEngine.process("email/password-changed", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Weatherly] 비밀번호 변경 완료 안내");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("비밀번호 변경 완료 이메일 발송 완료: {}", toEmail);

        } catch (Exception e) {
            log.error("비밀번호 변경 완료 이메일 발송 실패: {}", toEmail, e);
            // 실패해도 비밀번호 변경은 이미 완료되었으므로 예외를 던지지 않음
        }
    }
}