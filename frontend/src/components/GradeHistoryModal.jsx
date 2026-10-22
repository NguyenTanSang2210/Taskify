import { useEffect, useState } from "react";
import { registrationApi } from "../api/registrationApi";

const fmtDateTime = (v) => (v ? new Date(v).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" }) : "—");
const fmtScore = (s) => (s === null || s === undefined ? "—" : String(s).replace(".", ","));

/** Lịch sử chấm/sửa điểm của một đăng ký (P2-13). */
export default function GradeHistoryModal({ registrationId, title, onClose }) {
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    registrationApi
      .gradeHistory(registrationId)
      .then((data) => !cancelled && setItems(data || []))
      .catch((e) => !cancelled && setError(e.message || "Không tải được lịch sử điểm."));
    return () => {
      cancelled = true;
    };
  }, [registrationId]);

  return (
    <div className="fixed inset-0 z-[110] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={onClose}></div>
      <div className="relative z-10 w-full max-w-3xl overflow-hidden rounded-3xl bg-white shadow-2xl dark:bg-slate-900">
        <div className="flex items-start justify-between border-b border-outline-variant/10 px-8 py-6">
          <div>
            <span className="mb-1 block text-[10px] font-black uppercase tracking-widest text-primary">Lịch sử điểm</span>
            <h2 className="text-lg font-black text-on-surface">{title}</h2>
          </div>
          <button onClick={onClose} className="rounded-xl p-2 hover:bg-surface-container-high" aria-label="Đóng">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <div className="max-h-[60vh] overflow-auto p-6">
          {error && <p className="text-sm font-bold text-error">{error}</p>}
          {!error && items === null && <p className="text-sm text-outline">Đang tải...</p>}
          {items?.length === 0 && <p className="text-sm text-outline">Chưa có lần chấm điểm nào.</p>}
          {items?.length > 0 && (
            <table className="w-full text-left text-sm">
              <thead className="text-[10px] font-black uppercase tracking-widest text-outline">
                <tr>
                  <th className="py-2 pr-4">Thời gian</th>
                  <th className="py-2 pr-4">Người chấm</th>
                  <th className="py-2 pr-4">Điểm</th>
                  <th className="py-2">Lý do</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/10">
                {items.map((h) => (
                  <tr key={h.id}>
                    <td className="py-3 pr-4 whitespace-nowrap">{fmtDateTime(h.changedAt)}</td>
                    <td className="py-3 pr-4">{h.changedByName || "—"}</td>
                    <td className="py-3 pr-4 whitespace-nowrap font-black">
                      {h.oldScore === null ? <span className="text-outline">Chấm lần đầu: </span> : `${fmtScore(h.oldScore)} → `}
                      <span className="text-primary">{fmtScore(h.newScore)}</span>
                    </td>
                    <td className="py-3">
                      {h.reason || <span className="text-outline">—</span>}
                      {h.revisionRequestId && (
                        <span className="ml-2 rounded-full bg-secondary/10 px-2 py-0.5 text-[10px] font-black text-secondary">
                          Theo yêu cầu #{h.revisionRequestId}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export { fmtDateTime, fmtScore };
