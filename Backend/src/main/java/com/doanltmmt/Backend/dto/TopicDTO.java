package com.doanltmmt.Backend.dto;

import com.doanltmmt.Backend.entity.Topic;
import lombok.Data;

@Data
public class TopicDTO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private Integer capacity;
    private Long lecturerId;
    private String lecturerName;
    private Long workspaceId;
    private String workspaceName;

    public TopicDTO(Topic t) {
        if (t == null) return;
        this.id = t.getId();
        this.title = t.getTitle();
        this.description = t.getDescription();
        this.status = t.getStatus();
        this.capacity = t.getCapacity();
        if (t.getLecturer() != null) {
            this.lecturerId = t.getLecturer().getId();
            if (t.getLecturer().getUser() != null) {
                this.lecturerName = t.getLecturer().getUser().getFullName();
            }
        }
        if (t.getWorkspace() != null) {
            this.workspaceId = t.getWorkspace().getId();
            this.workspaceName = t.getWorkspace().getName();
        }
    }
}
