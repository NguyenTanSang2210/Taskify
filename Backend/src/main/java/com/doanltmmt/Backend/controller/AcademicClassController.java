package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.AcademicClassRequest;
import com.doanltmmt.Backend.dto.request.OnCreate;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.service.AcademicClassService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class AcademicClassController {

    private final AcademicClassService service;

    public AcademicClassController(AcademicClassService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<AcademicClass> list(@RequestParam(required = false) Long departmentId) {
        return service.list(departmentId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public AcademicClass create(@Validated({OnCreate.class, Default.class}) @RequestBody AcademicClassRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public AcademicClass update(@PathVariable Long id, @Valid @RequestBody AcademicClassRequest body) {
        return service.update(id, body);
    }
}
