import { useState, useCallback } from "react";
import { topicApi } from "../api/topicApi";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";
import { TOPIC_STATUS, statusOptions } from "../constants/status";
import { EMPTY_FORM, MAX_CAPACITY, MIN_CAPACITY } from "./lecturerTopics/topicForm";
import TopicTable from "./lecturerTopics/TopicTable";
import TopicFormModal from "./lecturerTopics/TopicFormModal";
import useLoadEffect from '../hooks/useLoadEffect';


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
  const [form, setForm] = useState(EMPTY_FORM);
  const [openWorkspaces, setOpenWorkspaces] = useState([]);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [notice, setNotice] = useState(null);

  const getStatusInfo = (s) => {
    if (s === "OPEN") return { label: "Mở đăng ký", color: "text-primary", bg: "bg-primary/10", icon: "door_open" };
    if (s === "CLOSED") return { label: "Đã đóng", color: "text-outline", bg: "bg-surface-container", icon: "lock" };
    if (s === "FULL") return { label: "Đã đủ sinh viên", color: "text-secondary", bg: "bg-secondary/10", icon: "group" };
    if (s === "COMPLETED") return { label: "Hoàn thành", color: "text-green-700", bg: "bg-green-500/10", icon: "task_alt" };
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

  useLoadEffect(load);

  const openCreate = async () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
    try {
      const list = await topicApi.workspacesOpenForTopics();
      setOpenWorkspaces(list || []);
      if (list?.length === 1) setForm((f) => ({ ...f, workspaceId: String(list[0].id) }));
    } catch {
      setOpenWorkspaces([]);
    }
  };
  const openEdit = (t) => {
    setEditing(t);
    setForm({ title: t.title || "", description: t.description || "", capacity: t.capacity || 1, workspaceId: "" });
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
    const capacity = Number(form.capacity);
    if (!Number.isInteger(capacity) || capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
      setNotice({ type: "warning", message: `Số sinh viên phải là số nguyên từ ${MIN_CAPACITY} đến ${MAX_CAPACITY}.` });
      return;
    }
    if (!editing && !form.workspaceId) {
      setNotice({ type: "warning", message: "Vui lòng chọn đợt đồ án cho đề tài." });
      return;
    }
    try {
      const payload = { title: form.title.trim(), description: form.description?.trim() || "", capacity };
      if (editing) {
        await topicApi.update(editing.id, lecturerId, payload);
      } else {
        await topicApi.create(lecturerId, { ...payload, workspace: { id: Number(form.workspaceId) } });
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
          className="px-8 py-3.5 bg-primary text-on-primary rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all flex items-center gap-2 group"
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
               <label htmlFor="lecturertopicspage-input-199" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm đề tài</label>
               <div className="relative group">
                 <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
                 <input id="lecturertopicspage-input-199"
                   className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
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
               <label htmlFor="lecturertopicspage-select-212" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Trạng thái hệ thống</label>
               <select id="lecturertopicspage-select-212"
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none"
                 value={status}
                 onChange={(e) => {
                   setPage(0);
                   setStatus(e.target.value);
                 }}
               >
                 <option value="">Tất cả trạng thái</option>
                 {statusOptions(TOPIC_STATUS).map((o) => (
                   <option key={o.value} value={o.value}>{o.label}</option>
                 ))}
               </select>
            </div>
            <div className="md:col-span-2 space-y-1.5">
               <label htmlFor="lecturertopicspage-select-228" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Hiển thị</label>
               <select id="lecturertopicspage-select-228"
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none"
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
      <TopicTable actionLoadingId={actionLoadingId} error={error} getStatusInfo={getStatusInfo} handleClose={handleClose} handleDelete={handleDelete} handleOpen={handleOpen} items={items} load={load} loading={loading} openEdit={openEdit} page={page} setPage={setPage} totalPages={totalPages} />

      {/* Premium Form Modal */}
      {modalOpen && (
        <TopicFormModal closeModal={closeModal} editing={editing} form={form} handleSave={handleSave} openWorkspaces={openWorkspaces} setForm={setForm} />
      )}
    </div>
  );
}
