package com.doanltmmt.Backend.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Điểm DUY NHẤT để gửi email. Không gửi ngay: phát sự kiện để {@link MailDispatcher} gửi SAU KHI transaction
 * hiện tại commit (rollback thì không gửi), trên luồng riêng.
 */
@Service
public class EmailService {

    /** Email cần gửi; {@code htmlBody} là HTML (tạo bằng {@link EmailTemplates}). */
    public record MailRequested(String to, String subject, String htmlBody) {
    }

    private final ApplicationEventPublisher events;

    public EmailService(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void sendEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) return;
        events.publishEvent(new MailRequested(to.trim(), subject, htmlBody));
    }
}
