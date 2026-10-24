package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * P4-11 — trang chi tiết đề tài. {@code members} chỉ có giá trị với thành viên đề tài (GV hướng dẫn, quản trị,
 * SV đã được duyệt); người khác nhận {@code null} để không lộ danh sách sinh viên.
 */
public record TopicDetailDTO(Long id, String title, String description, TopicStatus status, int capacity,
                             long approvedCount, long pendingCount, String myRegistrationStatus,
                             boolean member, boolean owner,
                             Lecturer lecturer, Workspace workspace, List<StudentSummaryDTO> members) {

    /** Email công việc của giảng viên hiện để sinh viên liên hệ (giống mockup "chi tiết đề tài"). */
    public record Lecturer(Long id, String fullName, String email, String degree, String speciality, String departmentName) {}

    public record Workspace(Long id, String name, String semester, WorkspaceStatus status,
                            LocalDateTime startAt, LocalDateTime endAt) {}
}
