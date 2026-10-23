package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.response.NotificationDTO;
import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.util.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông báo trong ứng dụng: tạo (lưu + đẩy realtime qua WebSocket sau khi commit) và đọc/đánh dấu đã đọc.
 * Mọi nơi tạo thông báo dùng {@link #notify} / {@link #publish} thay vì tự lưu và tự đẩy sự kiện.
 */
@Service
@SuppressWarnings("null")
public class NotificationService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final TopicRegistrationRepository regRepo;
    private final EventPublisher events;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;

    public NotificationService(NotificationRepository notificationRepo, UserRepository userRepo,
                               TopicRegistrationRepository regRepo, EventPublisher events,
                               AuditLogService auditLogService, SecurityScopeService scope) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.regRepo = regRepo;
        this.events = events;
        this.auditLogService = auditLogService;
        this.scope = scope;
    }

    // ------------------------------------------------------------------ tạo

    /** Thông báo loại INFO gắn với một đối tượng ({@code refType}/{@code refId}) và đường dẫn mở trên giao diện. */
    @Transactional
    public Notification notify(User user, String title, String content, String refType, Long refId, String linkPath) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(NotificationType.INFO);
        n.setTitle(title);
        n.setContent(content);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setLinkPath(linkPath);
        return publish(n);
    }

    /** Lưu một thông báo đã dựng sẵn (vd. nhắc hạn có mức độ, hạn chót) và đẩy realtime. */
    @Transactional
    public Notification publish(Notification n) {
        Notification saved = notificationRepo.save(n);
        events.notificationCreated(saved);
        return saved;
    }

    /** ADMIN gửi cho bất kỳ ai; giảng viên CHỈ gửi cho sinh viên đã đăng ký đề tài do mình hướng dẫn. */
    @Transactional
    public NotificationDTO createManual(Long userId, String title, String content, NotificationType type) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người nhận"));
        if (!scope.hasRole("ADMIN")) {
            User me = scope.requireCurrentUser();
            if (!regRepo.existsByTopic_Lecturer_IdAndStudent_Id(me.getId(), u.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn chỉ được gửi thông báo cho sinh viên đã đăng ký đề tài do bạn hướng dẫn.");
            }
        }
        Notification n = new Notification();
        n.setUser(u);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        Notification saved = publish(n);
        auditLogService.log("CREATE_NOTIFICATION", "Notification", saved.getId().toString(), "Gửi thủ công cho user " + userId);
        return NotificationDTO.from(saved);
    }

    // ------------------------------------------------------------------ đọc

    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO> mine(Long userId, int page, int size) {
        return PageResponse.of(notificationRepo.findByUser_Id(target(userId), Pages.of(page, size, NEWEST_FIRST)),
                NotificationDTO::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepo.countByUser_IdAndReadAtIsNull(target(userId));
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> recent(Long userId, int limit) {
        // LIMIT ở SQL (trước đây tải hết rồi mới cắt)
        return notificationRepo.findByUser_Id(target(userId), Pages.of(0, limit, NEWEST_FIRST)).stream()
                .map(NotificationDTO::from).toList();
    }

    @Transactional
    public NotificationDTO markRead(Long id) {
        Notification n = notificationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));
        if (!scope.hasRole("ADMIN")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (n.getUser() == null || n.getUser().getId() == null || !currentUserId.equals(n.getUser().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
            }
        }
        n.setReadAt(LocalDateTime.now());
        return NotificationDTO.from(notificationRepo.save(n));
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepo.findByUser_IdAndReadAtIsNull(target(userId));
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> n.setReadAt(now));
        notificationRepo.saveAll(unread);
    }

    /** Chỉ ADMIN xem được thông báo của người khác; người khác luôn là chính mình (bỏ qua userId gửi lên). */
    private Long target(Long userId) {
        return scope.hasRole("ADMIN") ? userId : scope.requireCurrentUser().getId();
    }
}
