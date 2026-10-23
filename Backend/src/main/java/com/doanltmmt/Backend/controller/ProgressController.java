package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.ProgressCommentRequest;
import com.doanltmmt.Backend.dto.request.ProgressCreateRequest;
import com.doanltmmt.Backend.dto.request.ProgressStatusRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.service.ProgressService;
import com.doanltmmt.Backend.util.Pages;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService service;

    public ProgressController(ProgressService service) {
        this.service = service;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('STUDENT')")
    public ProgressReportDTO create(@RequestParam Long studentId,
                                 @RequestParam Long topicId,
                                 @Valid @RequestBody ProgressCreateRequest body) {
        return service.create(studentId, topicId, body);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<ProgressReportDTO> listByStudent(@PathVariable Long studentId,
                                              @RequestParam(required = false) Long topicId) {
        return service.listByStudent(studentId, topicId);
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public PageResponse<ProgressReportDTO> listByTopic(@PathVariable Long topicId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = Pages.DEFAULT_SIZE) int size) {
        return service.listByTopic(topicId, page, size);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ProgressReportDTO updateStatus(@PathVariable Long id,
                                       @Valid @RequestBody ProgressStatusRequest body) {
        return service.updateStatus(id, body);
    }

    @PutMapping("/{id}/comment")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ProgressReportDTO addComment(@PathVariable Long id,
                                     @Valid @RequestBody ProgressCommentRequest body) {
        return service.addComment(id, body);
    }
}
