package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.response.DashboardDTO;
import com.doanltmmt.Backend.dto.response.DashboardDTO.Bin;
import com.doanltmmt.Backend.dto.response.DashboardDTO.Feedback;
import com.doanltmmt.Backend.dto.response.DashboardDTO.LecturerLoad;
import com.doanltmmt.Backend.dto.response.DashboardDTO.Option;
import com.doanltmmt.Backend.dto.response.DashboardDTO.Overview;
import com.doanltmmt.Backend.dto.response.DashboardDTO.Registrations;
import com.doanltmmt.Backend.dto.response.DashboardDTO.Scores;
import com.doanltmmt.Backend.dto.response.DashboardDTO.Students;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.repository.DashboardQueryRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P4-01 — thống kê tổng hợp cho ADMIN (toàn hệ thống hoặc một khoa) và quản trị khoa (khoa của mình), lọc theo đợt.
 * Mọi số liệu là truy vấn GROUP BY/COUNT trong {@link DashboardQueryRepository}; không cache (đủ nhanh với index V5).
 */
@Service
public class DashboardOverviewService {

    private static final int LECTURER_LIMIT = 15;

    private final DashboardQueryRepository queries;
    private final DepartmentRepository departmentRepo;
    private final WorkspaceRepository workspaceRepo;
    private final StudentRepository studentRepo;
    private final SecurityScopeService scope;

    public DashboardOverviewService(DashboardQueryRepository queries, DepartmentRepository departmentRepo,
                                    WorkspaceRepository workspaceRepo, StudentRepository studentRepo, SecurityScopeService scope) {
        this.queries = queries;
        this.departmentRepo = departmentRepo;
        this.workspaceRepo = workspaceRepo;
        this.studentRepo = studentRepo;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public Overview overview(Long departmentId, Long workspaceId) {
        boolean admin = scope.hasRole("ADMIN");
        Department dept = resolveDepartment(admin, departmentId);
        Long deptId = dept == null ? null : dept.getId();
        Workspace ws = resolveWorkspace(workspaceId, deptId);
        Long wsId = ws == null ? null : ws.getId();

        Map<String, Long> topics = new LinkedHashMap<>();
        for (TopicStatus s : TopicStatus.values()) topics.put(s.name(), 0L);
        for (Object[] row : queries.countTopicsByStatus(deptId, wsId)) topics.put(((TopicStatus) row[0]).name(), (Long) row[1]);

        long pending = 0, approved = 0, rejected = 0;
        for (Object[] row : queries.countRegistrationsByApproval(deptId, wsId)) {
            Boolean a = (Boolean) row[0];
            long n = (Long) row[1];
            if (a == null) pending = n;
            else if (a) approved = n;
            else rejected = n;
        }

        long[] bins = new long[10]; // [0,1) … [8,9), [9,10]
        long graded = 0;
        for (Object[] row : queries.scoreHistogram(deptId, wsId)) {
            int bucket = Math.min(9, Math.max(0, ((Number) row[0]).intValue()));
            long n = (Long) row[1];
            bins[bucket] += n;
            graded += n;
        }
        List<Bin> histogram = new ArrayList<>();
        for (int i = 0; i < bins.length; i++) histogram.add(new Bin(i + "–" + (i + 1), bins[i]));
        Double avg = queries.averageScore(deptId, wsId);

        long totalStudents = wsId != null ? queries.countStudentsInWorkspace(wsId)
                : deptId != null ? studentRepo.countByUser_Department_Id(deptId) : studentRepo.count();
        long withTopic = queries.countStudentsWithApprovedTopic(deptId, wsId);

        List<LecturerLoad> load = queries.lecturerLoad(deptId, wsId, PageRequest.of(0, LECTURER_LIMIT)).stream()
                .map(r -> new LecturerLoad((String) r[0], (Long) r[1], (Long) r[2])).toList();

        List<Object[]> times = queries.feedbackTimes(deptId, wsId);
        Double avgHours = times.isEmpty() ? null : round1(times.stream()
                .mapToLong(r -> Math.max(0, Duration.between((LocalDateTime) r[0], (LocalDateTime) r[1]).toMinutes()))
                .average().orElse(0) / 60.0);

        List<Option> departments = admin
                ? departmentRepo.findAll(Sort.by("name")).stream().map(d -> new Option(d.getId(), d.getName())).toList()
                : List.of();
        List<Workspace> wsList = deptId != null ? workspaceRepo.findByDepartment_IdOrderByIdDesc(deptId)
                : workspaceRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));

