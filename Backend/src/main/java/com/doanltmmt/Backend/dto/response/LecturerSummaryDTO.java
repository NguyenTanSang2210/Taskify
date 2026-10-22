package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.Lecturer;

public record LecturerSummaryDTO(Long id, String degree, String speciality, UserSummaryDTO user) {
    public static LecturerSummaryDTO from(Lecturer l) {
        return l == null ? null
                : new LecturerSummaryDTO(l.getId(), l.getDegree(), l.getSpeciality(), UserSummaryDTO.from(l.getUser()));
    }
}
