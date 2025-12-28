import React, { useEffect, useState } from "react";
import { notificationsApi } from "../api/notificationsApi";
import { connectAndSubscribe } from "../ws/stomp";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";

export default function NotificationsPage() {
  const { user } = useAuth();
  const userId = user?.id;

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const load = React.useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    try {
      const data = await notificationsApi.listMine(userId);
      setItems(data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!userId) return;
    const client = connectAndSubscribe({
      destination: `/topic/notifications/${userId}`,
      onMessage: async (payload) => {
        const n = payload?.notification;
        if (n) {
          setItems((prev = []) => [n, ...prev]);
          setNotice({ type: "info", message: "Có thông báo mới" });
          setTimeout(() => setNotice(null), 2000);
        }
      },
    });
    return () => client?.deactivate?.();
  }, [userId]);

  const markRead = async (id) => {
    try {
      const updated = await notificationsApi.markRead(id);
      setItems((prev = []) => {
        const idx = prev.findIndex((x) => x.id === id);
        if (idx >= 0) {
          const arr = [...prev];
          arr[idx] = updated;
          return arr;
        }
        return prev;
      });
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center">
        <h3>Thông báo</h3>
        <button
          className="btn btn-outline-secondary btn-sm"
          onClick={async () => {
            try {
              await notificationsApi.markAllRead(userId);
              setItems((prev) => prev.map((x) => ({ ...x, readAt: new Date().toISOString() })));
              setNotice({ type: "success", message: "Đã đánh dấu tất cả là đã đọc" });
              setTimeout(() => setNotice(null), 2500);
            } catch (e) { console.error(e); }
          }}
        >
          Đánh dấu tất cả đã đọc
        </button>
      </div>
      {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} />}
      {loading ? (
        <p className="text-muted mt-3">Đang tải...</p>
      ) : items.length === 0 ? (
        <p className="text-muted mt-3">Chưa có thông báo nào.</p>
      ) : (
        <div className="table-responsive mt-3">
          <table className="table table-hover">
            <thead className="table-light">
              <tr>
                <th style={{ width: "20%" }}>Thời gian</th>
                <th style={{ width: "16%" }}>Loại</th>
                <th style={{ width: "24%" }}>Tiêu đề</th>
                <th>Nội dung</th>
                <th style={{ width: "12%" }}>Trạng thái</th>
                <th style={{ width: "14%" }}></th>
              </tr>
            </thead>
            <tbody>
              {items.map((n) => (
                <tr key={n.id}>
                  <td>{new Date(n.createdAt).toLocaleString()}</td>
                  <td><span className={`badge ${n.type === "REMINDER" ? "bg-warning text-dark" : "bg-info"}`}>{n.type}</span></td>
                  <td>{n.title}</td>
                  <td className="small">{n.content || "-"}</td>
                  <td>{n.readAt ? <span className="badge bg-secondary">Đã đọc</span> : <span className="badge bg-primary">Chưa đọc</span>}</td>
                  <td className="text-end">
                    {!n.readAt && (
                      <button className="btn btn-outline-primary btn-sm" onClick={() => markRead(n.id)}>
                        Đánh dấu đã đọc
                      </button>
                    )}
                    <button
                      className="btn btn-outline-info btn-sm ms-2"
                      onClick={() => {
                        if (n.linkPath) window.location.assign(n.linkPath);
                      }}
                    >
                      Xem chi tiết
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
