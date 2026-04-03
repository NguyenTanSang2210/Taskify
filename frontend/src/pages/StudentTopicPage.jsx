import React, { useEffect, useState } from "react";
import { topicApi } from "../api/topicApi";
import { registrationApi } from "../api/registrationApi";
import TopicCard from "../components/TopicCard";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";

export default function StudentTopicPage() {
  const { user } = useAuth();
  const [topics, setTopics] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(6);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [loadingId, setLoadingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null);
  
  const roleName = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isStudent = roleName === "STUDENT";
  const studentId = isStudent ? user?.id : null;

  const load = async (override = {}) => {
    setLoading(true);
    setError("");
    try {
      const params = {
        studentId,
        query,
        status: status || undefined,
        page,
        size,
        ...override,
      };
      const data = await topicApi.getAllTopics(params);
      setTopics(data.items || []);
      setTotalPages(data.totalPages || 0);
      setSize(data.size ?? size);
    } catch (e) {
      console.error(e);
      setError("Không tải được danh sách đề tài");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, status, page, size]);

  const handleRegister = async (topicId) => {
    if (!isStudent || !studentId) {
      setNotice({ type: "warning", message: "Vui lòng đăng nhập bằng tài khoản Sinh viên để đăng ký." });
      return;
    }
    setLoadingId(topicId);
    try {
      await registrationApi.registerTopic(studentId, topicId);
      await load();
      setNotice({ type: "success", message: "Đăng ký đề tài thành công!" });
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Có lỗi khi đăng ký. Vui lòng thử lại." });
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Header Section */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Cơ hội nghiên cứu</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Danh sách đề tài</h1>
          <p className="text-sm text-outline mt-2 font-medium">Khám phá các đề tài nghiên cứu khoa học và đồ án tốt nghiệp hiện có.</p>
        </div>
        
        <div className="flex items-center gap-2">
           <div className="px-4 py-2 bg-secondary-container text-on-secondary-container rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center gap-2">
              <span className="material-symbols-outlined text-sm">info</span>
              Học kỳ 1 • 2024-2025
           </div>
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

      {/* Filters Area */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-[2rem] border border-outline-variant/10 shadow-sm space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-end">
          <div className="md:col-span-6 space-y-1.5">
            <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm đề tài</label>
            <div className="relative group">
              <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
              <input
                type="text"
                className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                placeholder="Nhập từ khóa, tên đề tài, giảng viên..."
                value={query}
                onChange={(e) => {
                  setPage(0);
                  setQuery(e.target.value);
                }}
              />
            </div>
          </div>

          <div className="md:col-span-3 space-y-1.5">
            <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Trạng thái đăng ký</label>
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
              <option value="CLOSED">Đã đóng đăng ký</option>
              <option value="REGISTERED">Đề tài đã đăng ký</option>
            </select>
          </div>

          <div className="md:col-span-3 space-y-1.5">
            <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Hiển thị</label>
            <select
              className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none"
              value={size}
              onChange={(e) => {
                setPage(0);
                setSize(Number(e.target.value));
              }}
            >
              <option value={6}>6 đề tài / trang</option>
              <option value={9}>9 đề tài / trang</option>
              <option value={12}>12 đề tài / trang</option>
            </select>
          </div>
        </div>
      </div>

      {!user && (
        <div className="flex items-center gap-3 p-4 bg-tertiary-container/10 text-tertiary rounded-2xl border border-tertiary-container/20">
           <span className="material-symbols-outlined">lock</span>
           <p className="text-xs font-black uppercase tracking-widest">Vui lòng đăng nhập để thực hiện chức năng đăng ký đề tài.</p>
        </div>
      )}

      {/* Topics Grid or Status */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 opacity-50 pointer-events-none">
           {[...Array(6)].map((_, i) => (
             <div key={i} className="h-64 bg-surface-container rounded-3xl animate-pulse"></div>
           ))}
        </div>
      ) : error ? (
        <div className="py-20 flex flex-col items-center justify-center text-center">
           <span className="material-symbols-outlined text-6xl text-error/30 mb-4">error</span>
           <p className="text-sm font-black uppercase tracking-widest text-error">{error}</p>
           <button onClick={() => load()} className="mt-4 text-xs font-black uppercase tracking-widest text-primary hover:underline">Thử lại</button>
        </div>
      ) : topics.length === 0 ? (
        <div className="py-32 flex flex-col items-center justify-center text-center opacity-40">
           <span className="material-symbols-outlined text-7xl mb-4">search_off</span>
           <h3 className="text-xl font-black uppercase tracking-widest">Không tìm thấy đề tài</h3>
           <p className="text-xs font-bold mt-2">Vui lòng thử lại với từ khóa hoặc bộ lọc khác.</p>
        </div>
      ) : (
        <div className="space-y-10">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {topics.map((t) => (
              <div key={t.id} className="h-full">
                <TopicCard
                  topic={t}
                  loading={loadingId === t.id}
                  onRegister={handleRegister}
                />
              </div>
            ))}
          </div>

          {/* Pagination */}
          <div className="flex flex-col md:flex-row justify-between items-center gap-4 py-8 border-t border-outline-variant/10">
            <p className="text-[10px] font-black uppercase tracking-widest text-outline">
              Hiển thị đề tài trang {page + 1} của {Math.max(1, totalPages)}
            </p>
            
            <div className="flex items-center gap-2">
              <button
                className={`p-2 rounded-xl transition-all ${
                  page <= 0 
                  ? "bg-surface-container text-outline/30 cursor-not-allowed" 
                  : "bg-white border border-outline-variant/20 text-on-surface hover:bg-primary-container hover:text-white"
                }`}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page <= 0}
                title="Trang trước"
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
                  ? "bg-surface-container text-outline/30 cursor-not-allowed" 
                  : "bg-white border border-outline-variant/20 text-on-surface hover:bg-primary-container hover:text-white"
                }`}
                onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
                disabled={page + 1 >= totalPages}
                title="Trang sau"
              >
                <span className="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
