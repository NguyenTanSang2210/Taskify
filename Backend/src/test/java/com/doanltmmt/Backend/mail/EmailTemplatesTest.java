package com.doanltmmt.Backend.mail;

import com.doanltmmt.Backend.service.EmailTemplates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplatesTest {

    @Test
    void userContent_isHtmlEscaped() {
        EmailTemplates.Mail mail = EmailTemplates.registrationRejected("<script>alert(1)</script>", "Thiếu \"mô tả\" & mục tiêu");
        assertFalse(mail.html().contains("<script>"));
        assertTrue(mail.html().contains("&lt;script&gt;"));
        assertTrue(mail.html().contains("&amp; mục tiêu"));
    }

    @Test
    void otpMails_containTheCode_inVietnamese() {
        assertTrue(EmailTemplates.loginOtp("123456").html().contains("123456"));
        assertEquals("Taskify - Mã xác thực đăng nhập", EmailTemplates.loginOtp("1").subject());
        assertTrue(EmailTemplates.resetPassword("654321").html().contains("10 phút"));
        assertTrue(EmailTemplates.activation("111222").html().contains("72 giờ"));
    }

    @Test
    void graded_formatsScore() {
        assertTrue(EmailTemplates.graded("Đề tài", 8.0, null).html().contains("<b>8</b>"));
        assertTrue(EmailTemplates.graded("Đề tài", 8.25, "Tốt").html().contains("<b>8.25</b>"));
        assertTrue(EmailTemplates.graded("Đề tài", 7.5, "").html().contains("<b>7.5</b>"));
    }

    @Test
    void plainText_isReadable() {
        String text = EmailTemplates.toPlainText(EmailTemplates.progressFeedback("Báo cáo 1", "Cần bổ sung <số liệu>").html());
        assertTrue(text.contains("Cần bổ sung <số liệu>"));
        assertFalse(text.contains("<p"));
    }
}
