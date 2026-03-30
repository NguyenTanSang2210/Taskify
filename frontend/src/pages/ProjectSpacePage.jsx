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

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [lecturerTopics, setLecturerTopics] = useState([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loadingId, setLoadingId] = useState(null);

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

  const loadLecturerTopics = useCallback(async () => {
    if (!isLecturer || !user?.id) return;
    try {
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
      setNotice({ type: "danger", message: "Khong tai duoc du lieu tien do" });
    } finally {
      setLoading(false);
    }
  }, [topicId, isStudent, user?.id]);

  useEffect(() => {
    loadProgress();
  }, [loadProgress]);

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
            setNotice({ type: "info", message: "Co bao cao moi tu sinh vien" });
            setTimeout(() => setNotice(null), 3000);
          }
        } else if (type === "PROGRESS_UPDATED") {
          setItems((prev) => prev.map((x) => (x.id === pr.id ? pr : x)));
          if (isStudent) {
            setNotice({ type: "info", message: "Giang vien da cap nhat danh gia" });
            setTimeout(() => setNotice(null), 3000);
          }
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId, isStudent, isLecturer]);

  const handleStudentSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setNotice({ type: "warning", message: "Vui long nhap tieu de" });
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
      setNotice({ type: "success", message: "Da gui bao cao" });
      setTitle("");
      setContent("");
      setFile(null);
      setTimeout(() => setNotice(null), 2500);
      loadProgress();
    } catch (err) {
      console.error(err);
      setNotice({ type: "danger", message: "Loi gui bao cao" });
      setTimeout(() => setNotice(null), 2500);
    } finally {
      setSubmitting(false);
    }
  };

  const handleLecturerUpdate = async (it, nextStatus, nextComment, nextDeadline) => {
    setLoadingId(it.id);
    try {
      await progressApi.comment(it.id, {
        status: nextStatus,
        lecturerComment: nextComment,
        deadline: nextDeadline,
      });
      return true;
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Cap nhat that bai" });
      setTimeout(() => setNotice(null), 2500);
      return false;
    } finally {
      setLoadingId(null);
    }
  };

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
        arr = arr.filter((x) => x.status === statusFilter);
      }
    }
    return arr;
  }, [items, search, statusFilter, isLecturer]);

  if (!role) return <div className="p-4">Vui long dang nhap</div>;

  return (
    <div className="container-fluid py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0">
          <i className="bi bi-kanban me-2"></i>
          Khong gian lam viec {isStudent ? "(Sinh vien)" : "(Giang vien)"}
        </h2>
      </div>

      {notice && (
        <div className={`alert alert-${notice.type} alert-dismissible fade show`} role="alert">
          {notice.message}
          <button type="button" className="btn-close" onClick={() => setNotice(null)}></button>
        </div>
      )}

      {isLecturer && (
        <div className="card mb-4 shadow-sm">
          <div className="card-body">
            <div className="row g-3">
              <div className="col-md-4">
                <label className="form-label fw-bold">Chon de tai</label>
                <select className="form-select" value={topicId} onChange={(e) => setTopicId(e.target.value)}>
                  {lecturerTopics.map((t) => (
                    <option key={t.id} value={t.id}>{t.title}</option>
                  ))}
                  {lecturerTopics.length === 0 && <option value="">Khong co de tai nao</option>}
                </select>
              </div>
              <div className="col-md-4">
                <label className="form-label fw-bold">Tim kiem</label>
                <input
                  className="form-control"
                  placeholder="Tieu de, Ten SV, MSSV..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>
              <div className="col-md-4">
                <label className="form-label fw-bold">Trang thai</label>
                <select className="form-select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                  <option value="">Tat ca</option>
                  <option value="TODO">TODO (Chua lam)</option>
                  <option value="IN_PROGRESS">IN_PROGRESS (Dang lam)</option>
                  <option value="DONE">DONE (Hoan thanh)</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      )}

      {isStudent && (
        <div className="row mb-4">
          <div className="col-md-4">
            <div className="card shadow-sm h-100">
              <div className="card-header bg-primary text-white">
                <h5 className="mb-0">Tao bao cao moi</h5>
              </div>
              <div className="card-body">
                {!topicId ? (
                  <div className="text-muted text-center py-3">Ban chua co de tai duoc duyet.</div>
                ) : (
                  <form onSubmit={handleStudentSubmit}>
                    <div className="mb-3">
                      <label className="form-label">Tieu de <span className="text-danger">*</span></label>
                      <input
                        className="form-control"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="Vi du: Bao cao tuan 1"
                      />
                    </div>
                    <div className="mb-3">
                      <label className="form-label">Noi dung</label>
                      <textarea
                        className="form-control"
                        rows={4}
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="Mo ta cong viec da lam..."
                      ></textarea>
                    </div>
                    <div className="mb-3">
                      <label className="form-label">Dinh kem tep</label>
                      <input
                        type="file"
                        className="form-control"
                        onChange={(e) => setFile(e.target.files[0])}
                      />
                    </div>
                    <button className="btn btn-primary w-100" type="submit" disabled={submitting}>
                      {submitting ? "Dang gui..." : "Gui bao cao"}
                    </button>
                  </form>
                )}
              </div>
            </div>
          </div>
          <div className="col-md-8">
            <div className="card shadow-sm h-100">
              <div className="card-header bg-white d-flex justify-content-between align-items-center">
                <h5 className="mb-0 text-primary">Lich su bao cao</h5>
                <button type="button" className="btn btn-sm btn-outline-secondary" onClick={loadProgress}>
                  <i className="bi bi-arrow-clockwise me-1"></i>Lam moi
                </button>
              </div>
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th>Thoi gian</th>
                      <th>Tieu de</th>
                      <th>Trang thai</th>
                      <th>Nhan xet GV</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loading && <tr><td colSpan="4" className="text-center py-3">Dang tai...</td></tr>}
                    {!loading && filteredItems.length === 0 && (
                      <tr><td colSpan="4" className="text-center py-3 text-muted">Chua co bao cao nao</td></tr>
                    )}
                    {!loading && filteredItems.map((it) => (
                      <tr key={it.id}>
                        <td className="small text-muted">{new Date(it.createdAt).toLocaleString()}</td>
                        <td>
                          <div className="fw-bold">{it.title}</div>
                          <div className="small text-muted text-truncate" style={{ maxWidth: 200 }}>{it.content}</div>
                          {it.fileUrl && (
                            <a href={it.fileUrl} target="_blank" rel="noreferrer" className="small text-primary text-decoration-none">
                              <i className="bi bi-paperclip"></i> Tep dinh kem
                            </a>
                          )}
                        </td>
                        <td>
                          <span className={`badge ${
                            it.status === "DONE" ? "bg-success" :
                            it.status === "IN_PROGRESS" ? "bg-warning text-dark" : "bg-secondary"
                          }`}>
                            {it.status}
                          </span>
                        </td>
                        <td>
                          {it.lecturerComment ? (
                            <div className="small bg-light p-2 rounded">{it.lecturerComment}</div>
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

      {isLecturer && (
        <div className="card shadow-sm">
          <div className="card-header bg-white d-flex justify-content-between align-items-center">
            <h5 className="mb-0 text-primary">Danh sach bao cao tu sinh vien</h5>
            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={loadProgress}>
              <i className="bi bi-arrow-clockwise me-1"></i>Lam moi
            </button>
          </div>
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th style={{ width: "15%" }}>Thoi gian</th>
                  <th style={{ width: "20%" }}>Sinh vien</th>
                  <th style={{ width: "20%" }}>Noi dung</th>
                  <th style={{ width: "15%" }}>Trang thai / Han</th>
                  <th style={{ width: "20%" }}>Nhan xet</th>
                  <th style={{ width: "10%" }}>Luu</th>
                </tr>
              </thead>
              <tbody>
                {loading && <tr><td colSpan="6" className="text-center py-3">Dang tai...</td></tr>}
                {!loading && filteredItems.length === 0 && (
                  <tr><td colSpan="6" className="text-center py-3 text-muted">Chua co bao cao nao</td></tr>
                )}
                {!loading && filteredItems.map((it) => (
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
