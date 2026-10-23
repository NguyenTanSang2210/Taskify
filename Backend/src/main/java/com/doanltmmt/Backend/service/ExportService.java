package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.repository.GradeHistoryRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Xuất danh sách đăng ký + điểm ra Excel.
 * <ul>
 *   <li>ADMIN: xuất một đề tài hoặc TOÀN BỘ hệ thống (bỏ trống topicId).</li>
 *   <li>LECTURER, DEPARTMENT_ADMIN: BẮT BUỘC chỉ định topicId và phải là người quản lý đề tài đó
 *       (trước đây giảng viên nào cũng xuất được điểm của toàn hệ thống).</li>
 * </ul>
 */
@Service
public class ExportService {

    private final TopicRegistrationRepository regRepo;
    private final TopicAccessService topicAccess;
    private final SecurityScopeService scope;
    private final GradeHistoryRepository gradeHistoryRepo;

    public ExportService(TopicRegistrationRepository regRepo, TopicAccessService topicAccess, SecurityScopeService scope,
                         GradeHistoryRepository gradeHistoryRepo) {
        this.regRepo = regRepo;
        this.gradeHistoryRepo = gradeHistoryRepo;
        this.topicAccess = topicAccess;
        this.scope = scope;
    }

    /** File .xlsx danh sách đăng ký + điểm (một đề tài, hoặc toàn hệ thống khi ADMIN bỏ trống topicId). */
    @Transactional(readOnly = true)
    public byte[] registrationsWorkbook(Long topicId) throws IOException {
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
            String[] columns = {"ID", "Đề tài", "Sinh viên", "Mã sinh viên", "Giảng viên", "Trạng thái", "Điểm",
                    "Nhận xét", "Đã chấm lại (số lần)"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);

            Map<Long, Long> revisions = new HashMap<>();
            for (Object[] row : gradeHistoryRepo.countRevisionsPerRegistration()) {
                revisions.put((Long) row[0], (Long) row[1]);
            }
            int rowIdx = 1;
            for (TopicRegistration reg : registrations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(reg.getId());
                row.createCell(1).setCellValue(reg.getTopic().getTitle());
                row.createCell(2).setCellValue(reg.getStudent().getUser().getFullName());
                row.createCell(3).setCellValue(reg.getStudent().getStudentCode());
                row.createCell(4).setCellValue(reg.getTopic().getLecturer() != null ? reg.getTopic().getLecturer().getUser().getFullName() : "N/A");

                String status = "Chờ duyệt";
                if (Boolean.TRUE.equals(reg.getApproved())) status = "Đã duyệt";
                else if (Boolean.FALSE.equals(reg.getApproved())) status = "Từ chối";
                row.createCell(5).setCellValue(status);
                if (reg.getScore() != null) {
                    row.createCell(6).setCellValue(reg.getScore());
                } else {
                    row.createCell(6).setCellValue("");
                }

                row.createCell(7).setCellValue(reg.getFeedback() != null ? reg.getFeedback() : "");
                row.createCell(8).setCellValue(revisions.getOrDefault(reg.getId(), 0L));
            }


            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
