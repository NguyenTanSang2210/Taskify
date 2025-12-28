import React, { useEffect, useState, useCallback } from "react";
import { registrationApi } from "../api/registrationApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { connectAndSubscribe } from "../ws/stomp";
import { useAuth } from "../context/AuthContext";

export default function StudentProgressPage() {
  const { user } = useAuth();

  const [topicId, setTopicId] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // --- LOAD TOPIC ---
  const loadStudentTopic = useCallback(async () => {
    if (!user?.id) return;
    try {
      const data = await registrationApi.getMine(user.id);
      const approved = (data || []).filter((r) => r.approved === true);
      if (approved.length > 0) {
        setTopicId(String(approved[0].topic?.id || ""));
      }
    } catch (e) {
      console.error(e);
    }
  }, [user?.id]);

  useEffect(() => {
    loadStudentTopic();
  }, [loadStudentTopic]);

  // --- LOAD PROGRESS ---
  const loadProgress = useCallback(async () => {
    if (!topicId) {
      setItems([]);
      return;
    }
    setLoading(true);
    try {
      const arr = await progressApi.listByStudent(user.id, topicId);
      setItems(arr || []);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không tải được dữ liệu tiến độ" });
    } finally {
      setLoading(false);
    }
  }, [topicId, user?.id]);

  useEffect(() => {
    loadProgress();
  }, [loadProgress]);

  // --- WEBSOCKET ---
  useEffect(() => {
    if (!topicId) return;
    const client = connectAndSubscribe({
      destination: `/topic/progress/${topicId}`,
      onMessage: (payload) => {
        const type = payload?.type;
        const pr = payload?.progress;
        if (!type || !pr) return;
        if (type === "PROGRESS_UPDATED") {
          setItems((prev) => prev.map((x) => (x.id === pr.id ? pr : x)));
          setNotice({ type: "info", message: "Giảng viên đã cập nhật đánh giá" });
          setTimeout(() => setNotice(null), 3000);
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId]);

  // --- SUBMIT ---
  const handleStudentSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setNotice({ type: "warning", message: "Vui lòng nhập tiêu đề" });
      setTimeout(() => setNotice(null), 2000);
      return;
    }
    setSubmitting(true);
    try {
      let fileUrl = "";
      if (file) {
        fileUrl = await fileApi.upload(file);
      }
      await progressApi.create(user.id, topicId, {
        title: title.trim(),
        content: content.trim(),
        fileUrl,
      });
      setNotice({ type: "success", message: "Đã gửi báo cáo" });
      setTitle("");
      setContent("");
      setFile(null);
      setTimeout(() => setNotice(null), 2500);
      loadProgress();
    } catch (err) {
      console.error(err);
      setNotice({ type: "danger", message: "Lỗi gửi báo cáo" });
      setTimeout(() => setNotice(null), 2500);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container-fluid py-4">
      <h2 className="mb-4">Theo dõi tiến độ</h2>
      
      {notice && (
        <div className={`alert alert-${notice.type} alert-dismissible fade show`} role="alert">
          {notice.message}
          <button type="button" className="btn-close" onClick={() => setNotice(null)}></button>
        </div>
      )}

      <div className="row mb-4">
            <div className="col-md-4">
                <div className="card shadow-sm h-100">
                    <div className="card-header bg-primary text-white">
                        <h5 className="mb-0">Tạo báo cáo mới</h5>
                    </div>
                    <div className="card-body">
                        {!topicId ? (
                            <div className="text-muted text-center py-3">Bạn chưa có đề tài được duyệt.</div>
                        ) : (
                            <form onSubmit={handleStudentSubmit}>
                                <div className="mb-3">
                                    <label className="form-label">Tiêu đề <span className="text-danger">*</span></label>
                                    <input 
                                        className="form-control" 
                                        value={title} 
                                        onChange={e => setTitle(e.target.value)} 
                                        placeholder="Ví dụ: Báo cáo tuần 1"
                                    />
                                </div>
                                <div className="mb-3">
                                    <label className="form-label">Nội dung</label>
                                    <textarea 
                                        className="form-control" 
                                        rows={4}
                                        value={content}
                                        onChange={e => setContent(e.target.value)}
                                        placeholder="Mô tả công việc đã làm..."
                                    ></textarea>
                                </div>
                                <div className="mb-3">
                                    <label className="form-label">Đính kèm tệp</label>
                                    <input 
                                        type="file" 
                                        className="form-control"
                                        onChange={e => setFile(e.target.files[0])}
                                    />
                                </div>
                                <button className="btn btn-primary w-100" type="submit" disabled={submitting}>
                                    {submitting ? "Đang gửi..." : "Gửi báo cáo"}
                                </button>
                            </form>
                        )}
                    </div>
                </div>
            </div>
            <div className="col-md-8">
                <div className="card shadow-sm h-100">
                    <div className="card-header bg-white d-flex justify-content-between align-items-center">
                        <h5 className="mb-0 text-primary">Lịch sử báo cáo</h5>
                        <button className="btn btn-sm btn-outline-secondary" onClick={loadProgress}>
                            <i className="bi bi-arrow-clockwise me-1"></i>Làm mới
                        </button>
                    </div>
                    <div className="table-responsive">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light">
                                <tr>
                                    <th>Thời gian</th>
                                    <th>Tiêu đề</th>
                                    <th>Trạng thái</th>
                                    <th>Nhận xét GV</th>
                                </tr>
                            </thead>
                            <tbody>
                                {loading && (
                                    <tr><td colSpan="4" className="text-center py-3">Đang tải...</td></tr>
                                )}
                                {!loading && items.length === 0 && (
                                    <tr><td colSpan="4" className="text-center py-3 text-muted">Chưa có báo cáo nào</td></tr>
                                )}
                                {!loading && items.map(it => (
                                    <tr key={it.id}>
                                        <td className="small text-muted">{new Date(it.createdAt).toLocaleString()}</td>
                                        <td>
                                            <div className="fw-bold">{it.title}</div>
                                            <div className="small text-muted text-truncate" style={{maxWidth: 200}}>{it.content}</div>
                                            {it.fileUrl && (
                                                <a href={it.fileUrl} target="_blank" rel="noreferrer" className="small text-primary text-decoration-none">
                                                    <i className="bi bi-paperclip"></i> Tệp đính kèm
                                                </a>
                                            )}
                                        </td>
                                        <td>
                                            <span className={`badge ${
                                                it.status === 'DONE' ? 'bg-success' : 
                                                it.status === 'IN_PROGRESS' ? 'bg-warning text-dark' : 'bg-secondary'
                                            }`}>
                                                {it.status}
                                            </span>
                                        </td>
                                        <td>
                                            {it.lecturerComment ? (
                                                <div className="small bg-light p-2 rounded">
                                                    {it.lecturerComment}
                                                </div>
                                            ) : (
                                                <span className="text-muted small">-</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
  );
}
