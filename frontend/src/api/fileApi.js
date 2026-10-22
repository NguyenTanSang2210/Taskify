import api from "../api";

/** Tên file từ header Content-Disposition (ưu tiên filename* dạng UTF-8 cho tên tiếng Việt). */
const fileNameFrom = (disposition, fallback) => {
    if (!disposition) return fallback;
    const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
    if (utf8) {
        try { return decodeURIComponent(utf8[1].replace(/"/g, "")); } catch { /* dùng cách dưới */ }
    }
    const plain = /filename="?([^";]+)"?/i.exec(disposition);
    return plain ? plain[1] : fallback;
};

/** Lỗi trả về khi tải dạng blob: đọc JSON trong blob để lấy thông báo của backend. */
const messageOf = async (err) => {
    const data = err?.data;
    if (data instanceof Blob) {
        try {
            const parsed = JSON.parse(await data.text());
            if (parsed?.message) return parsed.message;
        } catch { /* không phải JSON */ }
    }
    return err?.message || "Không tải được tệp.";
};

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
        try {
            const res = await api.get(path, { responseType: "blob" });
            const name = fileNameFrom(res.headers?.["content-disposition"], path.split("/").pop());
            const href = URL.createObjectURL(res.data);
            const a = document.createElement("a");
            a.href = href;
            a.download = name;
            document.body.appendChild(a);
            a.click();
            a.remove();
            setTimeout(() => URL.revokeObjectURL(href), 1000);
        } catch (err) {
            const wrapped = new Error(await messageOf(err), { cause: err });
            wrapped.status = err?.status;
            throw wrapped;
        }
    },
};
