package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.service.importer.ImportKind;
import com.doanltmmt.Backend.service.importer.UserImportService;
import com.doanltmmt.Backend.service.importer.UserImportService.ImportReport;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/** P4-03 — nhập sinh viên / giảng viên từ Excel: tải file mẫu → xem trước (báo lỗi từng dòng) → nhập. */
@RestController
@RequestMapping("/api/users/import")
@PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
public class UserImportController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final UserImportService service;

    public UserImportController(UserImportService service) {
        this.service = service;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam(defaultValue = "STUDENT") String kind) {
        ImportKind k = ImportKind.parse(kind);
        String fileName = k == ImportKind.STUDENT ? "mau-nhap-sinh-vien.xlsx" : "mau-nhap-giang-vien.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentType(XLSX)
                .body(service.template(k));
    }

    @PostMapping("/preview")
    public ImportReport preview(@RequestParam("file") MultipartFile file, @RequestParam String kind,
                                @RequestParam(required = false) Long departmentId,
                                @RequestParam(defaultValue = "false") boolean createMissingClasses) {
        return service.preview(file, ImportKind.parse(kind), departmentId, createMissingClasses);
    }

    @PostMapping
    public ImportReport commit(@RequestParam("file") MultipartFile file, @RequestParam String kind,
                               @RequestParam(required = false) Long departmentId,
                               @RequestParam(defaultValue = "false") boolean createMissingClasses) {
        return service.commit(file, ImportKind.parse(kind), departmentId, createMissingClasses);
    }
}
