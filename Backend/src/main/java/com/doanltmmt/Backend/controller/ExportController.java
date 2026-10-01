package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.service.TopicAccessService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Xuất danh sách đăng ký + điểm ra Excel.
 * <ul>
 *   <li>ADMIN: xuất một đề tài hoặc TOÀN BỘ hệ thống (bỏ trống topicId).</li>
 *   <li>LECTURER, DEPARTMENT_ADMIN: BẮT BUỘC chỉ định topicId và phải là người quản lý đề tài đó
 *       (trước đây giảng viên nào cũng xuất được điểm của toàn hệ thống).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final TopicRegistrationRepository regRepo;
    private final TopicAccessService topicAccess;
    private final SecurityScopeService scope;

    public ExportController(TopicRegistrationRepository regRepo, TopicAccessService topicAccess, SecurityScopeService scope) {
        this.regRepo = regRepo;
        this.topicAccess = topicAccess;
        this.scope = scope;
    }

    @GetMapping("/excel")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN','LECTURER')")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam(required = false) Long topicId) throws IOException {
        List<TopicRegistration> registrations;
        if (topicId == null) {
            if (!scope.hasRole("ADMIN")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn đề tài cần xuất (topicId).");
            }
            registrations = regRepo.findAll();
        } else {
            Topic topic = topicAccess.loadTopicOr404(topicId);
            topicAccess.requireOwner(topic);
            registrations = regRepo.findByTopic_Id(topicId);
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Registrations");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Topic");
            header.createCell(2).setCellValue("Student");
            header.createCell(3).setCellValue("Student Code");
            header.createCell(4).setCellValue("Lecturer");
            header.createCell(5).setCellValue("Status");
            header.createCell(6).setCellValue("Score");
            header.createCell(7).setCellValue("Feedback");

            int rowIdx = 1;
            for (TopicRegistration reg : registrations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(reg.getId());
                row.createCell(1).setCellValue(reg.getTopic().getTitle());
                row.createCell(2).setCellValue(reg.getStudent().getUser().getFullName());
                row.createCell(3).setCellValue(reg.getStudent().getStudentCode());
                row.createCell(4).setCellValue(reg.getTopic().getLecturer() != null ? reg.getTopic().getLecturer().getUser().getFullName() : "N/A");

                String status = "Pending";
                if (Boolean.TRUE.equals(reg.getApproved())) status = "Approved";
                else if (Boolean.FALSE.equals(reg.getApproved())) status = "Rejected";
                row.createCell(5).setCellValue(status);
                if (reg.getScore() != null) {
                    row.createCell(6).setCellValue(reg.getScore());
                } else {
                    row.createCell(6).setCellValue("");
                }

                row.createCell(7).setCellValue(reg.getFeedback() != null ? reg.getFeedback() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=registrations.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}
