import React, { useEffect, useRef, useState } from "react";
import { notificationsApi } from "../api/notificationsApi";
import { connectAndSubscribe } from "../ws/stomp";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function NotificationsBell() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const anchorRef = useRef(null);
  const userId = user?.id;
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const [menuPos, setMenuPos] = useState({ top: 0, left: 0 });

  const load = React.useCallback(async () => {
    if (!userId) return;
    try {
      const list = await notificationsApi.listMine(userId);
      setItems(list.slice(0, 5));
      const cnt = await notificationsApi.countUnread(userId);
      setUnread(cnt);
    } catch { /* noop */ }
  }, [userId]);

  // Tải khi người dùng mở dropdown để tránh cảnh báo hooks

  useEffect(() => {
    if (!userId) return;
    const client = connectAndSubscribe({
      destination: `/topic/notifications/${userId}`,
      onMessage: (payload) => {
        const n = payload?.notification;
        if (n) {
          setItems((prev = []) => [n, ...prev].slice(0, 5));
          setUnread((u) => u + 1);
        }
      },
    });
    return () => client?.deactivate?.();
  }, [userId]);

  const handleOpenItem = async (n) => {
    try {
      if (!n.readAt) {
        await notificationsApi.markRead(n.id);
        setUnread((u) => Math.max(0, u - 1));
        setItems((prev = []) => prev.map((x) => (x.id === n.id ? { ...x, readAt: new Date().toISOString() } : x)));
      }
    } catch { /* noop */ }
    if (n.linkPath) navigate(n.linkPath);
    else navigate("/notifications");
    setOpen(false);
  };

  return (
    <div ref={anchorRef}>
      <button
        className="btn btn-outline-light btn-sm"
        onClick={async () => {
          setOpen((o) => !o);
          try { await load(); } catch { /* noop */ }
          try {
            const asideRect = document.querySelector("aside")?.getBoundingClientRect();
            const left = Math.max(8, (asideRect?.right || 64) + 8);
            setMenuPos({ top: 0, left });
          } catch { /* noop */ }
        }}
        title="Thông báo"
      >
        🔔 {unread > 0 ? <span className="badge bg-danger">{unread}</span> : null}
      </button>
      {open && (
        <div
          className="dropdown-menu show p-2"
          style={{
            position: "fixed",
            minWidth: 320,
            bottom: 12,
            left: menuPos.left,
            zIndex: 2000,
            maxHeight: 300,
            overflowY: "auto"
          }}
        >
          <div className="d-flex justify-content-between align-items-center mb-2">
            <strong>Thông báo mới</strong>
            <div>
              {unread > 0 && (
                <button
                  className="btn btn-link btn-sm text-decoration-none p-0 me-2"
                  onClick={async (e) => {
                    e.stopPropagation();
                    try {
                      await notificationsApi.markAllRead(userId);
                      setUnread(0);
                      setItems((prev) => prev.map((x) => ({ ...x, readAt: new Date().toISOString() })));
                    } catch { /* noop */ }
                  }}
                >
                  Đọc hết
                </button>
              )}
              <Link className="small" to="/notifications" onClick={() => setOpen(false)}>Xem tất cả</Link>
            </div>
          </div>
          {items.length === 0 ? (
            <div className="text-muted small">Chưa có thông báo</div>
          ) : (
            items.map((n) => (
              <div
                key={n.id}
                className="d-flex align-items-start mb-2 p-2 rounded"
                style={{ background: n.readAt ? "#f8f9fa" : "#e7f1ff", cursor: "pointer" }}
                onClick={() => handleOpenItem(n)}
              >
                <span className={`badge me-2 ${n.type === "REMINDER" ? "bg-warning text-dark" : "bg-info"}`}>{n.type}</span>
                <div className="flex-grow-1">
                  <div className="small fw-bold">{n.title}</div>
                  <div className="small text-muted">{new Date(n.createdAt).toLocaleString()}</div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
