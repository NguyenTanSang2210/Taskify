import React, { useEffect, useState, useCallback, useMemo } from "react";
import { registrationApi } from "../api/registrationApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { topicApi } from "../api/topicApi";
import { connectAndSubscribe } from "../ws/stomp";
import ProgressRow from "../components/ProgressRow";
import { useAuth } from "../context/AuthContext";

export default function ProjectSpacePage() {
  const { user } = useAuth();
  const role = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isStudent = role === "STUDENT";
  const isLecturer = role === "LECTURER";

  const [topicId, setTopicId] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);

  // --- STUDENT STATE ---
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // --- LECTURER STATE ---
  const [lecturerTopics, setLecturerTopics] = useState([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loadingId, setLoadingId] = useState(null);

  // --- LOAD TOPIC (STUDENT) ---
  const loadStudentTopic = useCallback(async () => {
    if (!isStudent || !user?.id) return;
    try {
      const data = await registrationApi.getMine(user.id);
      const approved = (data || []).filter((r) => r.approved === true);
      if (approved.length > 0) {
        setTopicId(String(approved[0].topic?.id || ""));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isStudent, user?.id]);

  // --- LOAD TOPICS (LECTURER) ---
  const loadLecturerTopics = useCallback(async () => {
    if (!isLecturer || !user?.id) return;
    try {
      // Get all topics for this lecturer to populate the select box
      const data = await topicApi.getAllTopics({ lecturerId: user.id, size: 100 });
      setLecturerTopics(data.items || []);
      if (data.items?.length > 0 && !topicId) {
        setTopicId(String(data.items[0].id));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isLecturer, user?.id, topicId]);

  useEffect(() => {
    if (isStudent) loadStudentTopic();
    if (isLecturer) loadLecturerTopics();
  }, [loadStudentTopic, loadLecturerTopics, isStudent, isLecturer]);

  // --- LOAD PROGRESS ITEMS ---
  const loadProgress = useCallback(async () => {
    if (!topicId) {
      setItems([]);
      return;
    }
    setLoading(true);
    try {
      let arr = [];
      if (isStudent) {
        arr = await progressApi.listByStudent(user.id, topicId);
      } else {
        arr = await progressApi.listByTopic(topicId);
      }
      setItems(arr || []);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không tải được dữ liệu tiến độ" });
    } finally {
      setLoading(false);
    }
  }, [topicId, isStudent, user?.id]);

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
        if (type === "PROGRESS_CREATED") {
          setItems((prev) => {
            const exists = prev.find((x) => x.id === pr.id);
            return exists ? prev : [pr, ...prev];
          });
          if (isLecturer) {
             setNotice({ type: "info", message: "Có báo cáo mới từ sinh viên" });
             setTimeout(() => setNotice(null), 3000);
          }
        } else if (type === "PROGRESS_UPDATED") {
          setItems((prev) => prev.map((x) => (x.id === pr.id ? pr : x)));
          if (isStudent) {
             setNotice({ type: "info", message: "Giảng viên đã cập nhật đánh giá" });
             setTimeout(() => setNotice(null), 3000);
          }
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId, isStudent, isLecturer]);

  // --- STUDENT ACTIONS ---
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
      loadProgress(); // Reload to be sure
    } catch (err) {
      console.error(err);
      setNotice({ type: "danger", message: "Lỗi gửi báo cáo" });
      setTimeout(() => setNotice(null), 2500);
    } finally {
      setSubmitting(false);
    }
  };

  // --- LECTURER ACTIONS ---
  const handleLecturerUpdate = async (it, nextStatus, nextComment, nextDeadline) => {
    setLoadingId(it.id);
    try {
      await progressApi.comment(it.id, {
        status: nextStatus,
        lecturerComment: nextComment,
        deadline: nextDeadline,
      });
      // WebSocket handles update, but we can reload to be safe or wait for WS
      // Ideally WS updates UI.
      return true;
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Cập nhật thất bại" });
      setTimeout(() => setNotice(null), 2500);
      return false;
    } finally {
      setLoadingId(null);
    }
  };

  // --- FILTERS (LECTURER) ---
  const filteredItems = useMemo(() => {
    let arr = items;
    if (isLecturer) {
        const q = search.trim().toLowerCase();
        if (q) {
            arr = arr.filter((x) => {
                const sName = x.student?.user?.fullName || "";
                const sCode = x.student?.studentCode || "";
                return (
                    x.title.toLowerCase().includes(q) ||
                    sName.toLowerCase().includes(q) ||
                    sCode.toLowerCase().includes(q)
                );
            });
        }
        if (statusFilter) {
            arr = arr.filter(x => x.status === statusFilter);
        }
    }
    return arr;
  }, [items, search, statusFilter, isLecturer]);


  if (!role) return <div className="p-4">Vui lòng đăng nhập</div>;

  return (
    <div className="container-fluid py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0">
            <i className="bi bi-kanban me-2"></i>
            Không gian làm việc {isStudent ? "(Sinh viên)" : "(Giảng viên)"}
        </h2>
      </div>

      {notice && (
        <div className={`alert alert-${notice.type} alert-dismissible fade show`} role="alert">
          {notice.message}
          <button type="button" className="btn-close" onClick={() => setNotice(null)}></button>
        </div>
      )}

      {/* --- LECTURER TOOLBAR --- */}
      {isLecturer && (
        <div className="card mb-4 shadow-sm">
            <div className="card-body">
                <div className="row g-3">
                    <div className="col-md-4">
                        <label className="form-label fw-bold">Chọn đề tài</label>
                        <select className="form-select" value={topicId} onChange={e => setTopicId(e.target.value)}>
                            {lecturerTopics.map(t => (
                                <option key={t.id} value={t.id}>{t.title}</option>
                            ))}
                            {lecturerTopics.length === 0 && <option value="">Không có đề tài nào</option>}
                        </select>
                    </div>
                    <div className="col-md-4">
                        <label className="form-label fw-bold">Tìm kiếm</label>
                        <input 
                            className="form-control" 
                            placeholder="Tiêu đề, Tên SV, MSSV..." 
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                    </div>
                    <div className="col-md-4">
                        <label className="form-label fw-bold">Trạng thái</label>
                        <select className="form-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                            <option value="">Tất cả</option>
                            <option value="TODO">TODO (Chưa làm)</option>
                            <option value="IN_PROGRESS">IN_PROGRESS (Đang làm)</option>
                            <option value="DONE">DONE (Hoàn thành)</option>
                        </select>
                    </div>
                </div>
            </div>
        </div>
      )}

      {/* --- STUDENT TOOLBAR / FORM --- */}
      {isStudent && (
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
                                {!loading && filteredItems.length === 0 && (
                                    <tr><td colSpan="4" className="text-center py-3 text-muted">Chưa có báo cáo nào</td></tr>
                                )}
                                {!loading && filteredItems.map(it => (
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
      )}

      {/* --- LECTURER LIST VIEW --- */}
      {isLecturer && (
          <div className="card shadow-sm">
            <div className="card-header bg-white d-flex justify-content-between align-items-center">
                <h5 className="mb-0 text-primary">Danh sách báo cáo từ sinh viên</h5>
                <button className="btn btn-sm btn-outline-secondary" onClick={loadProgress}>
                    <i className="bi bi-arrow-clockwise me-1"></i>Làm mới
                </button>
            </div>
            <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                    <thead className="table-light">
                        <tr>
                            <th style={{ width: "15%" }}>Thời gian</th>
                            <th style={{ width: "20%" }}>Sinh viên</th>
                            <th style={{ width: "20%" }}>Nội dung</th>
                            <th style={{ width: "15%" }}>Trạng thái / Hạn</th>
                            <th style={{ width: "20%" }}>Nhận xét</th>
                            <th style={{ width: "10%" }}>Lưu</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading && (
                            <tr><td colSpan="6" className="text-center py-3">Đang tải...</td></tr>
                        )}
                        {!loading && filteredItems.length === 0 && (
                            <tr><td colSpan="6" className="text-center py-3 text-muted">Chưa có báo cáo nào</td></tr>
                        )}
                        {!loading && filteredItems.map(it => (
                            <ProgressRow 
                                key={it.id} 
                                item={it} 
                                loadingId={loadingId} 
                                onSave={(s, c, d) => handleLecturerUpdate(it, s, c, d)} 
                            />
                        ))}
                    </tbody>
                </table>
            </div>
          </div>
      )}
    </div>
  );
}
