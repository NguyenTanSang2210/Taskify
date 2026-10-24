package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.EmailDigestItem;
import com.doanltmmt.Backend.entity.NotificationSettings;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.enums.EmailCategory;
import com.doanltmmt.Backend.repository.EmailDigestItemRepository;
import com.doanltmmt.Backend.repository.NotificationSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * P4-06 — gửi email THÔNG BÁO theo cài đặt của người nhận: nhóm bị tắt thì bỏ qua (thông báo trong app vẫn có);
 * chế độ tổng hợp thì xếp vào hàng chờ, gửi gộp MỘT thư mỗi sáng. Email bảo mật (OTP, kích hoạt, đặt lại mật khẩu)
 * KHÔNG đi qua lớp này — luôn gửi ngay qua {@link EmailService}.
 */
@Service
public class NotificationMailer {

    private static final Logger log = LoggerFactory.getLogger(NotificationMailer.class);

    private final EmailService emailService;
    private final NotificationSettingsRepository settingsRepo;
    private final EmailDigestItemRepository digestRepo;
    private final TransactionTemplate tx;

    public NotificationMailer(EmailService emailService, NotificationSettingsRepository settingsRepo,
                              EmailDigestItemRepository digestRepo, TransactionTemplate tx) {
        this.emailService = emailService;
        this.settingsRepo = settingsRepo;
        this.digestRepo = digestRepo;
        this.tx = tx;
    }

    public void send(User to, EmailCategory category, EmailTemplates.Mail mail) {
        if (to == null || to.getEmail() == null || to.getEmail().isBlank()) return;
        NotificationSettings settings = to.getId() == null ? null : settingsRepo.findById(to.getId()).orElse(null);
        if (settings != null && !settings.emailEnabled(category)) return;
        if (settings != null && settings.isEmailDigest()) {
            EmailDigestItem item = new EmailDigestItem();
            item.setUser(to);
            item.setCategory(category);
            item.setSubject(mail.subject().length() > 255 ? mail.subject().substring(0, 255) : mail.subject());
            item.setBody(EmailTemplates.toPlainText(mail.html()));
            item.setCreatedAt(LocalDateTime.now());
            digestRepo.save(item);
            return;
        }
        emailService.sendEmail(to.getEmail(), mail.subject(), mail.html());
    }

    /** 07:30 hằng ngày (giờ Việt Nam): mỗi người có thông báo đang chờ nhận MỘT thư tổng hợp. Trả về số thư đã gửi. */
    public int sendDigests() {
        int sent = 0;
        for (Long userId : digestRepo.findPendingUserIds()) {
            Boolean ok = tx.execute(status -> {
                List<EmailDigestItem> items = digestRepo.findByUserIdOrdered(userId);
                if (items.isEmpty()) return false;
                User user = items.get(0).getUser();
                EmailTemplates.Mail mail = EmailTemplates.digest(items.stream().map(i -> new String[]{i.getSubject(), i.getBody()}).toList());
                emailService.sendEmail(user.getEmail(), mail.subject(), mail.html()); // gửi sau khi transaction commit
                digestRepo.deleteAllByIdIn(items.stream().map(EmailDigestItem::getId).toList());
                return true;
            });
            if (Boolean.TRUE.equals(ok)) sent++;
        }
        if (sent > 0) log.info("Đã gửi {} thư tổng hợp thông báo", sent);
        return sent;
    }
}
