package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.LecturerAssignment;
import java.time.LocalDateTime;

public record LecturerAssignmentDTO(Long id, String type, int quotaMaxGroups, boolean active, String reason,
                                    LocalDateTime createdAt, LecturerSummaryDTO lecturer, WorkspaceRef workspace) {
    public record WorkspaceRef(Long id, String name) {}

    public static LecturerAssignmentDTO from(LecturerAssignment a) {
        if (a == null) return null;
        WorkspaceRef ws = a.getWorkspace() == null ? null
                : new WorkspaceRef(a.getWorkspace().getId(), a.getWorkspace().getName());
        return new LecturerAssignmentDTO(a.getId(), a.getType(), a.getQuotaMaxGroups(), a.isActive(), a.getReason(),
                a.getCreatedAt(), LecturerSummaryDTO.from(a.getLecturer()), ws);
    }
}
