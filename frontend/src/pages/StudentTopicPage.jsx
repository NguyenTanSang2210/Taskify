import React, { useEffect, useState } from "react";
import { topicApi } from "../api/topicApi";
import { registrationApi } from "../api/registrationApi";
import TopicCard from "../components/TopicCard";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";

export default function StudentTopicPage() {
  const { user } = useAuth();
  const [topics, setTopics] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(6);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [loadingId, setLoadingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null);
  
  const roleName = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isStudent = roleName === "STUDENT";
  const studentId = isStudent ? user?.id : null;

  const load = async (override = {}) => {
    setLoading(true);
    setError("");
    try {
      const params = {
        studentId,
        query,
        status: status || undefined,
        page,
        size,
        ...override,
      };
      const data = await topicApi.getAllTopics(params);
      setTopics(data.items || []);
      setTotalPages(data.totalPages || 0);
      setPage(data.page ?? page);
      setSize(data.size ?? size);
    } catch (e) {
      console.error(e);
      setError("Không tải được danh sách đề tài");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, status, page, size]);

  const handleRegister = async (topicId) => {
    if (!isStudent || !studentId) {
      setNotice({ type: "warning", message: "Vui lòng đăng nhập bằng tài khoản Sinh viên để đăng ký." });
      setTimeout(() => setNotice(null), 3000);
      return;
    }
    setLoadingId(topicId);
    try {
      await registrationApi.registerTopic(studentId, topicId);
      // Reload để cập nhật trạng thái đăng ký theo bộ lọc hiện tại
      await load();
      setNotice({ type: "success", message: "Đăng ký thành công!" });
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Có lỗi khi đăng ký. Vui lòng thử lại." });
      setTimeout(() => setNotice(null), 3000);
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="container py-4">
      <h3>Danh sách đề tài</h3>
      {notice && (
        <InlineNotice
          type={notice.type}
          message={notice.message}
          onClose={() => setNotice(null)}
        />
      )}
      {/* Bộ lọc và tìm kiếm */}
      <div className="row mt-3 g-2 align-items-end">
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
      {!user && (
        <p className="text-muted mt-2">Bạn chưa đăng nhập. Vui lòng vào trang Đăng nhập để đăng ký đề tài.</p>
      )}
      {loading && (
        <p className="text-muted mt-3">Đang tải danh sách đề tài...</p>
      )}
      {error && !loading && (
        <p className="text-danger mt-3">{error}</p>
      )}
      {!loading && !error && topics.length === 0 && (
        <p className="text-muted mt-3">Chưa có đề tài nào.</p>
      )}

      {!loading && !error && topics.length > 0 && (
        <>
          <div className="row mt-3 g-3">
            {topics.map((t) => (
              <div key={t.id} className="col-md-4">
                <TopicCard
                  topic={t}
                  loading={loadingId === t.id}
                  onRegister={handleRegister}
                />
              </div>
            ))}
          </div>

          {/* Phân trang */}
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
        </>
      )}
    </div>
  );
}
