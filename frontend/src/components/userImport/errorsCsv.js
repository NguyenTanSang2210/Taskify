const cell = (value) => {
    const s = value == null ? "" : String(value);
    return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

/**
 * CSV các dòng lỗi của báo cáo import (mở bằng Excel: có BOM UTF-8 để hiện đúng tiếng Việt).
 * @param {{ rows: Array<{ row, username, fullName, email, classCode, errors: string[] }> }} report
 * @param {"STUDENT"|"LECTURER"} kind
 */
export function errorsCsv(report, kind) {
    const header = ["Dòng", kind === "LECTURER" ? "Mã giảng viên" : "MSSV", "Họ tên", "Email"];
    if (kind !== "LECTURER") header.push("Mã lớp");
    header.push("Lỗi");
    const lines = [header.map(cell).join(",")];
    for (const r of report?.rows || []) {
        if (!r.errors?.length) continue;
        const values = [r.row, r.username, r.fullName, r.email];
        if (kind !== "LECTURER") values.push(r.classCode);
        values.push(r.errors.join("; "));
        lines.push(values.map(cell).join(","));
    }
    return "﻿" + lines.join("\r\n") + "\r\n";
}
