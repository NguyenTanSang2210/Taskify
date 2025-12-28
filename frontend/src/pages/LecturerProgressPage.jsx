import React, { useEffect, useState, useCallback, useMemo } from "react";
import { topicApi } from "../api/topicApi";
import { progressApi } from "../api/progressApi";
import { connectAndSubscribe } from "../ws/stomp";
import ProgressRow from "../components/ProgressRow";
import { useAuth } from "../context/AuthContext";

export default function LecturerProgressPage() {
  const { user } = useAuth();

  const [topicId, setTopicId] = useState("");
  const [topics, setTopics] = useState([]);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loadingId, setLoadingId] = useState(null);

  // --- LOAD TOPICS ---
  const loadTopics = useCallback(async () => {
    if (!user?.id) return;
    try {
      const data = await topicApi.getAllTopics({ lecturerId: user.id, size: 100 });
      setTopics(data.items || []);
      if (data.items?.length > 0 && !topicId) {
        setTopicId(String(data.items[0].id));
      }
    } catch (e) {
      console.error(e);
    }
  }, [user?.id, topicId]);

  useEffect(() => {
    loadTopics();
  }, [loadTopics]);

  // --- LOAD PROGRESS ---
  const loadProgress = useCallback(async () => {
    if (!topicId) {
      setItems([]);
      return;
    }
    setLoading(true);
    try {
      const arr = await progressApi.listByTopic(topicId);
      setItems(arr || []);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không tải được dữ liệu tiến độ" });
    } finally {
      setLoading(false);
    }
  }, [topicId]);

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
          setNotice({ type: "info", message: "Có báo cáo mới từ sinh viên" });
          setTimeout(() => setNotice(null), 3000);
        } else if (type === "PROGRESS_UPDATED") {
          setItems((prev) => prev.map((x) => (x.id === pr.id ? pr : x)));
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId]);

  // --- ACTIONS ---
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
      setNotice({ type: "danger", message: e.message || "Cập nhật thất bại" });
      setTimeout(() => setNotice(null), 2500);
      return false;
    } finally {
      setLoadingId(null);
    }
  };

  // --- FILTER ---
  const filteredItems = useMemo(() => {
    let arr = items;
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
    return arr;
  }, [items, search, statusFilter]);

  return (
    <div className="container-fluid py-4">
      <h2 className="mb-4">Nhận xét tiến độ</h2>

      {notice && (
        <div className={`alert alert-${notice.type} alert-dismissible fade show`} role="alert">
          {notice.message}
          <button type="button" className="btn-close" onClick={() => setNotice(null)}></button>
        </div>
      )}

      <div className="card mb-4 shadow-sm">
            <div className="card-body">
                <div className="row g-3">
                    <div className="col-md-4">
                        <label className="form-label fw-bold">Chọn đề tài</label>
                        <select className="form-select" value={topicId} onChange={e => setTopicId(e.target.value)}>
                            {topics.map(t => (
                                <option key={t.id} value={t.id}>{t.title}</option>
                            ))}
                            {topics.length === 0 && <option value="">Không có đề tài nào</option>}
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
    </div>
  );
}
