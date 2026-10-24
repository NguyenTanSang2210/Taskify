package com.doanltmmt.Backend.entity;

import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Set;

import static com.doanltmmt.Backend.entity.enums.WorkspaceStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** P3-04 — luật chuyển trạng thái đợt nằm ở một chỗ (trước đây lặp ở 5 service/controller). */
class WorkspaceStatusTest {

    private static Set<WorkspaceStatus> next(WorkspaceStatus from) {
        Set<WorkspaceStatus> result = EnumSet.noneOf(WorkspaceStatus.class);
        for (WorkspaceStatus to : values()) if (to != from && from.canTransitionTo(to)) result.add(to);
        return result;
    }

    @Test
    void transitions_followTheLifecycle() {
        assertEquals(EnumSet.of(OPEN_TOPIC), next(DRAFT));
        assertEquals(EnumSet.of(OPEN_REGISTRATION, CLOSED), next(OPEN_TOPIC));
        assertEquals(EnumSet.of(LOCK_REGISTRATION, CLOSED), next(OPEN_REGISTRATION));
        assertEquals(EnumSet.of(IN_PROGRESS, CLOSED), next(LOCK_REGISTRATION));
        assertEquals(EnumSet.of(CLOSED), next(IN_PROGRESS));
        assertEquals(EnumSet.noneOf(WorkspaceStatus.class), next(CLOSED));
    }

    @Test
    void permissions_perPhase() {
        assertEquals(EnumSet.of(OPEN_TOPIC), filter(WorkspaceStatus::allowsTopicEditing));
        assertEquals(EnumSet.of(OPEN_TOPIC, OPEN_REGISTRATION), filter(WorkspaceStatus::allowsTopicOpenClose));
        assertEquals(EnumSet.of(DRAFT, OPEN_TOPIC), filter(WorkspaceStatus::allowsConfiguration));
        assertEquals(EnumSet.of(OPEN_REGISTRATION, LOCK_REGISTRATION), filter(WorkspaceStatus::allowsRegistrationReview));
    }

    private static Set<WorkspaceStatus> filter(java.util.function.Predicate<WorkspaceStatus> p) {
        Set<WorkspaceStatus> result = EnumSet.noneOf(WorkspaceStatus.class);
        for (WorkspaceStatus s : values()) if (p.test(s)) result.add(s);
        return result;
    }

    @Test
    void parse_acceptsLowerCaseAndLegacyName_rejectsUnknown() {
        assertEquals(IN_PROGRESS, WorkspaceStatus.parse(" in_progress "));
        assertEquals(OPEN_TOPIC, WorkspaceStatus.parse("OPEN"));
        assertThrows(ResponseStatusException.class, () -> WorkspaceStatus.parse("ACTIVE"));
        assertThrows(ResponseStatusException.class, () -> WorkspaceStatus.parse(""));
    }
}
