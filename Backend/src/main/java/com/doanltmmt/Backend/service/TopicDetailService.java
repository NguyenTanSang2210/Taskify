package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.response.StudentSummaryDTO;
import com.doanltmmt.Backend.dto.response.TopicDetailDTO;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.TopicSpecifications;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * P4-11 — chi tiết một đề tài. Ai thấy đề tài trong danh sách thì xem được chi tiết: Quản trị khoa chỉ khoa mình,
 * sinh viên chỉ đợt của lớp mình hoặc đề tài đã đăng ký; ngoài phạm vi → 404 như không tồn tại.
 */
@Service
public class TopicDetailService {

    private final TopicAccessService topicAccess;
    private final TopicRegistrationRepository regRepo;
    private final TopicRepository topicRepo;
    private final SecurityScopeService scope;

    public TopicDetailService(TopicAccessService topicAccess, TopicRegistrationRepository regRepo,
                              TopicRepository topicRepo, SecurityScopeService scope) {
        this.topicAccess = topicAccess;
        this.regRepo = regRepo;
        this.topicRepo = topicRepo;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public TopicDetailDTO get(Long id) {
        Topic topic = topicAccess.loadTopicOr404(id);
        User me = scope.requireCurrentUser();
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN") && !topicAccess.isOwner(topic, me)) {
            // giống danh sách: quản trị khoa không thấy đề tài khoa khác
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài");
        }
        if (scope.hasRole("STUDENT") && !scope.hasRole("ADMIN")
                && !topicRepo.exists(TopicSpecifications.visibleToStudent(me.getId()).and(TopicSpecifications.hasId(topic.getId())))) {
            // giống danh sách: sinh viên không thấy đề tài của đợt không gắn với lớp mình
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài");
        }

        List<TopicRegistration> regs = regRepo.findByTopic_Id(topic.getId());
        long approved = regs.stream().filter(r -> Boolean.TRUE.equals(r.getApproved())).count();
        long pending = regs.stream().filter(r -> r.getApproved() == null).count();
        boolean owner = topicAccess.isOwner(topic, me);
        boolean member = owner || topicAccess.isMember(topic, me);

        List<StudentSummaryDTO> members = !member ? null : regs.stream()
                .filter(r -> Boolean.TRUE.equals(r.getApproved()))
                .map(r -> StudentSummaryDTO.from(r.getStudent()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(s -> s.user() == null || s.user().fullName() == null ? "" : s.user().fullName()))
                .toList();

        return new TopicDetailDTO(topic.getId(), topic.getTitle(), topic.getDescription(), topic.getStatus(),
                topic.getCapacity() == null ? 1 : topic.getCapacity(), approved, pending,
                myRegistrationStatus(regs, me), member, owner,
                lecturer(topic.getLecturer()), workspace(topic.getWorkspace()), members);
    }

    /** Trạng thái đăng ký mới nhất của SV đang xem (cùng mã với danh sách đề tài); null nếu không phải SV. */
    private String myRegistrationStatus(List<TopicRegistration> regs, User me) {
        if (!scope.hasRole("STUDENT")) return null;
        return regs.stream()
                .filter(r -> r.getStudent() != null && Objects.equals(r.getStudent().getId(), me.getId()))
                .max(Comparator.comparing(TopicRegistration::getRegisteredAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(TopicRegistration::getId))
                .map(r -> r.getApproved() == null ? "CHO_DUYET" : r.getApproved() ? "DA_DUYET" : "TU_CHOI")
                .orElse("CHUA_DANG_KY");
    }

    private static TopicDetailDTO.Lecturer lecturer(Lecturer l) {
        if (l == null) return null;
        User u = l.getUser();
        return new TopicDetailDTO.Lecturer(l.getId(), u == null ? null : u.getFullName(), u == null ? null : u.getEmail(),
                l.getDegree(), l.getSpeciality(), l.getDepartment() == null ? null : l.getDepartment().getName());
    }

    private static TopicDetailDTO.Workspace workspace(Workspace w) {
        return w == null ? null
                : new TopicDetailDTO.Workspace(w.getId(), w.getName(), w.getSemester(), w.getStatus(), w.getStartAt(), w.getEndAt());
    }
}
