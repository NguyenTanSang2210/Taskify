import { useState } from "react";
import { fileApi } from "../api/fileApi";
import { showToast } from "../toast";

/** Nút tải file đính kèm (gửi kèm token — thay cho thẻ <a href> trước đây bị 401). */
export default function FileDownloadButton({ fileUrl, className = "", title = "Tải tệp đính kèm", children }) {
  const [busy, setBusy] = useState(false);

  const onClick = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (busy) return;
    setBusy(true);
    try {
      await fileApi.download(fileUrl);
    } catch (err) {
      // 403 đã được api.js báo chung
      if (err.status !== 403) showToast("danger", err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <button type="button" onClick={onClick} disabled={busy} className={`${className} disabled:opacity-50`} title={title}>
      {children}
    </button>
  );
}
