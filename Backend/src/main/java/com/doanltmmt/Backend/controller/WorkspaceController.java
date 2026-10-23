package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.WorkspaceDTO;
import com.doanltmmt.Backend.dto.request.OnCreate;
import com.doanltmmt.Backend.dto.request.WorkspaceRequest;
import com.doanltmmt.Backend.service.WorkspaceService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@SuppressWarnings("null")
public class WorkspaceController {

    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<WorkspaceDTO> list(@RequestParam(required = false) Long departmentId) {
        return service.list(departmentId);
    }

    /** Các đợt đang mở nhận đề tài — để giảng viên chọn khi tạo đề tài. */
    @GetMapping("/open-for-topics")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN','LECTURER')")
    public List<WorkspaceDTO> openForTopics() {
        return service.listOpenForTopics();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceDTO get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceDTO create(@Validated({OnCreate.class, Default.class}) @RequestBody WorkspaceRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceDTO update(@PathVariable Long id, @Valid @RequestBody WorkspaceRequest body) {
        return service.update(id, body);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceDTO transition(@PathVariable Long id, @RequestParam String to) {
        return service.transition(id, to);
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceDTO open(@PathVariable Long id) {
        return service.open(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceDTO close(@PathVariable Long id) {
        return service.close(id);
    }
}

