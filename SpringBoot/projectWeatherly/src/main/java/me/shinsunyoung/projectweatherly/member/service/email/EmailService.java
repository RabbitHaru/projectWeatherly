package me.shinsunyoung.projectweatherly.member.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 인증번호 이메일 발송
     */
    @Async
    public void sendVerificationCode(String toEmail, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[ProjectWeatherly] 비밀번호 재설정 인증번호");
            message.setText(
                    "안녕하세요,\n\n" +
                            "비밀번호 재설정을 요청하셨습니다.\n" +
                            "인증번호: " + verificationCode + "\n\n" +
                            "인증번호는 10분 동안 유효합니다.\n" +
                            "만약 비밀번호 재설정을 요청하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.\n\n" +
                            "감사합니다.\n" +
                            "ProjectWeatherly 팀"
            );

            mailSender.send(message);
            log.info("인증번호 이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("인증번호 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * HTML 템플릿을 사용한 이메일 발송 (선택사항)
     */
    @Async
    public void sendVerificationCodeHtml(String toEmail, String verificationCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            Context context = new Context();
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("email/verification-code", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[ProjectWeatherly] 비밀번호 재설정 인증번호");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("HTML 인증번호 이메일 발송 완료: {}", toEmail);
        } catch (MessagingException e) {
            log.error("HTML 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("HTML 이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 비밀번호 변경 완료 안내 이메일
     */
    @Async
    public void sendPasswordChangedNotification(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[ProjectWeatherly] 비밀번호 변경 완료");
            message.setText(
                    "안녕하세요,\n\n" +
                            "비밀번호가 성공적으로 변경되었습니다.\n\n" +
                            "만약 본인이 변경하지 않으셨다면, 즉시 관리자에게 문의해주세요.\n\n" +
                            "감사합니다.\n" +
                            "ProjectWeatherly 팀"
            );

            mailSender.send(message);
            log.info("비밀번호 변경 완료 이메일 발송: {}", toEmail);
        } catch (Exception e) {
            log.error("비밀번호 변경 완료 이메일 발송 실패: {}", toEmail, e);
        }
    }
}