package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.config.SecurityProperties;
import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.security.RateLimiter;
import com.doanltmmt.Backend.service.FileStorageService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final FileStorageService storage;
    private final SecurityScopeService scope;
    private final RateLimiter rateLimiter;
    private final SecurityProperties props;

    public FileUploadController(FileStorageService storage, SecurityScopeService scope,
                                RateLimiter rateLimiter, SecurityProperties props) {
        this.storage = storage;
        this.scope = scope;
        this.rateLimiter = rateLimiter;
        this.props = props;
    }

    /**
     * Tải file lên. Chỉ nhận các đuôi trong danh sách cho phép, nội dung phải khớp đuôi, tối đa 20 MB.
     * File thuộc về người tải lên và phải được gắn vào một báo cáo trong vòng 24 giờ.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = scope.requireCurrentUser().getId();
        // Giới hạn theo NGƯỜI DÙNG (không theo IP) để cả phòng máy dùng chung NAT không bị chặn nhầm
        rateLimiter.check("upload", String.valueOf(userId), props.getRate().getUploadPer10Minutes(), Duration.ofMinutes(10));

        StoredFile saved = storage.store(file, userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fileId", saved.getId());
        response.put("fileName", saved.getStoredName());
        response.put("originalName", saved.getOriginalName());
        response.put("fileUrl", FileStorageService.urlFor(saved));
        return ResponseEntity.ok(response);
    }
}
