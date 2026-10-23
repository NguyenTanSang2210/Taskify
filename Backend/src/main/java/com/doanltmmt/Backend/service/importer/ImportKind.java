package com.doanltmmt.Backend.service.importer;

import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;

import java.util.List;
import java.util.Locale;

/** Loại tài khoản nhập từ Excel và các cột của file mẫu (cột đầu tiên là tên đăng nhập). */
public enum ImportKind {
    STUDENT("STUDENT", "Sinh viên", List.of(
            new ImportColumn(ImportColumn.USERNAME, "MSSV", true, "2021601234"),
            new ImportColumn(ImportColumn.FULL_NAME, "Họ tên", true, "Nguyễn Văn An"),
            new ImportColumn(ImportColumn.EMAIL, "Email", true, "an.nv@sv.truong.edu.vn"),
            new ImportColumn(ImportColumn.CLASS_CODE, "Mã lớp", true, "CNTT-K15"),
            new ImportColumn(ImportColumn.PHONE, "Số điện thoại", false, "0901234567"))),
    LECTURER("LECTURER", "Giảng viên", List.of(
            new ImportColumn(ImportColumn.USERNAME, "Mã giảng viên", true, "gv.binh"),
            new ImportColumn(ImportColumn.FULL_NAME, "Họ tên", true, "Trần Thị Bình"),
            new ImportColumn(ImportColumn.EMAIL, "Email", true, "binh.tt@truong.edu.vn"),
            new ImportColumn(ImportColumn.DEGREE, "Học vị", false, "Tiến sĩ"),
            new ImportColumn(ImportColumn.SPECIALITY, "Chuyên ngành", false, "Trí tuệ nhân tạo"),
            new ImportColumn(ImportColumn.PHONE, "Số điện thoại", false, "0912345678")));

    private final String roleName;
    private final String label;
    private final List<ImportColumn> columns;

    ImportKind(String roleName, String label, List<ImportColumn> columns) {
        this.roleName = roleName;
        this.label = label;
        this.columns = columns;
    }

    public String roleName() {
        return roleName;
    }

    public String label() {
        return label;
    }

    public List<ImportColumn> columns() {
        return columns;
    }

    public static ImportKind parse(String raw) {
        try {
            return valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(ErrorCodes.INVALID_INPUT, "Loại tài khoản không hợp lệ — chỉ nhận STUDENT hoặc LECTURER");
        }
    }

    /** Một cột của file: khóa nội bộ, tiêu đề tiếng Việt, bắt buộc hay không, giá trị ví dụ ở file mẫu. */
    public record ImportColumn(String key, String header, boolean required, String example) {
        public static final String USERNAME = "username";
        public static final String FULL_NAME = "fullName";
        public static final String EMAIL = "email";
        public static final String PHONE = "phone";
        public static final String CLASS_CODE = "classCode";
        public static final String DEGREE = "degree";
        public static final String SPECIALITY = "speciality";
    }
}
