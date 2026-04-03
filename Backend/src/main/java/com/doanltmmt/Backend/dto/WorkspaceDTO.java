package com.doanltmmt.Backend.dto;

import com.doanltmmt.Backend.entity.Workspace;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkspaceDTO {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private String name;
    private String type;
    private String semester;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private boolean active;

    public WorkspaceDTO(Workspace ws) {
        if (ws == null) return;
        this.id = ws.getId();
        this.name = ws.getName();
        this.type = ws.getType();
        this.semester = ws.getSemester();
        this.startAt = ws.getStartAt();
        this.endAt = ws.getEndAt();
        this.status = ws.getStatus();
        this.active = ws.isActive();
        if (ws.getDepartment() != null) {
            this.departmentId = ws.getDepartment().getId();
            this.departmentName = ws.getDepartment().getName();
        }
    }
}
