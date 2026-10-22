package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.config.AsyncConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;

/**
 * Gửi email thật. Chạy SAU KHI transaction commit ({@code fallbackExecution}: không có transaction thì chạy ngay),
 * bất đồng bộ trên {@link AsyncConfig#MAIL_EXECUTOR}; lỗi SMTP tạm thời thử lại tối đa 2 lần.
 */
@Component
public class MailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MailDispatcher.class);
    static final int MAX_ATTEMPTS = 3;

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String fromEmail;
    private final long retryDelayMillis;

    public MailDispatcher(ObjectProvider<JavaMailSender> mailSender,
                          @Value("${spring.mail.username:}") String fromEmail,
                          @Value("${app.mail.retry-delay-ms:2000}") long retryDelayMillis) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMailRequested(EmailService.MailRequested mail) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Chưa cấu hình máy chủ email — bỏ qua email \"{}\"", mail.subject());
            return;
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                sender.send(build(sender, mail));
                log.info("Đã gửi email \"{}\"", mail.subject());
                return;
            } catch (MailException | MessagingException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Gửi email \"{}\" thất bại sau {} lần: {}", mail.subject(), MAX_ATTEMPTS, e.getMessage());
                    return;
                }
                log.warn("Gửi email \"{}\" lỗi (lần {}), thử lại: {}", mail.subject(), attempt, e.getMessage());
                sleep(retryDelayMillis * attempt);
            }
        }
    }

    private MimeMessage build(JavaMailSender sender, EmailService.MailRequested mail) throws MessagingException {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        if (fromEmail != null && !fromEmail.isBlank()) {
            helper.setFrom("Taskify <" + fromEmail + ">");
        }
        helper.setTo(mail.to());
        helper.setSubject(mail.subject());
        helper.setText(EmailTemplates.toPlainText(mail.htmlBody()), mail.htmlBody());
        return message;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
