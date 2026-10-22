package com.doanltmmt.Backend.service;

import org.springframework.web.util.HtmlUtils;

import java.util.Locale;

/**
 * Mẫu email HTML tiếng Việt. Mọi dữ liệu do người dùng nhập (tên đề tài, nhận xét, lý do...) đều được escape HTML.
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    /** Một email: tiêu đề + nội dung HTML. */
    public record Mail(String subject, String html) {
    }

    private static final String FOOTER = "Email này được gửi tự động từ hệ thống Taskify, vui lòng không trả lời.";

    private static String esc(String text) {
        // UTF-8: chỉ escape ký tự đặc biệt của HTML, giữ nguyên chữ tiếng Việt
        return text == null ? "" : HtmlUtils.htmlEscape(text, "UTF-8");
    }

    private static String layout(String heading, String bodyHtml) {
        return """
                <!doctype html><html lang="vi"><body style="margin:0;padding:24px;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;color:#1b1f2a">
                <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:28px;border:1px solid #e3e7f0">
                <div style="font-size:13px;font-weight:bold;letter-spacing:2px;color:#3b5bdb;text-transform:uppercase">Taskify</div>
                <h2 style="margin:8px 0 16px;font-size:20px">%s</h2>
                %s
                <p style="margin-top:28px;font-size:12px;color:#8a92a6">%s</p>
                </div></body></html>""".formatted(esc(heading), bodyHtml, FOOTER);
    }

    private static String p(String html) {
        return "<p style=\"font-size:15px;line-height:1.6;margin:0 0 12px\">" + html + "</p>";
    }

    private static String code(String code) {
        return "<div style=\"font-size:30px;font-weight:bold;letter-spacing:8px;background:#eef2ff;color:#1e3a8a;"
                + "border-radius:10px;padding:14px;text-align:center;margin:8px 0 16px\">" + esc(code) + "</div>";
    }

    private static final String DO_NOT_SHARE =
            "Không chia sẻ mã này cho bất kỳ ai — nhân viên Taskify không bao giờ hỏi mã của bạn.";

    // ------------------------------------------------------------------ xác thực

    public static Mail loginOtp(String otp) {
        return new Mail("Taskify - Mã xác thực đăng nhập", layout("Mã xác thực đăng nhập",
                p("Mã xác thực đăng nhập của bạn là:") + code(otp)
                        + p("Mã có hiệu lực trong <b>5 phút</b>. " + DO_NOT_SHARE)
                        + p("Nếu không phải bạn đang đăng nhập, hãy đổi mật khẩu ngay.")));
    }

    public static Mail activation(String otp) {
        return new Mail("Taskify - Kích hoạt tài khoản", layout("Kích hoạt tài khoản",
                p("Tài khoản Taskify của bạn đã được tạo. Mã kích hoạt:") + code(otp)
                        + p("Mã có hiệu lực trong <b>72 giờ</b>. Mở trang đăng nhập → <b>Kích hoạt tài khoản</b>, "
                        + "nhập tên đăng nhập (hoặc email) cùng mã này để đặt mật khẩu.")
                        + p(DO_NOT_SHARE)));
    }

    public static Mail resetPassword(String otp) {
        return new Mail("Taskify - Mã đặt lại mật khẩu", layout("Đặt lại mật khẩu",
                p("Mã đặt lại mật khẩu của bạn là:") + code(otp)
                        + p("Mã có hiệu lực trong <b>10 phút</b>. " + DO_NOT_SHARE)
                        + p("Nếu không phải bạn yêu cầu, hãy bỏ qua email này — mật khẩu hiện tại vẫn giữ nguyên.")));
    }

    // ------------------------------------------------------------------ đăng ký đề tài

    public static Mail registrationApproved(String topicTitle) {
        return new Mail("Taskify - Đăng ký đề tài đã được duyệt", layout("Đăng ký đề tài đã được duyệt",
                p("Giảng viên đã <b>duyệt</b> đăng ký của bạn cho đề tài <b>" + esc(topicTitle) + "</b>.")
                        + p("Vào mục <b>Đăng ký của tôi</b> trên Taskify để xem chi tiết và bắt đầu báo cáo tiến độ.")));
    }

    public static Mail registrationRejected(String topicTitle, String reason) {
        String why = reason == null || reason.isBlank() ? "" : p("Lý do: <i>" + esc(reason) + "</i>");
        return new Mail("Taskify - Đăng ký đề tài bị từ chối", layout("Đăng ký đề tài bị từ chối",
                p("Đăng ký của bạn cho đề tài <b>" + esc(topicTitle) + "</b> đã bị từ chối.") + why
                        + p("Bạn có thể đăng ký đề tài khác khi đợt đăng ký còn mở.")));
    }

    // ------------------------------------------------------------------ tiến độ & điểm

    public static Mail progressSubmitted(String studentName, String reportTitle, String topicTitle) {
        return new Mail("Taskify - Sinh viên nộp báo cáo tiến độ", layout("Có báo cáo tiến độ mới",
                p("Sinh viên <b>" + esc(studentName) + "</b> vừa nộp báo cáo <b>" + esc(reportTitle)
                        + "</b> cho đề tài <b>" + esc(topicTitle) + "</b>.")
                        + p("Vào mục <b>Tiến độ sinh viên</b> trên Taskify để xem và nhận xét.")));
    }

    public static Mail progressFeedback(String reportTitle, String comment) {
        String body = comment == null || comment.isBlank()
                ? p("Báo cáo <b>" + esc(reportTitle) + "</b> của bạn vừa được giảng viên cập nhật.")
                : p("Giảng viên đã nhận xét báo cáo <b>" + esc(reportTitle) + "</b>:")
                + "<blockquote style=\"margin:0 0 12px;padding:10px 14px;border-left:4px solid #3b5bdb;background:#f6f8ff\">"
                + esc(comment) + "</blockquote>";
        return new Mail("Taskify - Giảng viên nhận xét báo cáo", layout("Nhận xét báo cáo tiến độ", body));
    }

    public static Mail graded(String topicTitle, Double score, String feedback) {
        String fb = feedback == null || feedback.isBlank() ? "" : p("Nhận xét: <i>" + esc(feedback) + "</i>");
        return new Mail("Taskify - Đề tài đã có điểm", layout("Đề tài đã có điểm",
                p("Đề tài <b>" + esc(topicTitle) + "</b> của bạn đã được chấm: <b>" + formatScore(score) + "</b> điểm.") + fb));
    }

    // ------------------------------------------------------------------ nhắc việc

    public static Mail progressReminder(String topicTitle, int days) {
        return new Mail("Taskify - Nhắc gửi báo cáo tiến độ", layout("Nhắc gửi báo cáo tiến độ",
                p("Bạn chưa gửi báo cáo tiến độ nào trong <b>" + days + " ngày</b> gần đây cho đề tài <b>"
                        + esc(topicTitle) + "</b>.")
                        + p("Vào mục <b>Tiến độ</b> trên Taskify để cập nhật cho giảng viên hướng dẫn.")));
    }

    public static Mail milestoneReminder(String milestoneTitle, String topicTitle, String deadline, boolean dueToday) {
        String when = dueToday ? "<b>hết hạn hôm nay</b>" : "sẽ hết hạn vào <b>" + esc(deadline) + "</b>";
        return new Mail("Taskify - Nhắc hạn mốc " + (dueToday ? "(hôm nay)" : "sắp tới"), layout("Nhắc hạn mốc tiến độ",
                p("Mốc <b>" + esc(milestoneTitle) + "</b> của đề tài <b>" + esc(topicTitle) + "</b> " + when + ".")
                        + p("Hãy nộp báo cáo cho mốc này trước hạn để giảng viên kịp nhận xét.")));
    }

    public static String formatScore(Double score) {
        if (score == null) return "—";
        return score % 1 == 0 ? String.valueOf(score.intValue()) : String.format(Locale.ROOT, "%.2f", score).replaceAll("0+$", "");
    }

    /** Bản văn bản thuần (cho trình đọc email không hiển thị HTML). */
    public static String toPlainText(String html) {
        if (html == null) return "";
        String text = html.replaceAll("(?i)<br\\s*/?>|</p>|</div>|</h2>|</blockquote>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n\n")
                .trim();
        return HtmlUtils.htmlUnescape(text);
    }
}
