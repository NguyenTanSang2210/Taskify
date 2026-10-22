package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Kiểm tra quyền THEO ĐỐI TƯỢNG (đề tài) — lớp bảo vệ thứ hai sau {@code @PreAuthorize} theo vai trò.
 *
 * <p>Chỉ kiểm tra vai trò là chưa đủ: một giảng viên vẫn có vai trò LECTURER khi thao tác trên đề tài của
 * giảng viên khác (lỗi IDOR — Insecure Direct Object Reference). Lớp này trả lời "người này có liên quan tới
 * ĐỀ TÀI này không?":
 *
 * <table>
 *   <caption>Quan hệ với đề tài</caption>
 *   <tr><th>Vai trò</th><th>owner (quản lý)</th><th>member (xem / tham gia)</th></tr>
 *   <tr><td>ADMIN</td><td>luôn</td><td>luôn</td></tr>
 *   <tr><td>DEPARTMENT_ADMIN</td><td>đề tài thuộc workspace của khoa mình</td><td>= owner</td></tr>
 *   <tr><td>LECTURER</td><td>là giảng viên hướng dẫn của đề tài</td><td>= owner</td></tr>
 *   <tr><td>STUDENT</td><td>không</td><td>có đăng ký ĐÃ DUYỆT với đề tài</td></tr>
 *   <tr><td>khác (vai trò tùy biến)</td><td>không</td><td>không</td></tr>
 * </table>
 *
 * <p>Có hai dạng phương thức: dạng lấy người dùng hiện tại từ SecurityContext ({@code require*}) cho controller,
 * và dạng nhận người dùng tường minh ({@code is*}) — bắt buộc cho WebSocket (không có SecurityContext) và để kiểm
 * tra một người khác (ví dụ người NHẬN tin nhắn).
 */
@Service
public class TopicAccessService {

    static final String MSG_NOT_OWNER = "Bạn không có quyền quản lý đề tài này.";
    static final String MSG_NOT_MEMBER = "Bạn không thuộc đề tài này nên không có quyền truy cập.";

    private final TopicRepository topicRepo;
    private final TopicRegistrationRepository regRepo;
    private final SecurityScopeService scope;

    public TopicAccessService(TopicRepository topicRepo, TopicRegistrationRepository regRepo, SecurityScopeService scope) {
        this.topicRepo = topicRepo;
        this.regRepo = regRepo;
        this.scope = scope;
    }

    /** Nạp đề tài; 400 nếu thiếu id, 404 nếu không tồn tại. */
    public Topic loadTopicOr404(Long topicId) {
        if (topicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topicId là bắt buộc");
        }
        return topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài"));
    }

    // ------------------------------------------------------------------ dạng nhận người dùng tường minh

    /** Quản lý được đề tài: ADMIN, DEPARTMENT_ADMIN của khoa chứa đề tài, hoặc giảng viên hướng dẫn. */
    public boolean isOwner(Topic topic, User user) {
        if (topic == null || user == null) {
            return false;
        }
        return switch (roleName(user)) {
            case "ADMIN" -> true;
            case "DEPARTMENT_ADMIN" -> sameDepartment(topic, user);
            case "LECTURER" -> topic.getLecturer() != null
                    && topic.getLecturer().getId() != null
                    && Objects.equals(topic.getLecturer().getId(), user.getId());
            default -> false;
        };
    }

    /** Thành viên đề tài: mọi owner, cộng sinh viên có đăng ký ĐÃ DUYỆT. */
    public boolean isMember(Topic topic, User user) {
        if (isOwner(topic, user)) {
            return true;
        }
        return topic != null && user != null && user.getId() != null
                && "STUDENT".equals(roleName(user))
                && regRepo.existsByStudent_IdAndTopic_IdAndApprovedTrue(user.getId(), topic.getId());
    }

    // ------------------------------------------------------------------ dạng dùng người dùng hiện tại (cho controller)

    public void requireOwner(Topic topic) {
        if (!isOwner(topic, scope.requireCurrentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MSG_NOT_OWNER);
        }
    }

    public void requireMember(Topic topic) {
        if (!isMember(topic, scope.requireCurrentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MSG_NOT_MEMBER);
        }
    }

    // ------------------------------------------------------------------ nội bộ

    private static String roleName(User user) {
        return user.getRole() != null && user.getRole().getName() != null ? user.getRole().getName() : "";
    }

    private static boolean sameDepartment(Topic topic, User user) {
        return topic.getWorkspace() != null
                && topic.getWorkspace().getDepartment() != null
                && topic.getWorkspace().getDepartment().getId() != null
                && user.getDepartment() != null
                && Objects.equals(topic.getWorkspace().getDepartment().getId(), user.getDepartment().getId());
    }
}
