package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.StoredFileRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tải file. Quyền dựa vào hồ sơ {@link StoredFile}: chủ file, ADMIN, hoặc người quản lý đề tài của báo cáo
 * mà file được gắn. Sinh viên KHÔNG tải được file của sinh viên khác.
 */
@Service
@SuppressWarnings("null")
public class FileDownloadService {

    private static final String NOT_FOUND = "Không tìm thấy file.";

    /** File cần trả về cùng tên gốc (để đặt Content-Disposition). */
    public record Download(Resource resource, String originalName) {
    }

    private final StoredFileRepository fileRepo;
    private final ProgressReportRepository progressRepo;
    private final FileStorageService storage;
    private final TopicAccessService topicAccess;
    private final SecurityScopeService scope;

    public FileDownloadService(StoredFileRepository fileRepo, ProgressReportRepository progressRepo,
                               FileStorageService storage, TopicAccessService topicAccess, SecurityScopeService scope) {
        this.fileRepo = fileRepo;
        this.progressRepo = progressRepo;
        this.storage = storage;
        this.topicAccess = topicAccess;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public Download open(String storedName) {
        StoredFile file = fileRepo.findByStoredName(storedName)
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
            return new Download(new UrlResource(path.toUri()), file.getOriginalName());
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
