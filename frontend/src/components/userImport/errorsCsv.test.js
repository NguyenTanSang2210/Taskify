import { describe, expect, it } from "vitest";
import { errorsCsv } from "./errorsCsv";

const report = {
    rows: [
        { row: 2, username: "sv1", fullName: "Nguyễn Văn A", email: "a@x.vn", classCode: "K15", errors: [] },
        { row: 3, username: "sv2", fullName: 'Trần "Bé"', email: "sai", classCode: "K15", errors: ["Email không hợp lệ", "MSSV bị trùng trong file"] },
    ],
};

describe("errorsCsv", () => {
    it("chỉ xuất dòng lỗi, có BOM để Excel đọc đúng tiếng Việt, escape dấu ngoặc kép", () => {
        const csv = errorsCsv(report, "STUDENT");
        expect(csv.startsWith("﻿")).toBe(true);
        const lines = csv.slice(1).trim().split("\r\n");
        expect(lines).toEqual([
            "Dòng,MSSV,Họ tên,Email,Mã lớp,Lỗi",
            '3,sv2,"Trần ""Bé""",sai,K15,"Email không hợp lệ; MSSV bị trùng trong file"',
        ]);
    });

    it("file giảng viên không có cột mã lớp", () => {
        const csv = errorsCsv({ rows: [{ row: 5, username: "gv", fullName: "B", email: "b", errors: ["Email không hợp lệ"] }] }, "LECTURER");
        expect(csv.slice(1).split("\r\n")[0]).toBe("Dòng,Mã giảng viên,Họ tên,Email,Lỗi");
        expect(csv).toContain("5,gv,B,b,Email không hợp lệ");
    });
});
