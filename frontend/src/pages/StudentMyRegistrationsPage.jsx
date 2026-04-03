import React, { useEffect, useState, useCallback } from "react";
import { registrationApi } from "../api/registrationApi";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";
import { Link } from "react-router-dom";

export default function StudentMyRegistrationsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const { user } = useAuth();
  const roleName = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isStudent = roleName === "STUDENT";
  const studentId = isStudent ? user?.id : null;
  const [notice, setNotice] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      if (!isStudent || !studentId) {
        setItems([]);
        return;
      }
      const data = await registrationApi.getMine(studentId);
      setItems(data || []);
    } catch (e) {
      console.error(e);
      setError("Không thể tải danh sách đăng ký đề tài của bạn.");
    } finally {
      setLoading(false);
    }
  }, [isStudent, studentId]);

  useEffect(() => {
    load();
  }, [load]);

  const handleCancel = async (regId) => {
    if (!window.confirm("Bạn có chắc muốn hủy đăng ký này? Thao tác này không thể hoàn tác.")) return;
    try {
      await registrationApi.cancel(regId);
      await load();
      setNotice({ type: "success", message: "Đã hủy đăng ký thành công." });
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Không thể hủy đăng ký (Yêu cầu có thể đã được phê duyệt)." });
    }
  };

  const getStatusBadge = (approved) => {
    if (approved === null) return { 
        label: "Chờ phê duyệt", 
        style: "bg-surface-container text-outline",
        icon: "hourglass_empty"
    };
    if (approved === true) return { 
        label: "Đã phê duyệt", 
        style: "bg-primary-container text-on-primary-container",
        icon: "verified"
    };
    return { 
        label: "Bị từ chối", 
        style: "bg-error-container text-on-error-container",
        icon: "cancel"
    };
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header Area */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-secondary mb-2 block">Dành cho sinh viên</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Đăng ký của tôi</h1>
          <p className="text-sm text-outline mt-2 font-medium">Theo dõi trạng thái các đề tài bạn đã gửi yêu cầu đăng ký.</p>
        </div>
        
        <div className="flex items-center gap-3">
           <div className="px-4 py-2 bg-surface-container text-on-surface-variant rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center gap-2 border border-outline-variant/10">
              <span className="material-symbols-outlined text-sm">history</span>
              Tổng số: {items.length}
           </div>
           <button onClick={load} className="p-2.5 bg-white border border-outline-variant/20 rounded-xl hover:bg-surface-container transition-all">
              <span className="material-symbols-outlined">refresh</span>
           </button>
        </div>
      </div>

      {notice && (
        <div className="max-w-2xl mx-auto">
          <InlineNotice
            type={notice.type}
            message={notice.message}
            onClose={() => setNotice(null)}
            autoHideMs={3000}
          />
        </div>
      )}

      {/* Main Table Layer */}
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[400px] flex flex-col">
        {!user ? (
            <div className="flex-grow flex flex-col items-center justify-center p-20 text-center space-y-4">
               <span className="material-symbols-outlined text-6xl text-outline/20">lock_open</span>
               <p className="text-sm font-black uppercase tracking-widest text-outline">Vui lòng đăng nhập để xem thông tin đăng ký</p>
               <Link to="/login" className="px-8 py-3 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20">Đến trang đăng nhập</Link>
            </div>
        ) : loading ? (
            <div className="flex-grow flex items-center justify-center p-20 opacity-40">
               <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent"></div>
            </div>
        ) : error ? (
            <div className="flex-grow flex flex-col items-center justify-center p-20 text-center">
               <span className="material-symbols-outlined text-6xl text-error/30 mb-4">error</span>
               <p className="text-sm font-black uppercase tracking-widest text-error">{error}</p>
               <button onClick={load} className="mt-4 text-xs font-black uppercase tracking-widest text-primary hover:underline">Thử lại</button>
            </div>
        ) : items.length === 0 ? (
            <div className="flex-grow flex flex-col items-center justify-center p-24 text-center opacity-40">
               <span className="material-symbols-outlined text-7xl mb-4">tab_unselected</span>
               <h3 className="text-xl font-black uppercase tracking-widest">Chưa có đăng ký nào</h3>
               <p className="text-xs font-bold mt-2 uppercase tracking-tight">Cần chọn đề tài nghiên cứu để bắt đầu gửi yêu cầu.</p>
               <Link to="/" className="mt-6 text-[10px] font-black text-primary uppercase tracking-widest hover:underline">Khám phá đề tài ngay →</Link>
            </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
               <thead>
                  <tr className="bg-surface-container-low/30">
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Thông tin đề tài</th>
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Giảng viên hướng dẫn</th>
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Thời gian gửi</th>
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Trạng thái xử lý</th>
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Thao tác</th>
                  </tr>
               </thead>
               <tbody className="divide-y divide-outline-variant/10">
                  {items.map((r) => {
                     const status = getStatusBadge(r.approved);
                     return (
                        <tr key={r.id} className="group hover:bg-primary/[0.01] transition-colors">
                           <td className="px-8 py-6 max-w-sm">
                              <p className="text-sm font-black text-on-surface uppercase tracking-tight leading-snug group-hover:text-primary transition-colors">{r.topic?.title}</p>
                              <p className="text-[10px] text-outline mt-1 font-bold">MÃ ĐĂNG KÝ: #{r.id}</p>
                           </td>
                           <td className="px-8 py-6">
                              <div className="flex items-center gap-3">
                                 <div className="w-8 h-8 rounded-lg bg-secondary/10 text-secondary flex items-center justify-center font-black text-xs">
                                    {r.topic?.lecturer?.fullName?.charAt(0)}
                                 </div>
                                 <span className="text-[11px] font-black uppercase tracking-widest text-on-surface-variant truncate">{r.topic?.lecturer?.fullName}</span>
                              </div>
                           </td>
                           <td className="px-8 py-6">
                              <p className="text-xs font-black text-on-surface">{new Date(r.registeredAt).toLocaleDateString('vi-VN')}</p>
                              <p className="text-[10px] font-bold text-outline mt-0.5 uppercase">{new Date(r.registeredAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'})}</p>
                           </td>
                           <td className="px-8 py-6">
                              <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full ${status.style}`}>
                                 <span className="material-symbols-outlined text-sm">{status.icon}</span>
                                 <span className="text-[10px] font-black uppercase tracking-widest">{status.label}</span>
                              </div>
                              {r.approved === false && r.rejectReason && (
                                 <div className="mt-2 p-3 bg-error/5 rounded-xl border border-error/10 max-w-[200px]">
                                    <p className="text-[10px] font-bold text-error leading-relaxed">
                                       <span className="font-black">LÝ DO:</span> {r.rejectReason}
                                    </p>
                                 </div>
                              )}
                           </td>
                           <td className="px-8 py-6 text-right">
                              {r.approved === null && (
                                 <button 
                                    onClick={() => handleCancel(r.id)} 
                                    className="px-4 py-2 bg-error/5 text-error rounded-xl font-black text-[9px] uppercase tracking-[0.1em] border border-error/10 hover:bg-error hover:text-white transition-all active:scale-95"
                                 >
                                    Hủy đăng ký
                                 </button>
                              )}
                              {r.approved === true && (
                                 <Link 
                                    to="/progress" 
                                    className="px-4 py-2 bg-primary/10 text-primary rounded-xl font-black text-[9px] uppercase tracking-[0.1em] border border-primary/20 hover:bg-primary hover:text-white transition-all active:scale-95"
                                 >
                                    Xem tiến độ
                                 </Link>
                              )}
                           </td>
                        </tr>
                     );
                  })}
               </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Decorative Tips */}
      {!loading && items.length > 0 && (
         <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-primary/5 p-6 rounded-[2rem] border border-primary/10 flex items-center gap-4">
               <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shrink-0">
                  <span className="material-symbols-outlined">help_center</span>
               </div>
               <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.1em] text-primary">Thông tin hữu ích</p>
                  <p className="text-xs font-bold text-on-surface-variant mt-0.5 leading-relaxed">Nếu yêu cầu bị từ chối, bạn có thể đăng ký đề tài khác ngay lập tức.</p>
               </div>
            </div>
            <div className="bg-secondary/5 p-6 rounded-[2rem] border border-secondary/10 flex items-center gap-4">
               <div className="w-10 h-10 rounded-xl bg-secondary text-white flex items-center justify-center shrink-0">
                  <span className="material-symbols-outlined">notifications_active</span>
               </div>
               <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.1em] text-secondary">Hệ thống thông báo</p>
                  <p className="text-xs font-bold text-on-surface-variant mt-0.5 leading-relaxed">Chúng tôi sẽ gửi thông báo qua chuông khi giảng viên phê duyệt yêu cầu của bạn.</p>
               </div>
            </div>
         </div>
      )}
    </div>
  );
}
