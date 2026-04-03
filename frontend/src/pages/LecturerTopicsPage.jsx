import React, { useEffect, useState, useCallback, useMemo } from "react";
import { Link } from "react-router-dom";
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

  const getStatusInfo = (s) => {
    if (s === "OPEN") return { label: "Mở đăng ký", color: "text-primary", bg: "bg-primary/10", icon: "door_open" };
    if (s === "CLOSED") return { label: "Đã đóng", color: "text-outline", bg: "bg-surface-container", icon: "lock" };
    if (s === "REGISTERED") return { label: "Đã đăng ký", color: "text-secondary", bg: "bg-secondary/10", icon: "group" };
    return { label: s || "N/A", color: "text-outline", bg: "bg-surface-container", icon: "help" };
  };

  const load = useCallback(async (override = {}) => {
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
      setError("Không thể tải danh sách đề tài của bạn.");
    } finally {
      setLoading(false);
    }
  }, [lecturerId, query, status, page, size]);

  useEffect(() => {
    load();
  }, [load]);

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
      setNotice({ type: "warning", message: "Vui lòng nhập tiêu đề đề tài." });
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
      setNotice({ type: "success", message: editing ? "Đã lưu chỉnh sửa đề tài thành công!" : "Đã tạo đề tài mới thành công!" });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Lưu đề tài thất bại." });
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Bạn có chắc muốn xóa đề tài này?")) return;
    setActionLoadingId(id);
    try {
      await topicApi.remove(id, lecturerId);
      await load();
      setNotice({ type: "success", message: "Đã xóa đề tài thành công!" });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Xóa đề tài thất bại." });
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleOpen = async (id) => {
    setActionLoadingId(id);
    try {
      await topicApi.open(id, lecturerId);
      await load();
      setNotice({ type: "success", message: "Đã mở đăng ký đề tài thành công!" });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Mở đề tài thất bại." });
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleClose = async (id) => {
    const t = items.find((it) => it.id === id);
    if (t && (t.pendingCount ?? 0) > 0) {
      if (!window.confirm(`Đề tài hiện còn ${t.pendingCount} đăng ký đang CHỜ DUYỆT. Bạn có chắc muốn ĐÓNG đề tài?`)) return;
    }

    setActionLoadingId(id);
    try {
      await topicApi.close(id, lecturerId);
      await load();
      setNotice({ type: "success", message: "Đã đóng đề tài thành công!" });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Đóng đề tài thất bại." });
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header / Hero */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Quản lý nội dung</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Đề tài của tôi</h1>
          <p className="text-sm text-outline mt-2 font-medium">Tạo mới, chỉnh sửa và điều phối trạng thái các đề tài nghiên cứu bạn đang phụ trách.</p>
        </div>
        <button 
          onClick={openCreate}
          className="px-8 py-3.5 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all flex items-center gap-2 group"
        >
          <span className="material-symbols-outlined text-lg group-hover:rotate-90 transition-transform">add</span>
          Tạo đề tài mới
        </button>
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

      {/* Control Bar */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-[2rem] border border-outline-variant/10 shadow-sm">
         <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-end">
            <div className="md:col-span-5 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm đề tài</label>
               <div className="relative group">
                 <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
                 <input
                   className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                   placeholder="Nhập từ khóa hoặc tiêu đề..."
                   value={query}
                   onChange={(e) => {
                     setPage(0);
                     setQuery(e.target.value);
                   }}
                 />
               </div>
            </div>
            <div className="md:col-span-3 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Trạng thái hệ thống</label>
               <select
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none"
                 value={status}
                 onChange={(e) => {
                   setPage(0);
                   setStatus(e.target.value);
                 }}
               >
                 <option value="">Tất cả trạng thái</option>
                 <option value="OPEN">Đang mở đăng ký</option>
                 <option value="CLOSED">Đã đóng</option>
                 <option value="REGISTERED">Đã có sinh viên</option>
               </select>
            </div>
            <div className="md:col-span-2 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Hiển thị</label>
               <select
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none"
                 value={size}
                 onChange={(e) => {
                   setPage(0);
                   setSize(Number(e.target.value));
                 }}
               >
                 <option value={6}>6 mục</option>
                 <option value={9}>9 mục</option>
                 <option value={12}>12 mục</option>
               </select>
            </div>
            <div className="md:col-span-2 flex justify-end">
               <button onClick={() => load()} className="w-full p-2.5 bg-surface-container text-on-surface-variant rounded-xl border border-outline-variant/10 hover:bg-surface-container-high transition-all">
                  <span className="material-symbols-outlined text-2xl">refresh</span>
               </button>
            </div>
         </div>
      </div>

      {/* Main Table Content */}
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[400px] flex flex-col">
        {loading ? (
            <div className="flex-grow flex items-center justify-center p-20 opacity-40">
               <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary border-t-transparent"></div>
            </div>
        ) : error ? (
            <div className="flex-grow flex flex-col items-center justify-center p-20 text-center">
               <span className="material-symbols-outlined text-6xl text-error/30 mb-4">error</span>
               <p className="text-sm font-black uppercase tracking-widest text-error">{error}</p>
               <button onClick={() => load()} className="mt-4 text-xs font-black uppercase tracking-widest text-primary hover:underline">Thử lại</button>
            </div>
        ) : items.length === 0 ? (
            <div className="flex-grow flex flex-col items-center justify-center p-24 text-center opacity-40">
               <span className="material-symbols-outlined text-7xl mb-4">move_to_inbox</span>
               <h3 className="text-xl font-black uppercase tracking-widest">Chưa có đề tài nào</h3>
               <p className="text-xs font-bold mt-2">Bắt đầu bằng cách tạo đề tài nghiên cứu đầu tiên của bạn.</p>
            </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                 <thead>
                    <tr className="bg-surface-container-low/50">
                       <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Thông tin đề tài</th>
                       <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Trạng thái</th>
                       <th className="px-6 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center">Đăng ký</th>
                       <th className="px-6 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center">Chờ</th>
                       <th className="px-6 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center">Duyệt</th>
                       <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Thao tác</th>
                    </tr>
                 </thead>
                 <tbody className="divide-y divide-outline-variant/10">
                    {items.map((t) => {
                       const status = getStatusInfo(t.status);
                       return (
                         <tr key={t.id} className="group hover:bg-primary/[0.02] transition-colors">
                            <td className="px-8 py-6 max-w-sm">
                               <p className="text-sm font-black text-on-surface uppercase tracking-tight leading-snug group-hover:text-primary transition-colors">{t.title}</p>
                               <p className="text-[11px] text-outline mt-1.5 font-medium line-clamp-1">{t.description || "Không có mô tả chi tiết."}</p>
                            </td>
                            <td className="px-8 py-6">
                               <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full ${status.bg} ${status.color}`}>
                                  <span className="material-symbols-outlined text-sm">{status.icon}</span>
                                  <span className="text-[10px] font-black uppercase tracking-widest">{status.label}</span>
                               </div>
                            </td>
                            <td className="px-6 py-6 text-center">
                               <span className="text-sm font-black text-on-surface">{t.registrationCount ?? 0}</span>
                            </td>
                            <td className="px-6 py-6 text-center">
                               <span className={`text-sm font-black ${t.pendingCount > 0 ? "text-secondary" : "text-outline/30"}`}>{t.pendingCount ?? 0}</span>
                            </td>
                            <td className="px-6 py-6 text-center">
                               <span className={`text-sm font-black ${t.approvedCount > 0 ? "text-primary" : "text-outline/30"}`}>{t.approvedCount ?? 0}</span>
                            </td>
                            <td className="px-8 py-6 text-right">
                               <div className="flex justify-end gap-2">
                                  <Link 
                                    to={`/lecturer?topicId=${t.id}`}
                                    className="p-2.5 rounded-xl bg-surface-container hover:bg-secondary hover:text-white transition-all shadow-sm group/btn"
                                    title="Xem danh sách đăng ký"
                                  >
                                     <span className="material-symbols-outlined text-lg">group</span>
                                  </Link>
                                  <button 
                                    onClick={() => openEdit(t)}
                                    className="p-2.5 rounded-xl bg-surface-container hover:bg-primary hover:text-white transition-all shadow-sm"
                                    title="Chỉnh sửa đề tài"
                                  >
                                     <span className="material-symbols-outlined text-lg">edit</span>
                                  </button>
                                  {t.status !== "OPEN" ? (
                                    <button 
                                      onClick={() => handleOpen(t.id)} 
                                      disabled={actionLoadingId === t.id}
                                      className="p-2.5 rounded-xl bg-surface-container hover:bg-green-600 hover:text-white transition-all shadow-sm disabled:opacity-50"
                                      title="Mở đăng ký"
                                    >
                                       <span className="material-symbols-outlined text-lg">play_arrow</span>
                                    </button>
                                  ) : (
                                    <button 
                                      onClick={() => handleClose(t.id)} 
                                      disabled={actionLoadingId === t.id}
                                      className="p-2.5 rounded-xl bg-surface-container hover:bg-amber-600 hover:text-white transition-all shadow-sm disabled:opacity-50"
                                      title="Đóng đăng ký"
                                    >
                                       <span className="material-symbols-outlined text-lg">stop</span>
                                    </button>
                                  )}
                                  <button 
                                    onClick={() => handleDelete(t.id)} 
                                    disabled={actionLoadingId === t.id}
                                    className="p-2.5 rounded-xl bg-surface-container hover:bg-error hover:text-white transition-all shadow-sm disabled:opacity-50"
                                    title="Xóa đề tài"
                                  >
                                     <span className="material-symbols-outlined text-lg">delete</span>
                                  </button>
                               </div>
                            </td>
                         </tr>
                       );
                    })}
                 </tbody>
              </table>
            </div>

            {/* Pagination Layer */}
            <div className="mt-auto px-8 py-6 bg-surface-container-low/30 border-t border-outline-variant/10 flex flex-col md:flex-row justify-between items-center gap-4">
               <p className="text-[10px] font-black uppercase tracking-widest text-outline">
                  Hiển thị đề tài trang {page + 1} của {Math.max(1, totalPages)}
               </p>
               
               <div className="flex items-center gap-2">
                  <button
                    className={`p-2 rounded-xl transition-all ${
                      page <= 0 
                      ? "bg-surface-container-high/50 text-outline/30 cursor-not-allowed" 
                      : "bg-white border border-outline-variant/20 text-on-surface hover:bg-primary hover:text-white"
                    }`}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page <= 0}
                  >
                    <span className="material-symbols-outlined">chevron_left</span>
                  </button>

                  <div className="flex items-center gap-1 mx-2">
                     {[...Array(totalPages)].map((_, i) => (
                        <button 
                          key={i}
                          onClick={() => setPage(i)}
                          className={`w-8 h-8 rounded-lg text-[10px] font-black transition-all ${
                            page === i ? "bg-primary text-white" : "text-outline hover:bg-surface-container"
                          }`}
                        >
                          {i + 1}
                        </button>
                     ))}
                  </div>

                  <button
                    className={`p-2 rounded-xl transition-all ${
                      page + 1 >= totalPages
                      ? "bg-surface-container-high/50 text-outline/30 cursor-not-allowed" 
                      : "bg-white border border-outline-variant/20 text-on-surface hover:bg-primary hover:text-white"
                    }`}
                    onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
                    disabled={page + 1 >= totalPages}
                  >
                    <span className="material-symbols-outlined">chevron_right</span>
                  </button>
               </div>
            </div>
          </>
        )}
      </div>

      {/* Premium Form Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-in fade-in duration-300">
           <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={closeModal}></div>
           <div className="bg-white dark:bg-slate-900 w-full max-w-2xl rounded-[2.5rem] shadow-2xl z-10 overflow-hidden animate-in zoom-in-95 duration-300">
              {/* Modal Header */}
              <div className="px-10 py-8 border-b border-outline-variant/5 bg-surface-container-low/30 relative">
                 <div>
                    <span className="text-[10px] font-black uppercase tracking-widest text-primary mb-1 block">Xác định học hứa</span>
                    <h2 className="text-2xl font-black text-on-surface uppercase tracking-tight font-headline leading-none">
                       {editing ? "Cập nhật đề tài" : "Đề xuất đề tài mới"}
                    </h2>
                 </div>
                 <button onClick={closeModal} className="absolute top-8 right-10 p-2.5 hover:bg-surface-container-high rounded-2xl transition-colors">
                    <span className="material-symbols-outlined">close</span>
                 </button>
              </div>

              {/* Modal Body */}
              <div className="p-10 space-y-8">
                 <div className="space-y-2">
                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tiêu đề nghiên cứu</label>
                    <input
                      className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none"
                      value={form.title}
                      onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                      maxLength={255}
                      placeholder="VD: Ứng dụng AI trong phân tích dữ liệu rác thải nhựa..."
                    />
                 </div>
                 
                 <div className="space-y-2">
                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mô tả chi tiết & Yêu cầu</label>
                    <textarea
                      className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-bold transition-all duration-300 outline-none min-h-[180px]"
                      rows={6}
                      value={form.description}
                      onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                      placeholder="Mô tả mục tiêu, công nghệ sử dụng, và các yêu cầu đối với sinh viên..."
                    />
                 </div>

                 <div className="flex items-center gap-4 p-5 bg-primary/5 rounded-2xl border border-primary/10">
                    <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shrink-0">
                       <span className="material-symbols-outlined">lightbulb</span>
                    </div>
                    <div>
                       <p className="text-[10px] font-black uppercase tracking-[0.1em] text-primary">Mẹo học thuật</p>
                       <p className="text-xs font-bold text-on-surface-variant mt-0.5 leading-relaxed">Hãy nêu rõ những kỹ năng cần thiết (VD: React, Python) để sinh viên có sự chuẩn bị tốt nhất.</p>
                    </div>
                 </div>
              </div>

              {/* Modal Footer */}
              <div className="px-10 py-8 bg-surface-container-low/30 border-t border-outline-variant/5 flex justify-end gap-4">
                 <button onClick={closeModal} className="px-8 py-3.5 bg-white text-on-surface-variant font-black text-xs uppercase tracking-widest rounded-2xl hover:bg-surface-container transition-all">
                    Hủy bỏ
                 </button>
                 <button
                   onClick={handleSave}
                   className="px-10 py-3.5 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all"
                 >
                   {editing ? "Lưu thay đổi" : "Khởi tạo đề tài"}
                 </button>
              </div>
           </div>
        </div>
      )}
    </div>
  );
}
