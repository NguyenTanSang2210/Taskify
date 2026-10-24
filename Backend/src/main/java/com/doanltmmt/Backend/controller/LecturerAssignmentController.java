package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.LecturerAssignRequest;
import com.doanltmmt.Backend.dto.request.LecturerRevokeRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.service.LecturerAssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class LecturerAssignmentController {

    private final LecturerAssignmentService service;

    public LecturerAssignmentController(LecturerAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<LecturerAssignmentDTO> list(@RequestParam Long workspaceId) {
        return service.list(workspaceId);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public LecturerAssignmentDTO assign(@Valid @RequestBody LecturerAssignRequest body) {
        return service.assign(body);
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public LecturerAssignmentDTO revoke(@Valid @RequestBody LecturerRevokeRequest body) {
        return service.revoke(body);
    }
}
