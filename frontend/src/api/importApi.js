import api from "../api";
import { downloadFrom } from "./download";

const form = ({ file, kind, departmentId, createMissingClasses }) => {
    const data = new FormData();
    data.append("file", file);
    data.append("kind", kind);
    if (departmentId) data.append("departmentId", departmentId);
    data.append("createMissingClasses", createMissingClasses ? "true" : "false");
    return data;
};

/** P4-03 — nhập sinh viên / giảng viên từ Excel. Báo cáo: { total, valid, invalid, newClasses, committed, created, rows[] }. */
export const importApi = {
    downloadTemplate: (kind) => downloadFrom("/users/import/template", {
        params: { kind },
        fallbackName: kind === "LECTURER" ? "mau-nhap-giang-vien.xlsx" : "mau-nhap-sinh-vien.xlsx",
    }),
    /** Kiểm tra file, không ghi gì. */
    preview: async (input) => (await api.post("/users/import/preview", form(input))).data,
    /** Nhập các dòng hợp lệ (bỏ qua dòng lỗi); tài khoản chưa kích hoạt, mã kích hoạt gửi qua email. */
    commit: async (input) => (await api.post("/users/import", form(input))).data,
};
