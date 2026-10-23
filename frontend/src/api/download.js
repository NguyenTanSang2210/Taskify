import api from "../api";

/** Tên file từ header Content-Disposition (ưu tiên filename* dạng UTF-8 cho tên tiếng Việt). */
export const fileNameFrom = (disposition, fallback) => {
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

/** Lưu một Blob về máy với tên cho trước. */
export function saveBlob(blob, name) {
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = href;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(href), 1000);
}

/**
 * GET một file được bảo vệ bằng token rồi lưu về máy. Không dùng được thẻ <a href> vì trình duyệt không gửi header
 * Authorization; lỗi của backend (JSON nằm trong blob) được đọc ra thành thông báo tiếng Việt.
 */
export async function downloadFrom(path, { params, fallbackName } = {}) {
    try {
        const res = await api.get(path, { params, responseType: "blob" });
        saveBlob(res.data, fileNameFrom(res.headers?.["content-disposition"], fallbackName || path.split("/").pop()));
    } catch (err) {
        const wrapped = new Error(await messageOf(err), { cause: err });
        wrapped.status = err?.status;
        throw wrapped;
    }
}
