package com.doanltmmt.Backend.service.importer;

import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.service.importer.ImportKind.ImportColumn;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Đọc file .xlsx danh sách tài khoản và tạo file mẫu (P4-03). Không đụng tới CSDL. */
public final class ImportWorkbook {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int MAX_ROWS = 2000;

    /** Tên cột khác được chấp nhận (đã chuẩn hóa: bỏ dấu, chữ thường, chỉ chữ + số). */
    private static final Map<String, String> ALIASES = Map.of(
            "masinhvien", ImportColumn.USERNAME,
            "tendangnhap", ImportColumn.USERNAME,
            "username", ImportColumn.USERNAME,
            "hovaten", ImportColumn.FULL_NAME,
            "lop", ImportColumn.CLASS_CODE,
            "sdt", ImportColumn.PHONE,
            "dienthoai", ImportColumn.PHONE);

    private ImportWorkbook() {
    }

    /** Một dòng dữ liệu: số dòng trên Excel (bắt đầu từ 1) + giá trị theo khóa cột (đã cắt khoảng trắng). */
    public record RawRow(int rowNumber, Map<String, String> values) {
        public String get(String key) {
            String v = values.get(key);
            return v == null || v.isBlank() ? null : v;
        }
    }

    public static List<RawRow> read(MultipartFile file, ImportKind kind) {
        if (file == null || file.isEmpty()) throw invalid("Vui lòng chọn file Excel (.xlsx) cần nhập.");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) throw invalid("Chỉ nhận file Excel .xlsx (tải file mẫu để xem định dạng).");
        if (file.getSize() > MAX_BYTES) throw invalid("File quá lớn (tối đa 5 MB).");
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            return readSheet(wb.getSheetAt(0), kind);
        } catch (ApiException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw invalid("Không đọc được file — hãy lưu lại dưới dạng Excel Workbook (.xlsx).");
        }
    }

    private static List<RawRow> readSheet(Sheet sheet, ImportKind kind) {
        DataFormatter fmt = new DataFormatter(Locale.ROOT);
        Row header = null;
        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum() && header == null; r++) {
            Row row = sheet.getRow(r);
            if (row != null && !isBlank(row, fmt)) header = row;
        }
        if (header == null) throw invalid("File không có dữ liệu.");

        Map<String, String> keyByHeader = new HashMap<>();
        for (ImportColumn c : kind.columns()) keyByHeader.put(normalize(c.header()), c.key());
        Map<Integer, String> keyByIndex = new LinkedHashMap<>();
        for (Cell cell : header) {
            String norm = normalize(fmt.formatCellValue(cell));
            String key = keyByHeader.getOrDefault(norm, ALIASES.get(norm));
            if (key != null && kind.columns().stream().anyMatch(c -> c.key().equals(key))) {
                keyByIndex.putIfAbsent(cell.getColumnIndex(), key);
            }
        }
        List<String> missing = kind.columns().stream()
                .filter(c -> c.required() && !keyByIndex.containsValue(c.key()))
                .map(ImportColumn::header).toList();
        if (!missing.isEmpty()) {
            throw invalid("File thiếu cột bắt buộc: " + String.join(", ", missing) + ". Hãy dùng file mẫu cho "
                    + kind.label().toLowerCase(Locale.ROOT) + ".");
        }

        List<RawRow> rows = new ArrayList<>();
        for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isBlank(row, fmt)) continue;
            if (rows.size() == MAX_ROWS) throw invalid("File có quá " + MAX_ROWS + " dòng — hãy chia thành nhiều file.");
            Map<String, String> values = new HashMap<>();
            keyByIndex.forEach((idx, key) -> values.put(key, fmt.formatCellValue(row.getCell(idx)).trim()));
            rows.add(new RawRow(r + 1, values));
        }
        if (rows.isEmpty()) throw invalid("File không có dòng dữ liệu nào dưới dòng tiêu đề.");
        return rows;
    }

    public static byte[] template(ImportKind kind) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);

            Sheet sheet = wb.createSheet("Danh sách");
            Row header = sheet.createRow(0);
            Row example = sheet.createRow(1);
            List<ImportColumn> columns = kind.columns();
            for (int i = 0; i < columns.size(); i++) {
                Cell h = header.createCell(i);
                h.setCellValue(columns.get(i).header());
                h.setCellStyle(bold);
                example.createCell(i).setCellValue(columns.get(i).example());
                sheet.setColumnWidth(i, 24 * 256);
            }

            Sheet guide = wb.createSheet("Hướng dẫn");
            List<String> lines = new ArrayList<>(List.of(
                    "Nhập danh sách " + kind.label().toLowerCase(Locale.ROOT) + " vào Taskify",
                    "• Mỗi dòng ở sheet \"Danh sách\" là một tài khoản; xóa dòng ví dụ trước khi nhập.",
                    "• Cột bắt buộc: " + String.join(", ", columns.stream().filter(ImportColumn::required).map(ImportColumn::header).toList()) + ".",
                    "• " + columns.get(0).header() + " là tên đăng nhập (tối đa 50 ký tự, không trùng tài khoản đã có).",
                    "• Email dùng để gửi mã kích hoạt: người dùng tự đặt mật khẩu khi kích hoạt.",
                    "• Tối đa " + MAX_ROWS + " dòng mỗi file. Hệ thống kiểm tra và báo lỗi từng dòng trước khi nhập."));
            if (kind == ImportKind.STUDENT) {
                lines.add("• Mã lớp phải là lớp của khoa (hoặc chọn \"Tự tạo lớp chưa có\" khi nhập).");
            }
            for (int i = 0; i < lines.size(); i++) {
                Cell c = guide.createRow(i).createCell(0);
                c.setCellValue(lines.get(i));
                if (i == 0) c.setCellStyle(bold);
            }
            guide.setColumnWidth(0, 100 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được file mẫu", e);
        }
    }

    /** Bỏ dấu tiếng Việt, chữ thường, chỉ giữ chữ + số: "Số điện thoại" → "sodienthoai". */
    static String normalize(String text) {
        if (text == null) return "";
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static boolean isBlank(Row row, DataFormatter fmt) {
        for (Cell cell : row) {
            if (!fmt.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest(ErrorCodes.IMPORT_FILE_INVALID, message);
    }
}
