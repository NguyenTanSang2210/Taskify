package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.WorkspaceClass;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/** Điều kiện lọc đề tài dùng chung cho danh sách và chi tiết (một quy tắc, một chỗ). */
public final class TopicSpecifications {

    private TopicSpecifications() {
    }

    /**
     * Đề tài sinh viên {@code studentId} được xem: thuộc đợt đang gắn ({@code workspace_classes.active}) với lớp của
     * sinh viên, HOẶC sinh viên đã đăng ký đề tài (bất kể trạng thái — vẫn xem được khi đổi lớp). Sinh viên chưa có
     * lớp chỉ thấy đề tài mình đã đăng ký.
     */
    public static Specification<Topic> visibleToStudent(Long studentId) {
        return (root, q, cb) -> {
            Subquery<Long> classWorkspaces = q.subquery(Long.class);
            Root<WorkspaceClass> wc = classWorkspaces.from(WorkspaceClass.class);
            Root<Student> s = classWorkspaces.from(Student.class);
            classWorkspaces.select(wc.get("workspace").get("id")).where(
                    cb.equal(s.get("id"), studentId),
                    cb.equal(wc.get("academicClass"), s.get("academicClass")),
                    cb.isTrue(wc.get("active")));

            Subquery<Long> registered = q.subquery(Long.class);
            Root<TopicRegistration> r = registered.from(TopicRegistration.class);
            registered.select(r.get("id")).where(
                    cb.equal(r.get("topic"), root),
                    cb.equal(r.get("student").get("id"), studentId));

            return cb.or(root.get("workspace").get("id").in(classWorkspaces), cb.exists(registered));
        };
    }

    public static Specification<Topic> hasId(Long id) {
        return (root, q, cb) -> cb.equal(root.get("id"), id);
    }
}
