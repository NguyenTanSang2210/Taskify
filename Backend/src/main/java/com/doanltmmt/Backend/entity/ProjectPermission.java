package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "role_name", "tool_name"}))
@Getter
@Setter
public class ProjectPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "tool_name", nullable = false, length = 50)
    private String toolName; // e.g., "MILESTONE", "SUBMISSION", "RESOURCE", "GRADEBOOK", "DISCUSSION", "CALENDAR"

    @Column(name = "can_view")
    private boolean canView;

    @Column(name = "can_create")
    private boolean canCreate;

    @Column(name = "can_update")
    private boolean canUpdate;

    @Column(name = "can_delete")
    private boolean canDelete;
}
