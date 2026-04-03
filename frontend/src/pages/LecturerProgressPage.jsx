import React, { useEffect, useState, useCallback, useMemo } from "react";
import { topicApi } from "../api/topicApi";
import { progressApi } from "../api/progressApi";
import { connectAndSubscribe } from "../ws/stomp";
import ProgressRow from "../components/ProgressRow";
import InlineNotice from "../components/InlineNotice";
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
          setNotice({ type: "success", message: "Có báo cáo mới từ sinh viên!" });
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
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header / Hero */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-secondary mb-2 block">Theo dõi học thuật</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Nhận xét tiến độ</h1>
          <p className="text-sm text-outline mt-2 font-medium">Theo dõi các báo cáo từ sinh viên, phản hồi trực tuyến và kiểm soát thời gian hoàn thành.</p>
        </div>
        
        <div className="flex items-center gap-3">
           <div className="px-4 py-2 bg-surface-container text-on-surface-variant rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center gap-2 border border-outline-variant/10">
              <span className="material-symbols-outlined text-sm">update</span>
              Realtime Sync
           </div>
           <button onClick={loadProgress} className="p-2.5 bg-primary text-white rounded-xl shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all">
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

      {/* Control Bar */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-[2rem] border border-outline-variant/10 shadow-sm">
         <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-end">
            <div className="lg:col-span-4 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn đề tài đang xem</label>
               <select 
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-black transition-all duration-300 outline-none cursor-pointer" 
                 value={topicId} 
                 onChange={e => setTopicId(e.target.value)}
                >
                  {topics.map(t => (
                    <option key={t.id} value={t.id}>{t.title}</option>
                  ))}
                  {topics.length === 0 && <option value="">Không có đề tài nào</option>}
               </select>
            </div>
            <div className="lg:col-span-1 hidden lg:block text-center text-outline">
               <span className="material-symbols-outlined">chevron_right</span>
            </div>
            <div className="lg:col-span-4 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm nội dung</label>
               <div className="relative group">
                 <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
                 <input
                   className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                   placeholder="Tiêu đề, tên SV, MSSV..."
                   value={search}
                   onChange={e => setSearch(e.target.value)}
                 />
               </div>
            </div>
            <div className="lg:col-span-3 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lọc trạng thái</label>
               <select 
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none" 
                 value={statusFilter} 
                 onChange={e => setStatusFilter(e.target.value)}
               >
                 <option value="">Tất cả trạng thái</option>
                 <option value="TODO">Việc cần làm (TODO)</option>
                 <option value="IN_PROGRESS">Đang thực hiện</option>
                 <option value="DONE">Đã hoàn thành</option>
               </select>
            </div>
         </div>
      </div>

      {/* Main Content Area */}
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[400px]">
        <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30 flex justify-between items-center">
           <div>
              <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Danh sách báo cáo</h2>
              <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Hiển thị các cập nhật tiến độ từ học viên</p>
           </div>
           <div className="flex items-center gap-2">
              <span className="text-xs font-black text-outline mr-2">{filteredItems.length} BÁO CÁO</span>
              <div className="w-10 h-10 rounded-xl bg-surface-container flex items-center justify-center">
                 <span className="material-symbols-outlined text-outline">history</span>
              </div>
           </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-surface-container-low/20">
                <th className="px-8 py-4 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "15%" }}>Mốc thời gian</th>
                <th className="px-8 py-4 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Sinh viên</th>
                <th className="px-8 py-4 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Tiêu đề & Nội dung</th>
                <th className="px-8 py-4 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "15%" }}>Tiến độ / Hạn</th>
                <th className="px-8 py-4 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Nhận xét từ GV</th>
                <th className="px-8 py-4 text-[10px] uppercase tracking-widest font-black text-outline text-right" style={{ width: "10%" }}>Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {loading ? (
                <tr>
                   <td colSpan="6" className="py-20 text-center opacity-40">
                      <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent mx-auto mb-4"></div>
                      <p className="text-[10px] font-black uppercase tracking-widest">Đang tải dữ liệu tiến độ...</p>
                   </td>
                </tr>
              ) : filteredItems.length === 0 ? (
                <tr>
                   <td colSpan="6" className="py-32 text-center opacity-40">
                      <span className="material-symbols-outlined text-6xl mb-4">checklist</span>
                      <h3 className="text-lg font-black uppercase tracking-widest">Không có dữ liệu</h3>
                      <p className="text-xs font-bold mt-2">Chưa có báo cáo nào khớp với tiêu chí tìm kiếm.</p>
                   </td>
                </tr>
              ) : (
                filteredItems.map(it => (
                  <ProgressRow
                    key={it.id}
                    item={it}
                    loadingId={loadingId}
                    onSave={(s, c, d) => handleLecturerUpdate(it, s, c, d)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
