import React, { useEffect, useRef, useState, useCallback } from "react";
import { notificationsApi } from "../api/notificationsApi";
import { connectAndSubscribe } from "../ws/stomp";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function NotificationsBell() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const dropdownRef = useRef(null);
  const userId = user?.id;
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);

  const load = useCallback(async () => {
    if (!userId) return;
    try {
      const list = await notificationsApi.listMine(userId);
      setItems(list.slice(0, 6)); // Show top 6
      const cnt = await notificationsApi.countUnread(userId);
      setUnread(cnt);
    } catch { /* noop */ }
  }, [userId]);

  useEffect(() => {
    if (!userId) return;
    const client = connectAndSubscribe({
      destination: `/topic/notifications/${userId}`,
      onMessage: (payload) => {
        const n = payload?.notification;
        if (n) {
          setItems((prev) => [n, ...(prev || [])].slice(0, 6));
          setUnread((u) => u + 1);
        }
      },
    });
    return () => client?.deactivate?.();
  }, [userId]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleOpenItem = async (n) => {
    try {
      if (!n.readAt) {
        await notificationsApi.markRead(n.id);
        setUnread((u) => Math.max(0, u - 1));
        setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, readAt: new Date().toISOString() } : x)));
      }
    } catch { /* noop */ }
    if (n.linkPath) navigate(n.linkPath);
    else navigate("/notifications");
    setOpen(false);
  };

  const markAllRead = async (e) => {
    e.stopPropagation();
    try {
      await notificationsApi.markAllRead(userId);
      setUnread(0);
      setItems((prev) => prev.map((x) => ({ ...x, readAt: new Date().toISOString() })));
    } catch { /* noop */ }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all relative group focus:outline-none ${open ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'}`}
        onClick={() => {
          setOpen(!open);
          if (!open) load();
        }}
        title="Thông báo hệ thống"
      >
        <span className="material-symbols-outlined text-xl group-hover:scale-110 transition-transform">notifications</span>
        {unread > 0 && (
          <span className="absolute -top-1 -right-1 w-4 h-4 bg-error text-[8px] font-black text-white rounded-full flex items-center justify-center border-2 border-white dark:border-slate-900 shadow-sm animate-bounce">
             {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-4 w-[360px] md:w-[420px] bg-white dark:bg-slate-900 rounded-[2.5rem] shadow-2xl border border-outline-variant/10 z-[100] overflow-hidden animate-in fade-in slide-in-from-top-4 duration-300">
          {/* Dropdown Header */}
          <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30 backdrop-blur-xl">
             <div className="flex justify-between items-center">
                <div>
                   <h3 className="text-sm font-black text-on-surface uppercase tracking-tight">Hộp thư thông báo</h3>
                   <div className="flex items-center gap-1.5 mt-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
                      <p className="text-[10px] text-outline font-black uppercase tracking-widest">Bạn có {unread} tin mới</p>
                   </div>
                </div>
                <div className="flex items-center gap-3">
                   {unread > 0 && (
                     <button
                       className="p-2 text-primary hover:bg-primary/10 rounded-lg transition-all"
                       onClick={markAllRead}
                       title="Đánh dấu tất cả đã đọc"
                     >
                       <span className="material-symbols-outlined text-lg">done_all</span>
                     </button>
                   )}
                   <button 
                     onClick={() => { navigate("/notifications"); setOpen(false); }}
                     className="p-2 text-outline hover:bg-surface-container rounded-lg transition-all"
                     title="Mở toàn trang"
                   >
                     <span className="material-symbols-outlined text-lg">open_in_new</span>
                   </button>
                </div>
             </div>
          </div>

          {/* List Area */}
          <div className="max-h-[420px] overflow-y-auto no-scrollbar py-2 divide-y divide-outline-variant/5">
            {items.length === 0 ? (
              <div className="py-20 text-center opacity-30 select-none grayscale">
                 <div className="w-20 h-20 bg-surface-container rounded-full flex items-center justify-center mx-auto mb-6">
                    <span className="material-symbols-outlined text-4xl">notifications_off</span>
                 </div>
                 <h4 className="text-xs font-black uppercase tracking-widest">Nhật ký trống</h4>
                 <p className="text-[10px] font-bold mt-2 uppercase">Hệ thống chưa có cập nhật nào cho bạn.</p>
              </div>
            ) : (
              items.map((n) => {
                 const isRead = !!n.readAt;
                 const isReminder = n.type === "REMINDER";
                 return (
                    <div
                      key={n.id}
                      className={`flex items-start gap-4 px-8 py-5 hover:bg-primary/[0.02] cursor-pointer transition-all relative group ${!isRead ? "bg-primary/[0.01]" : ""}`}
                      onClick={() => handleOpenItem(n)}
                    >
                      {!isRead && <div className="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>}
                      
                      <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 shadow-sm transition-transform group-hover:scale-105 ${
                        isReminder ? "bg-amber-500/10 text-amber-600" : "bg-primary/10 text-primary"
                      }`}>
                        <span className="material-symbols-outlined text-xl">
                          {isReminder ? "alarm" : "info"}
                        </span>
                      </div>

                      <div className="flex-grow min-w-0">
                         <div className="flex justify-between items-start gap-2">
                           <h4 className={`text-xs leading-snug break-words uppercase tracking-tight ${!isRead ? "font-black text-on-surface" : "font-bold text-outline opacity-60"}`}>
                             {n.title}
                           </h4>
                           {!isRead && <div className="w-2 h-2 bg-primary rounded-full shrink-0 shadow-[0_0_8px_rgba(var(--primary-rgb),0.4)]"></div>}
                         </div>
                         <p className="text-[11px] font-medium text-outline/70 mt-1 line-clamp-1">{n.content}</p>
                         <div className="flex items-center gap-2 mt-2">
                            <span className={`text-[8px] font-black uppercase tracking-widest px-1.5 py-0.5 rounded-md ${isReminder ? "bg-amber-500/10 text-amber-600" : "bg-primary/10 text-primary"}`}>
                               {n.type}
                            </span>
                            <span className="text-[9px] text-outline/40 font-bold uppercase tracking-tighter">
                               {new Date(n.createdAt).toLocaleDateString()} • {new Date(n.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                            </span>
                         </div>
                      </div>
                    </div>
                 );
              })
            )}
          </div>

          {/* Footer Area */}
          <div className="bg-surface-container-low/50 p-6 border-t border-outline-variant/5">
             <Link 
                className="w-full flex items-center justify-center gap-3 py-3 bg-surface-container hover:bg-primary hover:text-white rounded-xl text-[10px] font-black uppercase tracking-widest transition-all duration-300 group" 
                to="/notifications" 
                onClick={() => setOpen(false)}
             >
                Xem chi tiết thông báo
                <span className="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">arrow_forward</span>
             </Link>
          </div>
        </div>
      )}
    </div>
  );
}
