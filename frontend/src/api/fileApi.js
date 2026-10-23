import api from "../api";
import { downloadFrom } from "./download";

export const fileApi = {
    /** @returns {{ fileName, originalName, fileUrl }} */
    upload: async (file) => {
        const formData = new FormData();
        formData.append("file", file);
        const res = await api.post("/upload", formData, {
            headers: {
                "Content-Type": "multipart/form-data",
            },
        });
        return res.data;
    },

    /**
     * Tải file đính kèm. File được bảo vệ bằng token nên KHÔNG dùng được thẻ <a href> (trình duyệt không gửi header
     * Authorization): gọi qua axios rồi lưu blob về máy với tên gốc của file.
     */
    download: async (fileUrl) => {
        const path = fileUrl.startsWith("/api/") ? fileUrl.slice(4) : fileUrl;
        await downloadFrom(path);
    },
};
