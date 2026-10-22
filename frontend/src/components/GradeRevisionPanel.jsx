import { useEffect, useState } from "react";
import { registrationApi } from "../api/registrationApi";
import GradeHistoryModal, { fmtDateTime, fmtScore } from "./GradeHistoryModal";
import InlineNotice from "./InlineNotice";

const STATUS = {
  PENDING: ["Chờ duyệt", "bg-amber-500/10 text-amber-700"],
  APPROVED: ["Đã mở khóa", "bg-green-500/10 text-green-700"],
  REJECTED: ["Từ chối", "bg-error/10 text-error"],
  USED: ["Đã sửa điểm", "bg-primary/10 text-primary"],
  EXPIRED: ["Hết hạn", "bg-surface-container text-outline"],
};

const DEFAULT_HOURS = 72;
const MAX_HOURS = 168;

/** Tab "Yêu cầu chấm lại" cho quản trị khoa (P2-13): duyệt / từ chối, xem lịch sử điểm. */
export default function GradeRevisionPanel() {
  const [status, setStatus] = useState("PENDING");
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ items: [], totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const [notice, setNotice] = useState(null);
  const [review, setReview] = useState(null); // { item, action: "approve" | "reject" }
  const [note, setNote] = useState("");
  const [hours, setHours] = useState(DEFAULT_HOURS);
  const [reviewError, setReviewError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [historyItem, setHistoryItem] = useState(null);

  useEffect(() => {
    let cancelled = false;
    registrationApi
      .listRevisions({ status: status || undefined, page, size: 10 })
      .then((d) => !cancelled && setData(d))
      .catch((e) => !cancelled && setNotice({ type: "danger", message: e.message || "Không tải được danh sách yêu cầu." }))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [status, page, reloadKey]);

  // Đổi bộ lọc/trang hoặc tải lại: bật trạng thái đang tải rồi để effect lấy dữ liệu
  const reload = (change) => {
    setLoading(true);
    change();
  };

  const openReview = (item, action) => {
    setReview({ item, action });
    setNote("");
    setHours(DEFAULT_HOURS);
    setReviewError("");
  };

  const submitReview = async () => {
    const { item, action } = review;
    if (action === "reject" && !note.trim()) return setReviewError("Vui lòng nêu lý do từ chối.");
    const h = Number(hours);
    if (action === "approve" && (!Number.isInteger(h) || h < 1 || h > MAX_HOURS)) {
      return setReviewError(`Số giờ mở khóa phải từ 1 đến ${MAX_HOURS}.`);
    }
    setSubmitting(true);
    try {
      if (action === "approve") await registrationApi.approveRevision(item.id, note.trim() || undefined, h);
      else await registrationApi.rejectRevision(item.id, note.trim());
      setReview(null);
      setNotice({ type: "success", message: action === "approve" ? "Đã duyệt và mở khóa điểm." : "Đã từ chối yêu cầu." });
      reload(() => setReloadKey((k) => k + 1));
    } catch (e) {
      setReviewError(e.message || "Thao tác thất bại.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 p-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h3 className="text-xl font-black uppercase tracking-tight text-on-surface">Yêu cầu chấm lại điểm</h3>
          <p className="mt-1 text-xs font-medium text-outline">
            Đợt đã kết thúc thì điểm bị khóa. Duyệt yêu cầu sẽ cho giảng viên sửa điểm một lần trong thời gian mở khóa.
          </p>
        </div>
        <select
          className="rounded-xl bg-surface-container-low px-4 py-3 text-xs font-black uppercase tracking-widest outline-none"
          value={status}
          onChange={(e) => {
            const next = e.target.value;
            reload(() => {
              setPage(0);
              setStatus(next);
            });
          }}
        >
          <option value="PENDING">Chờ duyệt</option>
          <option value="APPROVED">Đã mở khóa</option>
          <option value="USED">Đã sửa điểm</option>
          <option value="REJECTED">Từ chối</option>
          <option value="EXPIRED">Hết hạn</option>
          <option value="">Tất cả</option>
        </select>
      </div>

      {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} autoHideMs={3000} />}

      <div className="overflow-x-auto rounded-2xl border border-outline-variant/10">
        <table className="w-full min-w-[900px] text-left text-sm">
          <thead className="bg-surface-container-low/40 text-[10px] font-black uppercase tracking-widest text-outline">
            <tr>
              <th className="px-4 py-3">Gửi lúc</th>
              <th className="px-4 py-3">Sinh viên / Đề tài</th>
              <th className="px-4 py-3">Giảng viên</th>
              <th className="px-4 py-3">Điểm</th>
              <th className="px-4 py-3">Lý do</th>
              <th className="px-4 py-3">Trạng thái</th>
              <th className="px-4 py-3 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/10">
            {loading && (
              <tr><td colSpan="7" className="px-4 py-10 text-center text-outline">Đang tải...</td></tr>
            )}
            {!loading && data.items?.length === 0 && (
              <tr><td colSpan="7" className="px-4 py-10 text-center text-outline">Không có yêu cầu nào.</td></tr>
            )}
            {!loading && data.items?.map((it) => {
              const [label, cls] = STATUS[it.status] || [it.status, ""];
              return (
                <tr key={it.id}>
                  <td className="px-4 py-3 whitespace-nowrap">{fmtDateTime(it.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="font-black">{it.studentName} <span className="font-bold text-outline">({it.studentCode})</span></div>
                    <div className="text-xs text-outline">{it.topicTitle}{it.workspaceName ? ` · ${it.workspaceName}` : ""}</div>
                  </td>
                  <td className="px-4 py-3">{it.requestedByName}</td>
                  <td className="px-4 py-3 font-black">{fmtScore(it.currentScore)}</td>
                  <td className="px-4 py-3 max-w-xs">
                    <p className="line-clamp-3">{it.reason}</p>
                    {it.reviewNote && <p className="mt-1 text-xs text-outline">Ghi chú duyệt: {it.reviewNote}</p>}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2 py-1 text-[10px] font-black ${cls}`}>{label}</span>
                    {it.status === "APPROVED" && it.unlockUntil && (
                      <div className="mt-1 text-[10px] text-outline">đến {fmtDateTime(it.unlockUntil)}</div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button className="rounded-xl bg-surface-container px-3 py-2 text-[10px] font-black uppercase" onClick={() => setHistoryItem(it)}>
                        Lịch sử
                      </button>
                      {it.status === "PENDING" && (
                        <>
                          <button className="rounded-xl bg-primary px-3 py-2 text-[10px] font-black uppercase text-white" onClick={() => openReview(it, "approve")}>
                            Duyệt
                          </button>
                          <button className="rounded-xl bg-error/10 px-3 py-2 text-[10px] font-black uppercase text-error" onClick={() => openReview(it, "reject")}>
                            Từ chối
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {data.totalPages > 1 && (
        <div className="flex items-center justify-end gap-2 text-xs font-black">
          <button className="rounded-xl bg-surface-container px-3 py-2 disabled:opacity-40" disabled={page === 0} onClick={() => reload(() => setPage((p) => p - 1))}>Trước</button>
          <span>Trang {page + 1}/{data.totalPages}</span>
          <button className="rounded-xl bg-surface-container px-3 py-2 disabled:opacity-40" disabled={page + 1 >= data.totalPages} onClick={() => reload(() => setPage((p) => p + 1))}>Sau</button>
        </div>
      )}

      {review && (
        <div className="fixed inset-0 z-[110] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={() => !submitting && setReview(null)}></div>
          <div className="relative z-10 w-full max-w-lg space-y-5 rounded-3xl bg-white p-8 shadow-2xl dark:bg-slate-900">
            <h3 className="text-lg font-black uppercase">
              {review.action === "approve" ? "Duyệt yêu cầu chấm lại" : "Từ chối yêu cầu chấm lại"}
            </h3>
            <p className="text-sm text-on-surface-variant">
              {review.item.studentName} — {review.item.topicTitle}. Lý do của giảng viên: <i>{review.item.reason}</i>
            </p>
            {review.action === "approve" && (
              <label className="block space-y-1.5">
                <span className="text-[10px] font-black uppercase tracking-widest text-outline">Mở khóa trong (giờ, tối đa {MAX_HOURS})</span>
                <input type="number" min={1} max={MAX_HOURS} step={1} value={hours} onChange={(e) => setHours(e.target.value)}
                  className="w-full rounded-xl bg-surface-container-low px-4 py-3 font-black outline-none" />
              </label>
            )}
            <label className="block space-y-1.5">
              <span className="text-[10px] font-black uppercase tracking-widest text-outline">
                {review.action === "approve" ? "Ghi chú (không bắt buộc)" : "Lý do từ chối *"}
              </span>
              <textarea value={note} maxLength={1000} onChange={(e) => setNote(e.target.value)}
                className="min-h-[90px] w-full rounded-xl bg-surface-container-low px-4 py-3 text-sm outline-none" />
            </label>
            {reviewError && <p className="rounded-xl bg-error/10 px-4 py-3 text-xs font-bold text-error" role="alert">{reviewError}</p>}
            <div className="flex justify-end gap-3">
              <button className="rounded-xl px-5 py-3 text-xs font-black uppercase" disabled={submitting} onClick={() => setReview(null)}>Hủy</button>
              <button
                className={`rounded-xl px-6 py-3 text-xs font-black uppercase text-white ${review.action === "approve" ? "bg-primary" : "bg-error"} disabled:opacity-50`}
                disabled={submitting}
                onClick={submitReview}
              >
                {submitting ? "Đang xử lý..." : review.action === "approve" ? "Duyệt & mở khóa" : "Từ chối"}
              </button>
            </div>
          </div>
        </div>
      )}

      {historyItem && (
        <GradeHistoryModal
          registrationId={historyItem.registrationId}
          title={`${historyItem.studentName || ""} — ${historyItem.topicTitle || ""}`}
          onClose={() => setHistoryItem(null)}
        />
      )}
    </div>
  );
}
