import { useEffect, useState, useCallback, useMemo } from "react";
import { registrationApi } from "../api/registrationApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { topicApi } from "../api/topicApi";
import { connectAndSubscribe } from "../ws/stomp";
import { mergeProgressUpdate } from "../components/ProgressRow";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";
import { PROGRESS_STATUS, statusOptions } from "../constants/status";
import ProgressSubmitPanel from "./projectSpace/ProgressSubmitPanel";
import MilestonePanel from "./projectSpace/MilestonePanel";
import ProgressHistoryPanel from "./projectSpace/ProgressHistoryPanel";
import useLoadEffect from '../hooks/useLoadEffect';

export default function ProjectSpacePage() {
  const { user } = useAuth();
  const userId = user?.id;
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
    if (!isStudent || !userId) return;
    try {
      const data = await registrationApi.getMine(userId);
      const approved = (data || []).filter((r) => r.approved === true);
      if (approved.length > 0) {
        setTopicId(String(approved[0].topic?.id || ""));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isStudent, userId]);

  const loadLecturerTopics = useCallback(async () => {
    if (!isLecturer || !userId) return;
    try {
      const data = await topicApi.getAllTopics({ lecturerId: userId, size: 100 });
      setLecturerTopics(data.items || []);
      if (data.items?.length > 0 && !topicId) {
        setTopicId(String(data.items[0].id));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isLecturer, userId, topicId]);

  const loadTopics = useCallback(() => {
    if (isStudent) loadStudentTopic();
    if (isLecturer) loadLecturerTopics();
  }, [loadStudentTopic, loadLecturerTopics, isStudent, isLecturer]);

  useLoadEffect(loadTopics);

  const loadProgress = useCallback(async () => {
    if (!topicId) {
      setItems([]);
      return;
    }
    setLoading(true);
    try {
      let arr = [];
      if (isStudent) {
        arr = await progressApi.listByStudent(userId, topicId);
      } else {
        // không gian đề tài chỉ cần các báo cáo gần nhất (đầy đủ ở trang Nhận xét tiến độ)
        arr = (await progressApi.listByTopic(topicId, { size: 100 }))?.items;
      }
      setItems(arr || []);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không thể tải dữ liệu tiến độ" });
    } finally {
      setLoading(false);
    }
  }, [topicId, isStudent, userId]);

  useLoadEffect(loadProgress);

  useEffect(() => {
    if (!topicId) return;
    const client = connectAndSubscribe({
      destination: `/topic/progress.${topicId}`,
      onMessage: (payload) => {
        const type = payload?.type;
        const pr = payload?.progress;
        if (!type || !pr) return;
        if (type === "PROGRESS_CREATED") {
          if (isLecturer) {
            loadProgress(); // tải lại để có điểm trùng lặp (bản WebSocket không chứa)
            setNotice({ type: "success", message: "Có báo cáo mới từ sinh viên!" });
          } else {
            setItems((prev) => (prev.some((x) => x.id === pr.id) ? prev : [pr, ...prev]));
          }
        } else if (type === "PROGRESS_UPDATED") {
          setItems((prev) => mergeProgressUpdate(prev, pr));
          if (isStudent) {
            setNotice({ type: "info", message: "Giảng viên đã cập nhật phản hồi báo cáo." });
          }
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId, isStudent, isLecturer, loadProgress]);

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
        fileUrl = (await fileApi.upload(file)).fileUrl;
      }
      await progressApi.create(userId, topicId, {
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
           <button aria-label="Làm mới" onClick={loadProgress} className="p-2.5 bg-white dark:bg-slate-900 border border-outline-variant/20 rounded-xl hover:bg-surface-container transition-all">
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
                 <label htmlFor="projectspacepage-select-248" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn đề tài nghiên cứu</label>
                 <select id="projectspacepage-select-248" 
                   className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-black transition-all duration-300 outline-none cursor-pointer" 
                   value={topicId} 
                   onChange={(e) => setTopicId(e.target.value)}
                 >
                    {lecturerTopics.map((t) => (
                      <option key={t.id} value={t.id}>{t.title}</option>
                    ))}
                    {lecturerTopics.length === 0 && <option value="">Chưa có đề tài nào</option>}
                 </select>
              </div>
              <div className="lg:col-span-1 hidden lg:block text-center text-outline">
                 <span className="material-symbols-outlined">arrow_forward</span>
              </div>
              <div className="lg:col-span-4 space-y-1.5">
                 <label htmlFor="projectspacepage-input-266" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm nội dung / Sinh viên</label>
                 <div className="relative group">
                   <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
                   <input id="projectspacepage-input-266"
                     className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                     placeholder="Tiêu đề, tên SV, MSSV..."
                     value={search}
                     onChange={(e) => setSearch(e.target.value)}
                   />
                 </div>
              </div>
              <div className="lg:col-span-3 space-y-1.5">
                 <label htmlFor="projectspacepage-select-276" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Phân loại trạng thái</label>
                 <select id="projectspacepage-select-276" 
                    className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none" 
                    value={statusFilter} 
                    onChange={(e) => setStatusFilter(e.target.value)}
                 >
                    <option value="">Tất cả</option>
                    {statusOptions(PROGRESS_STATUS).map((o) => (
                   <option key={o.value} value={o.value}>{o.label}</option>
                 ))}
                 </select>
              </div>
           </div>
        </div>
      )}

      <MilestonePanel topicId={topicId} isLecturer={isLecturer} />

      {/* Main Workspace Grid */}
      <div className={isLecturer ? "flex flex-col space-y-8" : "grid grid-cols-1 lg:grid-cols-12 gap-8"}>
        
        {/* Student Column: Form Input */}
        {isStudent && (
          <ProgressSubmitPanel content={content} file={file} handleStudentSubmit={handleStudentSubmit} selectedLecturerTopic={selectedLecturerTopic} setContent={setContent} setFile={setFile} setTitle={setTitle} studentCanSubmit={studentCanSubmit} submitting={submitting} title={title} />
        )}

        {/* History / Review Column */}
        <ProgressHistoryPanel filteredItems={filteredItems} handleLecturerUpdate={handleLecturerUpdate} isLecturer={isLecturer} isStudent={isStudent} loading={loading} loadingId={loadingId} recentCount={recentCount} />
      </div>
    </div>
  );
}
