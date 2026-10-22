package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.Milestone;

public record MilestoneRefDTO(Long id, String title) {
    public static MilestoneRefDTO from(Milestone m) {
        return m == null ? null : new MilestoneRefDTO(m.getId(), m.getTitle());
    }
}
