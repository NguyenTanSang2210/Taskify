import React, { useEffect, useState, useCallback } from "react";
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

  const load = useCallback(async () => {
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
          setNotice({ type: "info", message: "Bạn có thông báo mới!" });
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

  const markAllAsRead = async () => {
    try {
      await notificationsApi.markAllRead(userId);
      setItems((prev) => prev.map((x) => ({ ...x, readAt: new Date().toISOString() })));
      setNotice({ type: "success", message: "Tất cả thông báo đã được đánh dấu là đã đọc." });
    } catch (e) {
      console.error(e);
    }
  };

  const getNotificationIcon = (type) => {
     if (type === "REMINDER") return { icon: "alarm", color: "text-amber-500", bg: "bg-amber-500/10" };
     if (type === "ALERT") return { icon: "warning", color: "text-error", bg: "bg-error/10" };
     return { icon: "info", color: "text-primary", bg: "bg-primary/10" };
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header Area */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 shrink-0">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Trung tâm điều phối</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Thông báo</h1>
          <p className="text-sm text-outline mt-2 font-medium">Theo dõi các cập nhật quan trọng về đề án, lịch học thuật và hoạt động nhóm.</p>
        </div>
        
        <div className="flex items-center gap-3">
           <button 
             onClick={markAllAsRead} 
             disabled={loading || items.length === 0}
             className="px-6 py-3 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all disabled:opacity-50"
           >
              Đánh dấu tất cả đã đọc
           </button>
           <button onClick={load} className="p-2.5 bg-white border border-outline-variant/20 rounded-xl hover:bg-surface-container transition-all">
              <span className="material-symbols-outlined">refresh</span>
           </button>
        </div>
      </div>

      {notice && (
        <div className="shrink-0 w-full">
          <InlineNotice
            type={notice.type}
            message={notice.message}
            onClose={() => setNotice(null)}
            autoHideMs={3000}
          />
        </div>
      )}

      {/* Notifications List Area */}
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px] flex flex-col">
         {loading ? (
           <div className="flex-grow flex flex-col items-center justify-center p-20 opacity-30 select-none">
              <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent mb-4"></div>
              <p className="text-[10px] font-black uppercase tracking-widest text-outline">Đang tải nhật ký...</p>
           </div>
         ) : items.length === 0 ? (
           <div className="flex-grow flex flex-col items-center justify-center p-32 text-center opacity-30 select-none">
              <span className="material-symbols-outlined text-7xl mb-6">notifications_off</span>
              <h3 className="text-xl font-black uppercase tracking-[0.2em] text-outline">Hộp thư trống</h3>
              <p className="text-[10px] font-bold text-outline mt-2 leading-relaxed uppercase tracking-widest max-w-[280px]">Hiện tại bạn không có thông báo học thuật nào mới.</p>
           </div>
         ) : (
           <div className="divide-y divide-outline-variant/5">
              {items.map((n) => {
                 const { icon, color, bg } = getNotificationIcon(n.type);
                 const isRead = !!n.readAt;
                 return (
                    <div 
                       key={n.id} 
                       className={`p-8 transition-all duration-300 group hover:bg-primary/[0.01] flex items-start gap-6 relative ${!isRead ? 'bg-primary/[0.02]' : ''}`}
                    >
                       {!isRead && <div className="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>}
                       
                       <div className={`w-14 h-14 rounded-2xl flex items-center justify-center shrink-0 shadow-sm ${bg} ${color}`}>
                          <span className="material-symbols-outlined text-2xl">{icon}</span>
                       </div>

                       <div className="flex-grow space-y-2">
                          <div className="flex items-center justify-between gap-4">
                             <div className="flex items-center gap-3">
                                <span className={`text-[9px] font-black uppercase tracking-widest px-2.5 py-1 rounded-md ${bg} ${color}`}>
                                   {n.type}
                                </span>
                                <span className="text-[10px] font-bold text-outline uppercase tracking-tight">
                                   {new Date(n.createdAt).toLocaleDateString('vi-VN')} tại {new Date(n.createdAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'})}
                                </span>
                             </div>
                             {!isRead && <div className="w-2.5 h-2.5 rounded-full bg-primary animate-pulse"></div>}
                          </div>

                          <div>
                             <h3 className={`text-base font-black uppercase tracking-tight leading-snug group-hover:text-primary transition-colors ${!isRead ? 'text-on-surface' : 'text-on-surface-variant/70'}`}>
                                {n.title}
                             </h3>
                             <p className="text-[13px] font-medium text-outline leading-relaxed mt-1.5 line-clamp-3">
                                {n.content || "Không có nội dung mô tả chi tiết cho thông báo này."}
                             </p>
                          </div>

                          <div className="pt-4 flex items-center gap-4">
                            {!isRead && (
                               <button 
                                 onClick={() => markRead(n.id)}
                                 className="px-5 py-2.5 bg-primary text-white rounded-xl font-black text-[9px] uppercase tracking-widest shadow-lg shadow-primary/10 hover:scale-105 active:scale-95 transition-all flex items-center gap-2"
                               >
                                  Đánh dấu đã đọc
                               </button>
                            )}
                            <button 
                              onClick={() => n.linkPath && window.location.assign(n.linkPath)}
                              className="px-5 py-2.5 bg-surface-container text-on-surface-variant rounded-xl font-black text-[9px] uppercase tracking-widest hover:bg-surface-container-high transition-all border border-outline-variant/10 flex items-center gap-2"
                            >
                               Xem chi tiết <span className="material-symbols-outlined text-sm">arrow_forward</span>
                            </button>
                          </div>
                       </div>
                    </div>
                 );
              })}
           </div>
         )}
      </div>

      {/* Summary Footer Tip */}
      {!loading && items.length > 0 && (
         <div className="bg-secondary/5 p-6 rounded-[2rem] border border-secondary/10 flex items-center gap-4">
            <div className="w-10 h-10 rounded-xl bg-secondary text-white flex items-center justify-center shrink-0">
               <span className="material-symbols-outlined">lightbulb</span>
            </div>
            <div>
               <p className="text-[10px] font-black uppercase tracking-[0.1em] text-secondary">Hệ thống thông minh</p>
               <p className="text-xs font-bold text-on-surface-variant mt-0.5 leading-relaxed">Thông báo giúp bạn không bỏ lỡ các phê duyệt quan trọng từ giảng viên và các mốc thời gian báo cáo tiến độ.</p>
            </div>
         </div>
      )}
    </div>
  );
}
