package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.DepartmentRequest;
import com.doanltmmt.Backend.dto.request.OnCreate;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.service.DepartmentService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<Department> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Department create(@Validated({OnCreate.class, Default.class}) @RequestBody DepartmentRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Department update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest body) {
        return service.update(id, body);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Department updateStatus(@PathVariable Long id, @RequestParam boolean active) {
        return service.updateStatus(id, active);
    }
}
