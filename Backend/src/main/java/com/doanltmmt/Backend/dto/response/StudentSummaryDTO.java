package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.Student;

public record StudentSummaryDTO(Long id, String studentCode, String className, UserSummaryDTO user) {
    public static StudentSummaryDTO from(Student s) {
        return s == null ? null
                : new StudentSummaryDTO(s.getId(), s.getStudentCode(), s.getClassName(), UserSummaryDTO.from(s.getUser()));
    }
}
