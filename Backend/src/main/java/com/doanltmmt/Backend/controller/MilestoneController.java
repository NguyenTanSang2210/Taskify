package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.MilestoneGradeRequest;
import com.doanltmmt.Backend.dto.request.MilestoneRequest;
import com.doanltmmt.Backend.dto.request.OnCreate;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.service.MilestoneGradeService;
import com.doanltmmt.Backend.service.MilestoneService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Milestone của đề tài. Xem: thành viên đề tài (sinh viên đã được duyệt, giảng viên hướng dẫn, quản trị khoa/hệ thống).
 * Tạo/sửa/xóa: chỉ người quản lý đề tài đó — KHÔNG phải mọi giảng viên.
 */
@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService service;
    private final MilestoneGradeService grades;

    public MilestoneController(MilestoneService service, MilestoneGradeService grades) {
        this.service = service;
        this.grades = grades;
    }

    /** P4-05 — bảng nộp bài + điểm theo mốc (giảng viên: mọi sinh viên của đề tài; sinh viên: chỉ mình). */
    @GetMapping("/board")
    @PreAuthorize("isAuthenticated()")
    public MilestoneBoardDTO board(@RequestParam Long topicId) {
        return grades.board(topicId);
    }

    @PutMapping("/{id}/grades/{studentId}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public MilestoneBoardDTO.Cell grade(@PathVariable Long id, @PathVariable Long studentId, @Valid @RequestBody MilestoneGradeRequest body) {
        return grades.grade(id, studentId, body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MilestoneDTO getMilestone(@PathVariable Long id) {
        return service.getMilestone(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MilestoneDTO> getMilestones(@RequestParam Long topicId) {
        return service.getMilestones(topicId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public MilestoneDTO createMilestone(@Validated({OnCreate.class, Default.class}) @RequestBody MilestoneRequest body) {
        return service.createMilestone(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public MilestoneDTO updateMilestone(@PathVariable Long id, @Valid @RequestBody MilestoneRequest body) {
        return service.updateMilestone(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public void deleteMilestone(@PathVariable Long id) {
        service.deleteMilestone(id);
    }
}
