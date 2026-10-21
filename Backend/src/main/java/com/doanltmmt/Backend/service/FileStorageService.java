package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.repository.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Nơi DUY NHẤT đọc/ghi thư mục upload ({@code app.upload.dir}). Tên file trên đĩa do hệ thống sinh
 * ({uuid}.{đuôi}); tên người dùng gửi chỉ lưu để hiển thị.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path root;
    private final long maxBytes;
    private final StoredFileRepository repo;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String dir,
                              @Value("${app.upload.max-bytes:20971520}") long maxBytes,
                              StoredFileRepository repo) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        this.repo = repo;
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Không tạo được thư mục upload: " + root, ex);
        }
    }

    public Path getRoot() {
        return root;
    }

    /** Kiểm tra, ghi file và tạo hồ sơ {@link StoredFile} (chưa gắn vào đối tượng nào). */
    public StoredFile store(MultipartFile file, Long ownerId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File rỗng.");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File quá lớn (tối đa " + (maxBytes / (1024 * 1024)) + " MB).");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được file tải lên.", ex);
        }
        if (content.length > maxBytes) { // kích thước thực, không tin header
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File quá lớn (tối đa " + (maxBytes / (1024 * 1024)) + " MB).");
        }

        String originalName = FileContentValidator.sanitizeOriginalName(file.getOriginalFilename());
        String ext;
        try {
            ext = FileContentValidator.validate(originalName, content);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        String storedName = UUID.randomUUID() + "." + ext;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ.");
        }
        try {
            Files.write(target, content);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được file. Vui lòng thử lại.", ex);
        }

        StoredFile sf = new StoredFile();
        sf.setStoredName(storedName);
        sf.setOriginalName(originalName);
        sf.setContentType(FileContentValidator.contentTypeFor(ext));
        sf.setSizeBytes(content.length);
        sf.setOwnerId(ownerId);
        try {
            return repo.save(sf);
        } catch (RuntimeException ex) {
            deleteQuietly(storedName);
            throw ex;
        }
    }

    /** Đường dẫn trên đĩa của file; null nếu tên trỏ ra ngoài thư mục upload. */
    public Path resolve(String storedName) {
        if (storedName == null || storedName.isBlank()) return null;
        Path p;
        try {
            p = root.resolve(storedName).normalize();
        } catch (InvalidPathException ex) { // tên cũ có ký tự hệ thống file không mã hóa được (locale không phải UTF-8)
            log.warn("Bỏ qua một tên file không biểu diễn được trên hệ thống file (kiểm tra locale UTF-8 của máy chủ).");
            return null;
        }
        return p.startsWith(root) && !p.equals(root) ? p : null;
    }

    /** Lấy tên file từ fileUrl dạng {@code /api/secure/files/{tên}} (hoặc chính tên đó). */
    public static String storedNameFromUrl(String fileUrl) {
        if (fileUrl == null) return null;
        String v = fileUrl.trim();
        int slash = v.lastIndexOf('/');
        return slash >= 0 ? v.substring(slash + 1) : v;
    }

    public static String urlFor(StoredFile f) {
        return "/api/secure/files/" + f.getStoredName();
    }

    public void deleteQuietly(String storedName) {
        Path p = resolve(storedName);
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (IOException ex) {
            log.warn("Không xóa được file {}: {}", storedName, ex.getMessage());
        }
    }
}
