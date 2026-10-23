package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.dto.TopicDTO;
import com.doanltmmt.Backend.entity.*;
import com.doanltmmt.Backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class TopicService {

    private final TopicRepository topicRepo;
    private final LecturerRepository lecturerRepo;
    private final TopicRegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final WorkspaceRepository workspaceRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public TopicService(TopicRepository topicRepo,
                        LecturerRepository lecturerRepo,
                        TopicRegistrationRepository regRepo,
                        UserRepository userRepo,
                        WorkspaceRepository workspaceRepo,
                        SecurityScopeService scope,
                        AuditLogService auditLogService) {
        this.topicRepo = topicRepo;
        this.lecturerRepo = lecturerRepo;
        this.regRepo = regRepo;
        this.userRepo = userRepo;
        this.workspaceRepo = workspaceRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 10;
    private static final int MAX_TITLE = 255;
    private static final int MAX_DESCRIPTION = 10_000;

    @Transactional
    public TopicDTO createTopic(Long lecturerId, Topic topic) {
        if (scope.hasRole("LECTURER")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (!Objects.equals(currentUserId, lecturerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
            }
        }
        topic.setId(null); // luôn tạo mới — không cho ghi đè đề tài khác qua trường id trong body
        if (topic.getTitle() == null || topic.getTitle().isBlank()) {
            throw badRequest("Tên đề tài không được để trống");
        }
        if (topic.getCapacity() == null) topic.setCapacity(MIN_CAPACITY);
        validateFields(topic.getTitle(), topic.getDescription(), topic.getCapacity());
        topic.setTitle(topic.getTitle().trim());
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn đợt cho đề tài.");
        }
        Workspace w = workspaceRepo.findById(topic.getWorkspace().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
        
        requireWorkspaceAllowsTopicCreateOrEdit(w);

        // Quản trị khoa chỉ được tạo đề tài trong workspace của KHOA MÌNH
        // (trước đây chỉ kiểm tra giảng viên thuộc khoa của workspace, không kiểm tra chính quản trị khoa)
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN") && w.getDepartment() != null) {
            scope.requireDepartmentAccess(w.getDepartment().getId());
        }

        Lecturer lecturer = lecturerRepo.findById(lecturerId).orElseGet(() -> {
            User user = userRepo.findById(lecturerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giảng viên."));
            // Chỉ tài khoản giảng viên mới được làm người hướng dẫn (trước đây có thể gán đề tài cho bất kỳ tài khoản nào)
            if (user.getRole() == null || !"LECTURER".equals(user.getRole().getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người hướng dẫn phải là tài khoản giảng viên");
            }
            Lecturer newLecturer = new Lecturer();
            newLecturer.setUser(user);
            newLecturer.setDepartment(user.getDepartment());
            return lecturerRepo.save(newLecturer);
        });

        if (!scope.hasRole("ADMIN")) {
            if (lecturer.getUser() == null || lecturer.getUser().getDepartment() == null || w.getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin khoa.");
            }
            if (!lecturer.getUser().getDepartment().getId().equals(w.getDepartment().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giảng viên không thuộc khoa của đợt.");
            }
        }

        topic.setLecturer(lecturer);
        topic.setStatus(TopicStatus.OPEN);
        topic.setWorkspace(w);

        Topic saved = topicRepo.save(topic);
        auditLogService.log("CREATE_TOPIC", "Topic", saved.getId().toString(), "Title: " + saved.getTitle());
        return new TopicDTO(saved);
    }

    @Transactional
    public TopicDTO updateTopic(Long id, Long lecturerId, Topic payload) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
        
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền sửa đề tài này.");
        }
        
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đề tài chưa thuộc đợt nào.");
        }
        
        requireWorkspaceAllowsTopicCreateOrEdit(topic.getWorkspace());
        
        validateFields(payload.getTitle(), payload.getDescription(), payload.getCapacity());
        if (payload.getTitle() != null) topic.setTitle(payload.getTitle().trim());
        if (payload.getDescription() != null) topic.setDescription(payload.getDescription());
        if (payload.getCapacity() != null) {
            long approved = regRepo.countByTopic_IdAndApprovedTrue(topic.getId());
            if (payload.getCapacity() < approved) {
                throw badRequest("Số lượng sinh viên không được nhỏ hơn số sinh viên đã được duyệt (" + approved + ")");
            }
            topic.setCapacity(payload.getCapacity());
            topic.refreshCapacityStatus(approved);
        }
        // Trạng thái chỉ đổi qua open/close và theo số chỗ — không nhận từ body
        
        Topic saved = topicRepo.save(topic);
        auditLogService.log("UPDATE_TOPIC", "Topic", saved.getId().toString(), "Updated topic details");
        return new TopicDTO(saved);
    }

    @Transactional
    public void deleteTopic(Long id, Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
        
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa đề tài này.");
        }
        
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đề tài chưa thuộc đợt nào.");
        }
        
        requireWorkspaceAllowsTopicCreateOrEdit(topic.getWorkspace());
        topicRepo.deleteById(id);
        auditLogService.log("DELETE_TOPIC", "Topic", id.toString(), "Deleted");
    }

    @Transactional
    public TopicDTO openTopic(Long id, Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền mở đề tài này.");
        }
        requireWorkspaceAllowsTopicOpenClose(topic.getWorkspace());
        topic.setStatus(TopicStatus.OPEN);
        // Mở lại đề tài đã đủ SV được duyệt → FULL (không nhận thêm đăng ký)
        topic.refreshCapacityStatus(regRepo.countByTopic_IdAndApprovedTrue(topic.getId()));
        return new TopicDTO(topicRepo.save(topic));
    }

    @Transactional
    public TopicDTO closeTopic(Long id, Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đóng đề tài này.");
        }
        requireWorkspaceAllowsTopicOpenClose(topic.getWorkspace());
        topic.setStatus(TopicStatus.CLOSED);
        return new TopicDTO(topicRepo.save(topic));
    }

    public Map<String, Object> getAll(Long studentId, String queryText, String status, Long lecturerId, int page, int size) {
        Specification<Topic> spec = Specification.allOf(List.<Specification<Topic>>of());

        if (status != null && !status.isBlank()) {
            TopicStatus wanted = parseTopicStatus(status);
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), wanted));
        }
        if (lecturerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("lecturer").get("id"), lecturerId));
        }
        if (queryText != null && !queryText.isBlank()) {
            String qLower = "%" + queryText.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), qLower));
        }
        
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            Long deptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
            if (deptId != null) {
                Long finalDeptId = deptId;
                spec = spec.and((root, q, cb) -> cb.equal(root.get("workspace").get("department").get("id"), finalDeptId));
            } else {
                return Map.of("items", List.of(), "page", page, "size", size, "totalElements", 0, "totalPages", 0);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Topic> pageRes = topicRepo.findAll(spec, pageable);

        List<Map<String, Object>> items = pageRes.getContent().stream().map(topic -> {
            Map<String, Object> topicMap = new HashMap<>();
            topicMap.put("id", topic.getId());
            topicMap.put("title", topic.getTitle());
            topicMap.put("description", topic.getDescription());
            topicMap.put("status", topic.getStatus());
            topicMap.put("capacity", topic.getCapacity());
            if (topic.getWorkspace() != null) {
                topicMap.put("workspaceId", topic.getWorkspace().getId());
                topicMap.put("workspaceName", topic.getWorkspace().getName());
            }
            
            // Basic lecturer info to avoid full entity deep nesting in this specific map
            if (topic.getLecturer() != null && topic.getLecturer().getUser() != null) {
                topicMap.put("lecturerName", topic.getLecturer().getUser().getFullName());
                topicMap.put("lecturerId", topic.getLecturer().getId());
            }

            topicMap.put("registrationCount", regRepo.countByTopic_Id(topic.getId()));
            topicMap.put("pendingCount", regRepo.countByTopic_IdAndApprovedIsNull(topic.getId()));
            topicMap.put("approvedCount", regRepo.countByTopic_IdAndApprovedTrue(topic.getId()));
            topicMap.put("rejectedCount", regRepo.countByTopic_IdAndApprovedFalse(topic.getId()));

            String regStatus = "CHUA_DANG_KY";
            if (studentId != null) {
                regStatus = regRepo.findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(studentId, topic.getId())
                        .map(reg -> {
                            if (reg.getApproved() == null) return "CHO_DUYET";
                            return reg.getApproved() ? "DA_DUYET" : "TU_CHOI";
                        }).orElse("CHUA_DANG_KY");
            }
            topicMap.put("registrationStatus", regStatus);
            return topicMap;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", pageRes.getNumber());
        response.put("size", pageRes.getSize());
        response.put("totalElements", pageRes.getTotalElements());
        response.put("totalPages", pageRes.getTotalPages());
        return response;
    }

    private boolean canModifyTopic(Topic topic, Long lecturerId) {
        if (scope.hasRole("ADMIN")) return true;
        if (scope.hasRole("DEPARTMENT_ADMIN")) {
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) return false;
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            return true;
        }
        if (!scope.hasRole("LECTURER") || lecturerId == null) return false;
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!Objects.equals(currentUserId, lecturerId)) return false;
        return topic.getLecturer() != null && Objects.equals(topic.getLecturer().getId(), lecturerId);
    }

    private void requireWorkspaceAllowsTopicCreateOrEdit(Workspace w) {
        if (!w.getStatus().allowsTopicEditing()) {
            throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE, "Đợt \"" + w.getName() + "\" không còn nhận tạo/sửa đề tài (trạng thái: " + w.getStatus().label() + ")");
        }
    }

    private void requireWorkspaceAllowsTopicOpenClose(Workspace w) {
        if (!w.getStatus().allowsTopicOpenClose()) {
            throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE, "Chỉ đóng/mở đề tài khi đợt đang mở đề tài hoặc mở đăng ký");
        }
    }

    private static TopicStatus parseTopicStatus(String raw) {
        try {
            return TopicStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw badRequest("Trạng thái đề tài không hợp lệ: " + raw);
        }
    }

    /** Kiểm tra các trường do người dùng nhập; trường {@code null} (không gửi khi sửa) được bỏ qua. */
    private static void validateFields(String title, String description, Integer capacity) {
        if (title != null && (title.isBlank() || title.trim().length() > MAX_TITLE)) {
            throw badRequest("Tên đề tài phải từ 1 đến " + MAX_TITLE + " ký tự");
        }
        if (description != null && description.length() > MAX_DESCRIPTION) {
            throw badRequest("Mô tả đề tài tối đa " + MAX_DESCRIPTION + " ký tự");
        }
        if (capacity != null && (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY)) {
            throw badRequest("Số lượng sinh viên phải từ " + MIN_CAPACITY + " đến " + MAX_CAPACITY);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
