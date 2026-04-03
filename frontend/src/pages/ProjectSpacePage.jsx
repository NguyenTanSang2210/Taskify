import React, { useEffect, useState, useCallback, useMemo } from "react";
import { registrationApi } from "../api/registrationApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { topicApi } from "../api/topicApi";
import { connectAndSubscribe } from "../ws/stomp";
import ProgressRow from "../components/ProgressRow";
import InlineNotice from "../components/InlineNotice";
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
      setNotice({ type: "danger", message: "Không thể tải dữ liệu tiến độ" });
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
            setNotice({ type: "success", message: "Có báo cáo mới từ sinh viên!" });
          }
        } else if (type === "PROGRESS_UPDATED") {
          setItems((prev) => prev.map((x) => (x.id === pr.id ? pr : x)));
          if (isStudent) {
            setNotice({ type: "info", message: "Giảng viên đã cập nhật phản hồi báo cáo." });
          }
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId, isStudent, isLecturer]);

  const handleStudentSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setNotice({ type: "warning", message: "Vui lòng nhập tiêu đề báo cáo." });
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
      setNotice({ type: "success", message: "Gửi báo cáo tiến độ thành công!" });
      setTitle("");
      setContent("");
      setFile(null);
      loadProgress();
    } catch (err) {
      console.error(err);
      setNotice({ type: "danger", message: "Gửi báo cáo thất bại." });
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
      setNotice({ type: "success", message: "Đã cập nhật nhận xét thành công!" });
      return true;
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Cập nhật thất bại" });
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

  if (!role) return (
     <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-8 bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm animate-in fade-in">
        <span className="material-symbols-outlined text-outline text-6xl mb-4">lock</span>
        <h2 className="text-2xl font-black text-on-surface uppercase tracking-widest">Truy cập bị hạn chế</h2>
        <p className="text-outline font-bold mt-2">Vui lòng đăng nhập để vào Không gian làm việc.</p>
        <button className="mt-8 px-8 py-3 bg-primary text-white rounded-xl font-black uppercase tracking-widest shadow-xl shadow-primary/20">Đến trang đăng nhập</button>
     </div>
  );

  const selectedLecturerTopic = lecturerTopics.find((t) => String(t.id) === String(topicId));
  const studentCanSubmit = isStudent && !!topicId;
  const recentCount = filteredItems.length;

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header Area */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Collaborative Workspace</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Không gian làm việc</h1>
          <p className="text-sm text-outline mt-2 font-medium max-w-2xl">
            {isStudent
              ? "Gửi báo cáo tiến độ định kỳ, nhận phản hồi trực tiếp từ giảng viên và quản lý tài liệu nghiên cứu."
              : "Theo dõi nhịp độ làm việc của sinh viên, xem các báo cáo mới nhất và thực hiện đánh giá chuyên môn."}
          </p>
        </div>
        
        <div className="flex items-center gap-3">
           <div className={`px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center gap-2 border ${isStudent ? 'bg-primary-container text-on-primary-container border-primary/10' : 'bg-secondary-container text-on-secondary-container border-secondary/10'}`}>
              <span className="material-symbols-outlined text-sm">{isStudent ? 'person' : 'school'}</span>
              VAI TRÒ: {isStudent ? "SINH VIÊN" : "GIẢNG VIÊN"}
           </div>
           <button onClick={loadProgress} className="p-2.5 bg-white border border-outline-variant/20 rounded-xl hover:bg-surface-container transition-all">
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

      {/* Role-Specific Control / Info Layer */}
      {isLecturer && (
        <div className="bg-white dark:bg-slate-900 p-6 rounded-[2rem] border border-outline-variant/10 shadow-sm">
           <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-end">
              <div className="lg:col-span-4 space-y-1.5">
                 <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn đề tài nghiên cứu</label>
                 <select 
                   className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-black transition-all duration-300 outline-none cursor-pointer" 
                   value={topicId} 
                   onChange={(e) => setTopicId(e.target.value)}
                 >
                    {lecturerTopics.map((t) => (
                      <option key={t.id} value={t.id}>{t.title}</option>
                    ))}
                    {lecturerTopics.length === 0 && <option value="">Chưa có đề tài nào</option>}
                 </select>
              </div>
              <div className="lg:col-span-1 hidden lg:block text-center text-outline/30">
                 <span className="material-symbols-outlined">arrow_forward</span>
              </div>
              <div className="lg:col-span-4 space-y-1.5">
                 <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm nội dung / Sinh viên</label>
                 <div className="relative group">
                   <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
                   <input
                     className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                     placeholder="Tiêu đề, tên SV, MSSV..."
                     value={search}
                     onChange={(e) => setSearch(e.target.value)}
                   />
                 </div>
              </div>
              <div className="lg:col-span-3 space-y-1.5">
                 <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Phân loại trạng thái</label>
                 <select 
                    className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none" 
                    value={statusFilter} 
                    onChange={(e) => setStatusFilter(e.target.value)}
                 >
                    <option value="">Tất cả</option>
                    <option value="TODO">Việc cần làm (TODO)</option>
                    <option value="IN_PROGRESS">Đang thực hiện</option>
                    <option value="DONE">Đã hoàn thành</option>
                 </select>
              </div>
           </div>
        </div>
      )}

      {/* Main Workspace Grid */}
      <div className={isLecturer ? "flex flex-col space-y-8" : "grid grid-cols-1 lg:grid-cols-12 gap-8"}>
        
        {/* Student Column: Form Input */}
        {isStudent && (
          <aside className="lg:col-span-4 space-y-8 animate-in slide-in-from-left-4 duration-500">
             <div className="bg-white dark:bg-slate-900 p-8 rounded-[2.5rem] border border-outline-variant/10 shadow-sm">
                <div className="flex items-center gap-4 mb-8">
                   <div className="w-12 h-12 rounded-2xl bg-primary/5 text-primary flex items-center justify-center">
                      <span className="material-symbols-outlined text-2xl">post_add</span>
                   </div>
                   <div>
                      <h2 className="text-sm font-black text-on-surface uppercase tracking-widest">Gửi báo cáo</h2>
                      <p className="text-[10px] text-outline font-bold mt-0.5 uppercase tracking-tight">Cập nhật tiến trình mới</p>
                   </div>
                </div>

                {!studentCanSubmit ? (
                  <div className="text-center py-10 space-y-4 opacity-40">
                     <span className="material-symbols-outlined text-5xl">lock</span>
                     <p className="text-xs font-bold text-outline leading-relaxed uppercase tracking-[0.2em]">Cần có đề tài được phê duyệt để bắt đầu nộp báo cáo.</p>
                  </div>
                ) : (
                  <form onSubmit={handleStudentSubmit} className="space-y-6">
                    <div className="space-y-1.5">
                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tiêu đề báo cáo</label>
                       <input
                         className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                         value={title}
                         onChange={(e) => setTitle(e.target.value)}
                         placeholder="VD: Báo cáo tuần 1: Xây dựng Database"
                         required
                       />
                    </div>
                    <div className="space-y-1.5">
                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Nội dung thực hiện</label>
                       <textarea
                         className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[160px]"
                         rows={5}
                         value={content}
                         onChange={(e) => setContent(e.target.value)}
                         placeholder="Mô tả các đầu việc đã hoàn thành và khó khăn gặp phải..."
                       />
                    </div>
                    <div className="space-y-1.5">
                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tài liệu đính kèm</label>
                       <div className="relative group">
                          <input type="file" className="absolute inset-0 opacity-0 cursor-pointer z-10" onChange={(e) => setFile(e.target.files[0])} />
                          <div className="w-full px-4 py-4 border-2 border-dashed border-outline-variant/30 group-hover:border-primary group-hover:bg-primary/[0.02] rounded-xl flex items-center justify-center gap-3 transition-all">
                             <span className="material-symbols-outlined text-outline group-hover:text-primary">upload_file</span>
                             <span className="text-[10px] font-black uppercase tracking-widest text-outline group-hover:text-primary truncate">
                                {file ? file.name : "Tải tệp tin hỗ trợ báo cáo"}
                             </span>
                          </div>
                       </div>
                    </div>
                    <button 
                       className="w-full py-4 bg-primary text-white rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50" 
                       type="submit" 
                       disabled={submitting}
                    >
                      {submitting ? "Đang xử lý..." : "Xác nhận gửi báo cáo"}
                    </button>
                  </form>
                )}
             </div>

             <div className="bg-secondary-container/20 p-6 rounded-[2rem] border border-secondary/10">
                <div className="flex items-center gap-3 mb-2">
                   <span className="material-symbols-outlined text-secondary">verified_user</span>
                   <p className="text-[10px] font-black uppercase tracking-widest text-secondary">Đề tài hiện tại</p>
                </div>
                <p className="text-xs font-bold text-on-secondary-container leading-relaxed">
                   {selectedLecturerTopic ? selectedLecturerTopic.title : "Đang tải tên đề tài..."}
                </p>
             </div>
          </aside>
        )}

        {/* History / Review Column */}
        <div className={isStudent ? "lg:col-span-8 animate-in slide-in-from-right-4 duration-500" : "w-full animate-in slide-in-from-bottom-4 duration-500"}>
           <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px] flex flex-col">
              <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30 flex justify-between items-center">
                 <div>
                    <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Lịch sử hoạt động</h2>
                    <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Danh sách các báo cáo tiến độ và phản hồi</p>
                 </div>
                 <div className="flex items-center gap-2">
                    <span className="text-xs font-black text-outline mr-2">{recentCount} MỤC</span>
                    <div className="w-10 h-10 rounded-xl bg-surface-container flex items-center justify-center">
                       <span className="material-symbols-outlined text-outline">view_agenda</span>
                    </div>
                 </div>
              </div>

              <div className="overflow-x-auto flex-grow">
                 <table className="w-full text-left border-collapse">
                    <thead>
                       <tr className="bg-surface-container-low/10">
                          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: isLecturer ? "15%" : "20%" }}>Mốc thời gian</th>
                          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: isLecturer ? "20%" : "30%" }}>{isLecturer ? "Chi tiết sinh viên" : "Tiêu đề báo cáo"}</th>
                          {isLecturer ? <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Nội dung báo cáo</th> : <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center" style={{ width: "15%" }}>Trạng thái</th>}
                          {isLecturer ? <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "15%" }}>Tiến độ / Hạn</th> : <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "25%" }}>Nhận xét từ GV</th>}
                          {isLecturer && <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Phản hồi chuyên môn</th>}
                          {isLecturer && <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right" style={{ width: "10%" }}>Xác nhận</th>}
                          {!isLecturer && <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right" style={{ width: "10%" }}>Tài liệu</th>}
                       </tr>
                    </thead>
                    <tbody className="divide-y divide-outline-variant/5">
                       {loading ? (
                          <tr>
                             <td colSpan={isLecturer ? 6 : 5} className="py-20 text-center opacity-40">
                                <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent mx-auto mb-4"></div>
                                <p className="text-[10px] font-black uppercase tracking-widest">Đang đồng bộ dữ liệu...</p>
                             </td>
                          </tr>
                       ) : filteredItems.length === 0 ? (
                          <tr>
                             <td colSpan={isLecturer ? 6 : 5} className="py-32 text-center opacity-40">
                                <span className="material-symbols-outlined text-6xl mb-4">move_item</span>
                                <h3 className="text-lg font-black uppercase tracking-widest">Không có dữ liệu</h3>
                                <p className="text-xs font-bold mt-2 uppercase tracking-tight">Chưa có hoạt động nào trong không gian này.</p>
                             </td>
                          </tr>
                       ) : (
                          filteredItems.map((it) => (
                            isLecturer ? (
                              <ProgressRow
                                key={it.id}
                                item={it}
                                loadingId={loadingId}
                                onSave={(s, c, d) => handleLecturerUpdate(it, s, c, d)}
                              />
                            ) : (
                              <tr key={it.id} className="group hover:bg-primary/[0.01] transition-colors">
                                 <td className="px-8 py-6">
                                    <p className="text-[11px] font-black text-on-surface uppercase">{new Date(it.createdAt).toLocaleDateString('vi-VN')}</p>
                                    <p className="text-[10px] font-bold text-outline mt-1 uppercase opacity-60">{new Date(it.createdAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'})}</p>
                                 </td>
                                 <td className="px-8 py-6">
                                    <p className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors">{it.title}</p>
                                    <p className="text-[11px] text-outline mt-1 font-medium line-clamp-1">{it.content}</p>
                                 </td>
                                 <td className="px-8 py-6 text-center">
                                    <div className={`inline-flex items-center px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest ${
                                       it.status === "DONE" ? "bg-primary text-white" :
                                       it.status === "IN_PROGRESS" ? "bg-secondary-container text-on-secondary-container" : "bg-surface-container text-outline"
                                    }`}>
                                       {it.status}
                                    </div>
                                 </td>
                                 <td className="px-8 py-6">
                                    {it.lecturerComment ? (
                                       <div className="bg-surface-container-low p-3 rounded-xl border border-outline-variant/10">
                                          <p className="text-[11px] font-bold text-on-surface-variant leading-relaxed line-clamp-2">{it.lecturerComment}</p>
                                          {it.deadline && (
                                              <p className="mt-2 text-[9px] font-black text-secondary uppercase tracking-widest flex items-center gap-1">
                                                 <span className="material-symbols-outlined text-[12px]">event</span>
                                                 Hạn: {new Date(it.deadline).toLocaleDateString('vi-VN')}
                                              </p>
                                          )}
                                       </div>
                                    ) : (
                                       <span className="text-[10px] font-black uppercase tracking-widest text-outline/30 italic">Đang đợi giảng viên đánh giá...</span>
                                    )}
                                 </td>
                                 <td className="px-8 py-6 text-right">
                                    {it.fileUrl ? (
                                       <a 
                                          href={it.fileUrl} 
                                          target="_blank" 
                                          rel="noreferrer" 
                                          className="p-2.5 rounded-xl bg-surface-container text-primary hover:bg-primary hover:text-white transition-all shadow-sm inline-flex"
                                          title="Tải tệp đính kèm"
                                       >
                                          <span className="material-symbols-outlined text-lg">download</span>
                                       </a>
                                    ) : (
                                       <span className="material-symbols-outlined text-outline/20">block</span>
                                    )}
                                 </td>
                              </tr>
                            )
                          ))
                       )}
                    </tbody>
                 </table>
              </div>
           </div>
        </div>
      </div>
    </div>
  );
}
