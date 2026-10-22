package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.ProjectPermission;

public record ProjectPermissionDTO(Long id, String roleName, String toolName, boolean canView, boolean canCreate,
                                   boolean canUpdate, boolean canDelete, TopicRefDTO topic) {
    public static ProjectPermissionDTO from(ProjectPermission p) {
        return p == null ? null : new ProjectPermissionDTO(p.getId(), p.getRoleName(), p.getToolName(),
                p.isCanView(), p.isCanCreate(), p.isCanUpdate(), p.isCanDelete(), TopicRefDTO.from(p.getTopic()));
    }
}
