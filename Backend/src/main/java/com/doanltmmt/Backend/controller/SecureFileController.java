package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.service.FileDownloadService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/** Tải file đã lưu; kiểm tra quyền ở {@link FileDownloadService}. */
@RestController
@RequestMapping("/api/secure/files")
public class SecureFileController {

    private final FileDownloadService service;

    public SecureFileController(FileDownloadService service) {
        this.service = service;
    }

    @GetMapping("/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        FileDownloadService.Download file = service.open(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.originalName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }
}
