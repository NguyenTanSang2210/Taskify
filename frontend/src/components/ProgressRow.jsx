import React, { useState } from "react";

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

  return (
    <tr key={item.id}>
      <td>{new Date(item.createdAt).toLocaleString()}</td>
      <td>
        <div className="small">
          <div><strong>{user.fullName || "-"}</strong></div>
          <div className="text-muted">MSSV: {student.studentCode || "-"}</div>
        </div>
      </td>
      <td>{item.title}</td>
      <td className="small">
        <div>{item.content}</div>
        {item.fileUrl && (
          <div className="mt-1">
            <a href={item.fileUrl} target="_blank" rel="noreferrer" className="btn btn-sm btn-outline-primary py-0">
              <i className="bi bi-paperclip me-1"></i>Tải về
            </a>
          </div>
        )}
      </td>
      <td>
        <select className="form-select form-select-sm" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="TODO">Chưa làm</option>
          <option value="IN_PROGRESS">Đang làm</option>
          <option value="DONE">Hoàn thành</option>
        </select>
        <div className="mt-2">
            <label className="form-label small mb-0">Hạn chót:</label>
            <input 
                type="datetime-local" 
                className={`form-control form-control-sm ${new Date(deadline) < new Date() && status !== 'DONE' ? 'is-invalid' : ''}`}
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
            />
            {deadline && new Date(deadline) < new Date() && status !== 'DONE' && (
                <div className="text-danger small mt-1">
                    <i className="bi bi-exclamation-circle me-1"></i>Đã quá hạn
                </div>
            )}
        </div>
      </td>
      <td>
        <textarea
          className="form-control form-control-sm"
          rows={3}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Nhận xét của giảng viên..."
        />
      </td>
      <td className="text-end">
        {saved && !error && (
          <div className="save-toast mb-2">
            <svg className="checkmark" viewBox="0 0 52 52">
              <circle className="checkmark__circle" cx="26" cy="26" r="25" fill="none" />
              <path className="checkmark__check" fill="none" d="M14 27l7 7 16-16" />
            </svg>
            <span className="ms-2 fw-semibold text-success">Đã lưu</span>
          </div>
        )}
        {error && (
          <div className="save-toast mb-2">
            <span className="fw-semibold text-danger">Lưu thất bại</span>
          </div>
        )}
        <button
          className="btn btn-primary btn-sm"
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
          {saving ? "Đang lưu..." : "Lưu"}
        </button>
      </td>
    </tr>
  );
}
