import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import InlineNotice from "../components/InlineNotice";
import { milestoneApi } from "../api/milestoneApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { useAuth } from "../context/AuthContext";

export default function StudentProgressDetailPage() {
  const { user } = useAuth();
  const studentId = user?.id;
  const navigate = useNavigate();
  const location = useLocation();

  const [milestoneId, setMilestoneId] = useState("");
  const [milestone, setMilestone] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [fileUrl, setFileUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const msId = params.get("milestoneId");
    if (msId) setMilestoneId(msId);
  }, [location.search]);

  useEffect(() => {
    const load = async () => {
      if (!milestoneId) return;
      setLoading(true);
      try {
        const ms = await milestoneApi.get(milestoneId);
        setMilestone(ms);
        setTitle(ms?.title || "");
      } catch (e) {
        setNotice({ type: "danger", message: e.message || "Không tải được thông tin mốc" });
        setTimeout(() => setNotice(null), 3000);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [milestoneId]);

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    try {
      const data = await fileApi.upload(file);
      setFileUrl(data.fileUrl);
      setNotice({ type: "success", message: "Đã tải tệp lên" });
      setTimeout(() => setNotice(null), 2000);
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Tải tệp thất bại" });
      setTimeout(() => setNotice(null), 3000);
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async () => {
    if (!studentId || !milestone?.topic?.id) {
      setNotice({ type: "warning", message: "Thiếu thông tin sinh viên hoặc đề tài" });
      setTimeout(() => setNotice(null), 2500);
      return;
    }
    if (!title.trim() || !content.trim()) {
      setNotice({ type: "warning", message: "Tiêu đề và nội dung không được để trống" });
      setTimeout(() => setNotice(null), 2500);
      return;
    }

    setSubmitting(true);
    try {
      let finalFileUrl = fileUrl;
      
      // Auto-upload if file selected but not yet uploaded
      if (file && !finalFileUrl) {
        try {
            const upData = await fileApi.upload(file);
            finalFileUrl = upData.fileUrl;
        } catch (upErr) {
            throw new Error("Tự động tải tệp thất bại: " + (upErr.message || "Lỗi không xác định"));
        }
      }

      const payload = {
        title: title.trim(),
        content: content.trim(),
        fileUrl: finalFileUrl || undefined,
        deadline: milestone?.deadline || undefined,
        milestoneId: milestone?.id || undefined,
      };
      await progressApi.create(studentId, milestone.topic.id, payload);
      setNotice({ type: "success", message: "Đã nộp báo cáo tiến độ" });
      setTimeout(() => setNotice(null), 2000);
      navigate("/progress");
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Nộp báo cáo thất bại" });
      setTimeout(() => setNotice(null), 3000);
    } finally {
      setSubmitting(false);
    }
  };

  const timeRemaining = (() => {
    if (!milestone?.deadline) return null;
    const diffMs = new Date(milestone.deadline) - new Date();
    if (diffMs <= 0) return "Đã quá hạn";
    const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diffMs / (1000 * 60 * 60)) % 24);
    return `${days} ngày ${hours} giờ`;
  })();

  return (
    <div className="container py-4">
      <h3>Chi tiết tiến độ</h3>
      {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} />}

      {loading ? (
        <div>Đang tải...</div>
      ) : !milestone ? (
        <div className="text-danger">Không tìm thấy mốc tiến độ</div>
      ) : (
        <>
          {/* Phần 1: Thông tin mốc */}
          <div className="card mb-3">
            <div className="card-body">
              <div className="d-flex justify-content-between">
                <div>
                  <div className="fw-bold">{milestone.title}</div>
                  <div className="small text-muted">Đề tài: #{milestone.topic?.id} - {milestone.topic?.title}</div>
                </div>
                <div className="text-end">
                  <div>Hạn chót: {milestone.deadline ? new Date(milestone.deadline).toLocaleString() : "-"}</div>
                  <div className={`small ${timeRemaining === "Đã quá hạn" ? "text-danger fw-bold" : "text-muted"}`}>
                    {timeRemaining ? `Còn lại: ${timeRemaining}` : ""}
                  </div>
                </div>
              </div>
              {milestone.description && <div className="mt-2 small">{milestone.description}</div>}
            </div>
          </div>

          {/* Phần 2: Nội dung văn bản */}
          <div className="card mb-3">
            <div className="card-body">
              <h6 className="card-title">Văn bản bài báo cáo</h6>
              <div className="mb-3">
                <label className="form-label">Tiêu đề</label>
                <input className="form-control" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} />
              </div>
              <div className="mb-3">
                <label className="form-label">Nội dung</label>
                <textarea className="form-control" rows={8} value={content} onChange={(e) => setContent(e.target.value)} placeholder="Nhập nội dung báo cáo..." />
              </div>
            </div>
          </div>

          {/* Phần 3: Đính kèm tệp */}
          <div className="card mb-3">
            <div className="card-body">
              <h6 className="card-title">Đính kèm</h6>
              <div className="mb-2">
                <input type="file" className="form-control" onChange={(e) => setFile(e.target.files?.[0] || null)} />
              </div>
              <div className="d-flex align-items-center gap-2">
                <button className="btn btn-outline-primary btn-sm" disabled={!file || uploading} onClick={handleUpload}>
                  {uploading ? "Đang tải..." : "Tải tệp lên"}
                </button>
                {fileUrl && (
                  <a className="btn btn-outline-secondary btn-sm" href={fileUrl} target="_blank" rel="noreferrer">
                    Xem tệp đã tải
                  </a>
                )}
                {fileUrl && (
                  <button className="btn btn-outline-danger btn-sm" onClick={() => setFileUrl("")}>
                    Gỡ tệp
                  </button>
                )}
              </div>
              <div className="form-text mt-2">Chấp nhận các định dạng tệp thông dụng. Kích thước tối đa tùy cấu hình hệ thống.</div>
            </div>
          </div>

          {/* Phần 4: Gửi */}
          <div className="text-end">
            <button className="btn btn-secondary me-2" onClick={() => navigate(-1)}>Hủy</button>
            <button className="btn btn-primary" disabled={submitting} onClick={handleSubmit}>
              {submitting ? "Đang gửi..." : "Gửi báo cáo"}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

