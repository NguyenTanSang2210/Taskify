package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.StoredFileRepository;
import com.doanltmmt.Backend.service.FileStorageService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.service.TopicAccessService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tải file. Quyền dựa vào hồ sơ {@link StoredFile}: chủ file, ADMIN, hoặc người quản lý đề tài của báo cáo
 * mà file được gắn. Sinh viên KHÔNG tải được file của sinh viên khác.
 */
@RestController
@RequestMapping("/api/secure/files")
@SuppressWarnings("null")
public class SecureFileController {

    private static final String NOT_FOUND = "Không tìm thấy file.";

    private final StoredFileRepository fileRepo;
    private final ProgressReportRepository progressRepo;
    private final FileStorageService storage;
    private final TopicAccessService topicAccess;
    private final SecurityScopeService scope;

    public SecureFileController(StoredFileRepository fileRepo, ProgressReportRepository progressRepo,
                                FileStorageService storage, TopicAccessService topicAccess, SecurityScopeService scope) {
        this.fileRepo = fileRepo;
        this.progressRepo = progressRepo;
        this.storage = storage;
        this.topicAccess = topicAccess;
        this.scope = scope;
    }

    @GetMapping("/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        StoredFile file = fileRepo.findByStoredName(filename)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND));
        User me = scope.requireCurrentUser();
        if (!canDownload(file, me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tải file này.");
        }

        Path path = storage.resolve(file.getStoredName());
        if (path == null || !Files.isReadable(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND);
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(file.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND);
        }
    }

    private boolean canDownload(StoredFile file, User me) {
        if (scope.hasRole("ADMIN") || file.getOwnerId().equals(me.getId())) {
            return true;
        }
        if (StoredFile.REF_PROGRESS_REPORT.equals(file.getRefType()) && file.getRefId() != null) {
            ProgressReport report = progressRepo.findById(file.getRefId()).orElse(null);
            return report != null && topicAccess.isOwner(report.getTopic(), me);
        }
        return false;
    }
}
