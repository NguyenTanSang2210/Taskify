import React, { useState } from "react";
import FileDownloadButton from "./FileDownloadButton";
import { PROGRESS_STATUS, statusOptions } from "../constants/status";

/** Ngưỡng cảnh báo trùng lặp — khớp SimilarityService.ALERT_THRESHOLD ở backend. */
export const SIMILARITY_ALERT = 0.8;

/**
 * Bản cập nhật qua WebSocket là bản công khai (không có điểm trùng lặp) — giữ lại điểm đã tải cho giảng viên.
 */
export function mergeProgressUpdate(prev, pr) {
  return prev.map((x) =>
    x.id === pr.id ? { ...x, ...pr, similarityScore: x.similarityScore, similarToReportId: x.similarToReportId } : x
  );
}

function SimilarityBadge({ score, reportId }) {
  if (score == null || score < SIMILARITY_ALERT) return null;
  return (
    <span
      className="mt-1 inline-flex w-fit items-center gap-1 rounded-full bg-error/10 px-2 py-0.5 text-[10px] font-black text-error"
      title="Nội dung giống một báo cáo khác trong cùng đợt — cần kiểm tra"
    >
      <span className="material-symbols-outlined text-[12px]">content_copy</span>
      Trùng lặp {Math.round(score * 100)}% với báo cáo #{reportId}
    </span>
  );
}

export default function ProgressRow({ item, loadingId, onSave }) {
  const student = item.student || {};
  const user = student.user || {};
  const [comment, setComment] = useState(item.lecturerComment || "");
  const [status, setStatus] = useState(item.status || "TODO");
  
  // Format deadline for datetime-local input (yyyy-MM-ddThh:mm)
  const formatDeadline = (isoString) => {
    if (!isoString) return "";
    return isoString.substring(0, 16);
  };

  const [deadline, setDeadline] = useState(formatDeadline(item.deadline));
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");

  const isOverdue = deadline && new Date(deadline) < new Date() && status !== 'DONE';

  return (
    <tr key={item.id} className="hover:bg-primary/5 transition-colors group">
      <td className="px-6 py-4">
        <div className="flex flex-col">
          <span className="text-xs font-bold text-on-surface">{new Date(item.createdAt).toLocaleDateString()}</span>
          <span className="text-[10px] text-outline font-medium opacity-60">
            {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary font-black text-[10px] border border-primary/20 uppercase">
            {user.fullName?.charAt(0) || "S"}
          </div>
          <div className="flex flex-col">
            <span className="text-xs font-black text-on-surface">{user.fullName || "-"}</span>
            <span className="text-[10px] font-bold text-outline uppercase tracking-tight">{student.studentCode || "-"}</span>
          </div>
        </div>
      </td>
      <td className="px-6 py-4 max-w-xs">
        <div className="flex flex-col gap-1">
          <span className="text-sm font-black text-on-surface leading-snug">{item.title}</span>
          <SimilarityBadge score={item.similarityScore} reportId={item.similarToReportId} />
          <p className="text-xs text-outline line-clamp-2 italic">{item.content}</p>
          {item.fileUrl && (
            <FileDownloadButton
              fileUrl={item.fileUrl}
              className="mt-2 inline-flex items-center gap-1.5 text-[10px] font-black text-primary hover:underline uppercase tracking-widest"
            >
              <span className="material-symbols-outlined text-sm">attach_file</span>
              Tải tài liệu đính kèm
            </FileDownloadButton>
          )}
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="flex flex-col gap-3 min-w-[160px]">
          <select 
            className="w-full bg-surface-container border-none rounded-xl text-[11px] font-black uppercase tracking-widest text-on-surface-variant focus:ring-2 focus:ring-primary/20 py-2 cursor-pointer transition-all"
            value={status} 
            onChange={(e) => setStatus(e.target.value)}
          >
            {statusOptions(PROGRESS_STATUS).map((o) => (
                   <option key={o.value} value={o.value}>{o.label}</option>
                 ))}
          </select>
          <div className="flex flex-col gap-1">
             <div className="flex justify-between items-center px-1">
                <span className="text-[9px] font-black uppercase tracking-widest text-outline">Hạn chót:</span>
                {isOverdue && (
                  <span className="text-[9px] font-black uppercase tracking-widest text-error flex items-center gap-0.5 animate-pulse">
                    <span className="material-symbols-outlined text-[12px]">warning</span> Quá hạn
                  </span>
                )}
             </div>
             <input 
                type="datetime-local" 
                className={`w-full bg-surface-container border-none rounded-xl text-[11px] font-bold text-on-surface focus:ring-2 focus:ring-primary/20 py-1.5 transition-all ${isOverdue ? 'ring-1 ring-error/50 bg-error/5 text-error' : ''}`}
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
            />
          </div>
        </div>
      </td>
      <td className="px-6 py-4">
        <textarea
          className="w-full bg-surface-container border-none rounded-xl text-xs font-medium text-on-surface-variant focus:ring-2 focus:ring-primary/20 p-3 transition-all min-h-[80px] placeholder:text-outline/50"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Nhập nhận xét hoặc hướng dẫn cho sinh viên..."
        />
      </td>
      <td className="px-6 py-4 text-right">
        <div className="flex flex-col items-end gap-2">
          {saved && !error && (
            <div className="flex items-center gap-1.5 py-1 px-3 bg-secondary/10 text-secondary rounded-full animate-bounce">
              <span className="material-symbols-outlined text-sm font-black">check_circle</span>
              <span className="text-[10px] font-black uppercase tracking-widest">Đã lưu</span>
            </div>
          )}
          {error && (
            <div className="flex items-center gap-1.5 py-1 px-3 bg-error/10 text-error rounded-full">
              <span className="material-symbols-outlined text-sm font-black">error</span>
              <span className="text-[10px] font-black uppercase tracking-widest">Lỗi</span>
            </div>
          )}
          <button
            className={`px-6 py-2.5 rounded-xl font-black text-[11px] uppercase tracking-widest transition-all duration-300 shadow-lg ${
              saving || loadingId === item.id
                ? "bg-surface-container text-outline cursor-not-allowed"
                : "bg-primary text-on-primary shadow-primary/20 hover:scale-105 active:scale-95"
            }`}
            disabled={saving || loadingId === item.id}
            onClick={async () => {
              setError("");
              setSaving(true);
              try {
                const ok = await onSave(status, comment, deadline);
                if (ok) {
                  setSaved(true);
                  setTimeout(() => setSaved(false), 2500);
                } else {
                  setError("Lỗi");
                  setTimeout(() => setError(""), 2500);
                }
              } finally {
                setSaving(false);
              }
            }}
          >
            {saving ? "..." : "Cập nhật"}
          </button>
        </div>
      </td>
    </tr>
  );
}
