package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.StoredFileRepository;
import com.doanltmmt.Backend.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bảo trì hồ sơ file:
 * <ul>
 *   <li>{@link #backfill()} (chạy khi khởi động, idempotent): tạo {@link StoredFile} cho các báo cáo cũ đã có
 *       {@code fileUrl} từ trước khi có bảng này — chủ file = sinh viên nộp báo cáo.</li>
 *   <li>{@link #purgeOrphans()} (hằng đêm): xóa file đã tải lên nhưng không được gắn vào báo cáo nào sau 24 giờ.</li>
 * </ul>
 */
@Component
public class StoredFileMaintenance implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoredFileMaintenance.class);
    private static final String URL_PREFIX = "/api/secure/files/";

    private final StoredFileRepository fileRepo;
    private final ProgressReportRepository progressRepo;
    private final FileStorageService storage;

    public StoredFileMaintenance(StoredFileRepository fileRepo, ProgressReportRepository progressRepo,
                                 FileStorageService storage) {
        this.fileRepo = fileRepo;
        this.progressRepo = progressRepo;
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int n = backfill();
            if (n > 0) log.info("Đã tạo hồ sơ cho {} file cũ.", n);
        } catch (RuntimeException ex) {
            log.warn("Backfill hồ sơ file thất bại (bỏ qua, không chặn khởi động): {}", ex.getMessage());
        }
    }

    /** @return số hồ sơ mới tạo */
    public int backfill() {
        int created = 0;
        for (ProgressReport pr : progressRepo.findAll()) {
            String url = pr.getFileUrl();
            if (url == null || !url.startsWith(URL_PREFIX) || pr.getStudent() == null) continue;
            String storedName = FileStorageService.storedNameFromUrl(url);
            if (storedName == null || storedName.isBlank() || fileRepo.findByStoredName(storedName).isPresent()) continue;

            StoredFile sf = new StoredFile();
            sf.setStoredName(storedName);
            sf.setOriginalName(displayNameOf(storedName));
            sf.setContentType("application/octet-stream");
            Path p = storage.resolve(storedName);
            long size = 0;
            try {
                if (p != null && Files.exists(p)) size = Files.size(p);
            } catch (java.io.IOException ignored) { /* kích thước chỉ để tham khảo */ }
            sf.setSizeBytes(size);
            sf.setOwnerId(pr.getStudent().getId());
            sf.setRefType(StoredFile.REF_PROGRESS_REPORT);
            sf.setRefId(pr.getId());
            fileRepo.save(sf);
            created++;
        }
        return created;
    }

    /** Tên cũ có dạng {uuid}_{tên gốc}: bỏ tiền tố uuid để hiển thị. */
    static String displayNameOf(String storedName) {
        if (storedName.length() > 37 && storedName.charAt(36) == '_') {
            return storedName.substring(37);
        }
        return storedName;
    }

    @Scheduled(cron = "${app.upload.purge-cron:0 30 3 * * *}")
    public int purgeOrphans() {
        List<StoredFile> orphans = fileRepo.findByRefTypeIsNullAndCreatedAtBefore(LocalDateTime.now().minusHours(24));
        for (StoredFile f : orphans) {
            storage.deleteQuietly(f.getStoredName());
            fileRepo.delete(f);
        }
        if (!orphans.isEmpty()) log.info("Đã dọn {} file chưa được gắn quá 24 giờ.", orphans.size());
        return orphans.size();
    }
}
