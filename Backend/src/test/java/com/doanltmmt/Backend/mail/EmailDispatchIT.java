package com.doanltmmt.Backend.mail;

import com.doanltmmt.Backend.service.EmailService;
import com.doanltmmt.Backend.service.EmailTemplates;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** P2-06 — email gửi bất đồng bộ, chỉ sau khi transaction commit, có thử lại khi lỗi SMTP. */
@SpringBootTest(properties = "app.mail.retry-delay-ms=10")
class EmailDispatchIT {

    @MockitoBean private JavaMailSender mailSender;
    @Autowired private EmailService emailService;
    @Autowired private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        clearInvocations(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private void send(String topicTitle) {
        EmailTemplates.Mail mail = EmailTemplates.registrationApproved(topicTitle);
        emailService.sendEmail("sv@test.local", mail.subject(), mail.html());
    }

    @Test
    void sentAfterCommit_onMailThread_asVietnameseHtml() throws Exception {
        AtomicReference<String> thread = new AtomicReference<>();
        doAnswer(i -> { thread.set(Thread.currentThread().getName()); return null; }).when(mailSender).send(any(MimeMessage.class));

        tx.executeWithoutResult(s -> send("Hệ thống <b>quản lý</b>"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(3000)).send(captor.capture());
        MimeMessage msg = captor.getValue();
        assertEquals("Taskify - Đăng ký đề tài đã được duyệt", msg.getSubject());
        assertTrue(thread.get().startsWith("mail-"), "phải gửi trên luồng riêng, thực tế: " + thread.get());
        msg.saveChanges();
        java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
        msg.writeTo(raw);
        String body = raw.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("text/html") && body.contains("text/plain"), "phải có cả bản HTML và văn bản thuần");
    }

    @Test
    void notSent_whenTransactionRollsBack() {
        tx.executeWithoutResult(s -> {
            send("Đề tài bị hủy");
            s.setRollbackOnly();
        });
        verify(mailSender, after(500).never()).send(any(MimeMessage.class));
    }

    @Test
    void sentImmediately_whenNoTransaction() {
        send("Không trong transaction");
        verify(mailSender, timeout(3000)).send(any(MimeMessage.class));
    }

    @Test
    void retriesTemporarySmtpFailures() {
        doThrow(new MailSendException("SMTP tạm lỗi")).doThrow(new MailSendException("SMTP tạm lỗi")).doNothing()
                .when(mailSender).send(any(MimeMessage.class));
        send("Thử lại");
        verify(mailSender, timeout(3000).times(3)).send(any(MimeMessage.class));
    }

    @Test
    void givesUpAfterThreeAttempts() {
        doThrow(new MailSendException("SMTP hỏng")).when(mailSender).send(any(MimeMessage.class));
        send("Hỏng hẳn");
        verify(mailSender, timeout(3000).times(3)).send(any(MimeMessage.class));
        verify(mailSender, after(300).times(3)).send(any(MimeMessage.class));
    }
}
