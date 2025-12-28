import React, { useEffect, useState } from "react";
import { topicApi } from "../api/topicApi";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";

export default function LecturerTopicsPage() {
  const { user } = useAuth();
  const lecturerId = user?.id;

  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(6);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // topic object or null
  const [form, setForm] = useState({ title: "", description: "" });
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [notice, setNotice] = useState(null);

  const getStatusLabel = (s) => {
    if (s === "OPEN") return "Mở";
    if (s === "CLOSED") return "Đã đóng";
    if (s === "REGISTERED") return "Đã đăng ký";
    return s || "";
  };

  const load = async (override = {}) => {
    setLoading(true);
    setError("");
    try {
      const params = {
        lecturerId,
        query,
        status: status || undefined,
        page,
        size,
        ...override,
      };
      const data = await topicApi.getAllTopics(params);
      setItems(data.items || []);
      setTotalPages(data.totalPages || 0);
      setPage(data.page ?? page);
      setSize(data.size ?? size);
    } catch (e) {
      console.error(e);
      setError("Không tải được danh sách đề tài của giảng viên");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, query, status]);

  const openCreate = () => {
    setEditing(null);
    setForm({ title: "", description: "" });
    setModalOpen(true);
  };
  const openEdit = (t) => {
    setEditing(t);
    setForm({ title: t.title || "", description: t.description || "" });
    setModalOpen(true);
  };
  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
  };

  const handleSave = async () => {
    if (!form.title?.trim()) {
      setNotice({ type: "warning", message: "Tiêu đề không được để trống" });
      setTimeout(() => setNotice(null), 2500);
      return;
    }
    try {
      if (editing) {
        await topicApi.update(editing.id, lecturerId, {
          title: form.title.trim(),
          description: form.description?.trim() || "",
        });
      } else {
        await topicApi.create(lecturerId, {
          title: form.title.trim(),
          description: form.description?.trim() || "",
        });
      }
      await load();
      closeModal();
      setNotice({ type: "success", message: editing ? "Đã lưu chỉnh sửa đề tài" : "Đã tạo đề tài" });
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Lưu đề tài thất bại" });
      setTimeout(() => setNotice(null), 3000);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Bạn có chắc muốn xóa đề tài này?")) return;
    setActionLoadingId(id);
    try {
      await topicApi.remove(id, lecturerId);
      await load();
      setNotice({ type: "success", message: "Đã xóa đề tài" });
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Xóa đề tài thất bại" });
      setTimeout(() => setNotice(null), 3000);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleOpen = async (id) => {
    setActionLoadingId(id);
    try {
      await topicApi.open(id, lecturerId);
      await load();
      setNotice({ type: "success", message: "Đã mở đăng ký đề tài" });
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Mở đề tài thất bại" });
      setTimeout(() => setNotice(null), 3000);
    } finally {
      setActionLoadingId(null);
    }
  };
  const handleClose = async (id) => {
    // Cảnh báo nếu còn đăng ký đang chờ duyệt
    const t = items.find((it) => it.id === id);
    if (t && (t.pendingCount ?? 0) > 0) {
      const ok = window.confirm(
        `Đề tài hiện còn ${t.pendingCount} đăng ký đang CHỜ DUYỆT.\nBạn có chắc muốn ĐÓNG đề tài?`
      );
      if (!ok) {
        return; // hủy thao tác đóng
      }
    }

    setActionLoadingId(id);
    try {
      await topicApi.close(id, lecturerId);
      await load();
      setNotice({ type: "success", message: "Đã đóng đề tài" });
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Đóng đề tài thất bại" });
      setTimeout(() => setNotice(null), 3000);
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="container py-4">
      <h3>Quản lý đề tài của tôi</h3>
      {notice && (
        <InlineNotice
          type={notice.type}
          message={notice.message}
          onClose={() => setNotice(null)}
        />
      )}

      <div className="d-flex justify-content-between align-items-end mt-3">
        <div className="row g-2 align-items-end flex-grow-1">
          <div className="col-md-6">
            <label className="form-label">Tìm kiếm</label>
            <input
              type="text"
              className="form-control"
              placeholder="Nhập từ khóa..."
              value={query}
              onChange={(e) => {
                setPage(0);
                setQuery(e.target.value);
              }}
            />
          </div>
          <div className="col-md-3">
            <label className="form-label">Trạng thái</label>
            <select
              className="form-select"
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
            >
              <option value="">Tất cả</option>
              <option value="OPEN">Mở đăng ký</option>
              <option value="CLOSED">Đã đóng</option>
              <option value="REGISTERED">Đã đăng ký</option>
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label">Số lượng mỗi trang</label>
            <select
              className="form-select"
              value={size}
              onChange={(e) => {
                setPage(0);
                setSize(Number(e.target.value));
              }}
            >
              <option value={6}>6</option>
              <option value={9}>9</option>
              <option value={12}>12</option>
            </select>
          </div>
        </div>

        <button className="btn btn-primary ms-3" onClick={openCreate}>
          + Tạo đề tài
        </button>
      </div>

      {loading && <p className="text-muted mt-3">Đang tải...</p>}
      {error && !loading && <p className="text-danger mt-3">{error}</p>}
      {!loading && !error && items.length === 0 && (
        <p className="text-muted mt-3">Chưa có đề tài nào.</p>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="table-responsive mt-3">
          <table className="table table-striped">
            <thead>
              <tr>
                <th style={{width:"28%"}}>Tiêu đề</th>
                <th>Mô tả</th>
                <th style={{width:"10%"}}>Trạng thái</th>
                <th style={{width:"8%"}}>Tổng</th>
                <th style={{width:"8%"}}>Chờ</th>
                <th style={{width:"8%"}}>Duyệt</th>
                <th style={{width:"8%"}}>Từ chối</th>
                <th style={{width:"24%"}} className="text-end">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {items.map((t) => (
                <tr key={t.id}>
                  <td>{t.title}</td>
                  <td>{t.description}</td>
                  <td>
                    {t.status === "OPEN" && <span className="badge bg-success">{getStatusLabel(t.status)}</span>}
                    {t.status === "CLOSED" && <span className="badge bg-secondary">{getStatusLabel(t.status)}</span>}
                    {t.status === "REGISTERED" && <span className="badge bg-info">{getStatusLabel(t.status)}</span>}
                  </td>
                  <td>
                    <span className="badge bg-dark" title="Tổng số lượt đăng ký">
                      {t.registrationCount ?? 0}
                    </span>
                  </td>
                  <td>
                    <span className="badge bg-warning" title="Số đăng ký đang chờ duyệt">
                      {t.pendingCount ?? 0}
                    </span>
                  </td>
                  <td>
                    <span className="badge bg-success" title="Số đăng ký đã được duyệt">
                      {t.approvedCount ?? 0}
                    </span>
                  </td>
                  <td>
                    <span className="badge bg-danger" title="Số đăng ký đã bị từ chối">
                      {t.rejectedCount ?? 0}
                    </span>
                  </td>
                  <td className="text-end">
                    <button className="btn btn-outline-primary btn-sm me-2" onClick={() => openEdit(t)}>
                      Sửa
                    </button>
                    <a
                      className="btn btn-outline-info btn-sm me-2"
                      href={`/lecturer?topicId=${t.id}`}
                    >
                      Xem đăng ký
                    </a>
                    {t.status !== "OPEN" ? (
                      <button
                        className="btn btn-outline-success btn-sm me-2"
                        onClick={() => handleOpen(t.id)}
                        disabled={actionLoadingId === t.id}
                      >
                        Mở
                      </button>
                    ) : (
                      <button
                        className="btn btn-outline-warning btn-sm me-2"
                        onClick={() => handleClose(t.id)}
                        disabled={actionLoadingId === t.id}
                      >
                        Đóng
                      </button>
                    )}
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(t.id)}
                      disabled={actionLoadingId === t.id}
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Phân trang */}
      {!loading && totalPages > 0 && (
        <div className="d-flex justify-content-between align-items-center mt-3">
          <button
            className="btn btn-outline-secondary"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page <= 0}
          >
            « Trang trước
          </button>
          <span className="text-muted">
            Trang {page + 1} / {Math.max(1, totalPages)}
          </span>
          <button
            className="btn btn-outline-secondary"
            onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
            disabled={page + 1 >= totalPages}
          >
            Trang sau »
          </button>
        </div>
      )}

      {/* Modal tạo/sửa */}
      {modalOpen && (
        <div className="custom-modal-backdrop">
          <div className="custom-modal">
            <h5 className="mb-3">{editing ? "Sửa đề tài" : "Tạo đề tài"}</h5>
            <div className="mb-3">
              <label className="form-label">Tiêu đề</label>
              <input
                className="form-control"
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                maxLength={255}
                placeholder="Nhập tiêu đề đề tài"
              />
            </div>
            <div className="mb-3">
              <label className="form-label">Mô tả</label>
              <textarea
                className="form-control"
                rows={4}
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                placeholder="Nhập mô tả..."
              />
            </div>
            <div className="text-end">
              <button className="btn btn-secondary me-2" onClick={closeModal}>Hủy</button>
              <button className="btn btn-primary" onClick={handleSave}>Lưu</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
