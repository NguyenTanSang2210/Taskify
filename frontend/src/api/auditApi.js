import api from "../api";
import { downloadFrom } from "./download";

const clean = (params) => Object.fromEntries(Object.entries(params).filter(([, v]) => v !== "" && v != null));

/** P4-02 — nhật ký hệ thống (chỉ ADMIN). Bộ lọc: actor, action, category, entity, entityId, from, to, page, size. */
export const auditApi = {
    search: async (params) => (await api.get("/audit", { params: clean(params) })).data,
    options: async () => (await api.get("/audit/options")).data,
    exportCsv: (params) => downloadFrom("/audit/export", { params: clean(params), fallbackName: "nhat-ky-he-thong.csv" }),
};
