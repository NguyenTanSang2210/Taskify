package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.WorkspaceClassRequest;
import com.doanltmmt.Backend.entity.WorkspaceClass;
import com.doanltmmt.Backend.service.WorkspaceClassService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspace-classes")
public class WorkspaceClassController {

    private final WorkspaceClassService service;

    public WorkspaceClassController(WorkspaceClassService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<WorkspaceClass> list(@RequestParam Long workspaceId) {
        return service.list(workspaceId);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceClass assign(@Valid @RequestBody WorkspaceClassRequest body) {
        return service.assign(body);
    }

    @PostMapping("/unassign")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceClass unassign(@Valid @RequestBody WorkspaceClassRequest body) {
        return service.unassign(body);
    }
}
