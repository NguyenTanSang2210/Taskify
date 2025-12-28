package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/secure/files")
@CrossOrigin(origins = "http://localhost:5173")
public class SecureFileController {

    private final ProgressReportRepository progressRepo;
    private final UserRepository userRepo;
    private final SecurityScopeService scope;

    public SecureFileController(ProgressReportRepository progressRepo, UserRepository userRepo, SecurityScopeService scope) {
        this.progressRepo = progressRepo;
        this.userRepo = userRepo;
        this.scope = scope;
    }

    @GetMapping("/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepo.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            boolean isAdmin = scope.hasRole("ADMIN");
            boolean isDeptAdmin = scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN");

            ProgressReport report = progressRepo.findByFileUrlEndingWith(filename);
            if (report != null) {
                boolean isOwner = report.getStudent().getUser().getId().equals(currentUser.getId());
                boolean isLecturer = report.getTopic().getLecturer() != null &&
                        report.getTopic().getLecturer().getUser().getId().equals(currentUser.getId());

                boolean inScopeDepartment = false;
                if (isDeptAdmin) {
                    if (report.getTopic() == null || report.getTopic().getWorkspace() == null || report.getTopic().getWorkspace().getDepartment() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report has no topic/workspace/department");
                    }
                    scope.requireDepartmentAccess(report.getTopic().getWorkspace().getDepartment().getId());
                    inScopeDepartment = true;
                }

                if (!isAdmin && !isOwner && !isLecturer && !inScopeDepartment) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this file");
                }
            } else {
                if (!isAdmin) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to unverified file");
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
    }
}