        return new Overview(deptId, dept == null ? null : dept.getName(), wsId, departments,
                wsList.stream().map(w -> new Option(w.getId(), w.getName())).toList(), topics,
                new Students(totalStudents, withTopic, percent(withTopic, totalStudents)),
                new Registrations(pending, approved, rejected, graded, percent(graded, approved)),
                new Scores(avg == null ? null : round1(avg), histogram), load,
                new Feedback(avgHours, times.size(), queries.countUncommentedReports(deptId, wsId)));
    }

    /** File Excel của {@link #overview}: sheet Tổng quan, Phân bố điểm, Giảng viên. */
    @Transactional(readOnly = true)
    public byte[] exportExcel(Long departmentId, Long workspaceId) {
        Overview o = overview(departmentId, workspaceId);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);

            Sheet summary = wb.createSheet("Tổng quan");
            String scopeName = (o.departmentName() == null ? "Toàn hệ thống" : "Khoa " + o.departmentName())
                    + (o.workspaceId() == null ? "" : " · đợt " + o.workspaces().stream()
                    .filter(w -> w.id().equals(o.workspaceId())).map(Option::name).findFirst().orElse(""));
            List<Object[]> rows = new ArrayList<>(List.of(
                    new Object[]{"Phạm vi", scopeName},
                    new Object[]{"Thời điểm xuất", LocalDateTime.now().withNano(0).toString().replace('T', ' ')},
                    new Object[]{"Tổng số sinh viên", o.students().total()},
                    new Object[]{"Sinh viên đã có đề tài được duyệt", o.students().withTopic()},
                    new Object[]{"Tỉ lệ có đề tài (%)", o.students().rate()},
                    new Object[]{"Đăng ký chờ duyệt", o.registrations().pending()},
                    new Object[]{"Đăng ký đã duyệt", o.registrations().approved()},
                    new Object[]{"Đăng ký bị từ chối", o.registrations().rejected()},
                    new Object[]{"Sinh viên đã có điểm", o.registrations().graded()},
                    new Object[]{"Tỉ lệ hoàn thành (%)", o.registrations().completionRate()},
                    new Object[]{"Điểm trung bình", o.scores().average()},
                    new Object[]{"Thời gian phản hồi trung bình của GV (giờ)", o.feedback().avgHours()},
                    new Object[]{"Báo cáo chưa nhận xét", o.feedback().uncommented()}));
            o.topicsByStatus().forEach((status, n) -> rows.add(new Object[]{"Đề tài " + TopicStatus.valueOf(status).label(), n}));
            write(summary, bold, new String[]{"Chỉ số", "Giá trị"}, rows);

            write(wb.createSheet("Phân bố điểm"), bold, new String[]{"Khoảng điểm", "Số sinh viên"},
                    o.scores().histogram().stream().map(b -> new Object[]{b.label(), b.count()}).toList());
            write(wb.createSheet("Giảng viên"), bold, new String[]{"Giảng viên", "Số đề tài", "Sinh viên được duyệt"},
                    o.lecturerLoad().stream().map(l -> new Object[]{l.name(), l.topics(), l.approvedStudents()}).toList());
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được file thống kê", e);
        }
    }

    private static void write(Sheet sheet, CellStyle bold, String[] header, List<Object[]> rows) {
        Row h = sheet.createRow(0);
        for (int i = 0; i < header.length; i++) {
            h.createCell(i).setCellValue(header[i]);
            h.getCell(i).setCellStyle(bold);
            sheet.setColumnWidth(i, (i == 0 ? 44 : 18) * 256);
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            Object[] values = rows.get(r);
            for (int c = 0; c < values.length; c++) {
                Object v = values[c];
                if (v instanceof Number n) row.createCell(c).setCellValue(n.doubleValue());
                else row.createCell(c).setCellValue(v == null ? "" : v.toString());
            }
        }
    }

    private Department resolveDepartment(boolean admin, Long departmentId) {
        if (admin) {
            return departmentId == null ? null
                    : departmentRepo.findById(departmentId).orElseThrow(() -> ApiException.notFound("Không tìm thấy khoa."));
        }
        Department own = scope.requireCurrentUser().getDepartment();
        if (own == null) throw ApiException.forbidden("Tài khoản của bạn chưa thuộc khoa nào.");
        if (departmentId != null && !departmentId.equals(own.getId())) {
            throw ApiException.forbidden("Bạn chỉ xem được thống kê của khoa mình.");
        }
        return own;
    }

    private Workspace resolveWorkspace(Long workspaceId, Long deptId) {
        if (workspaceId == null) return null;
        Workspace ws = workspaceRepo.findById(workspaceId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt."));
        if (deptId != null && (ws.getDepartment() == null || !deptId.equals(ws.getDepartment().getId()))) {
            throw ApiException.forbidden("Đợt không thuộc khoa đang xem.");
        }
        return ws;
    }

    static Double percent(long part, long whole) {
        return whole == 0 ? null : round1(part * 100.0 / whole);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
