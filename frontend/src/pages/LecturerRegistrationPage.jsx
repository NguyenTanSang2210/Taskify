import { useEffect, useState, useCallback, useMemo } from "react";
import { useLocation } from "react-router-dom";
import { registrationApi } from "../api/registrationApi";
import { topicApi } from "../api/topicApi";
import { useAuth } from "../context/AuthContext";
import RegistrationTable from "../components/RegistrationTable";
import GradeHistoryModal from "../components/GradeHistoryModal";
import InlineNotice from "../components/InlineNotice";
import { connectAndSubscribe } from "../ws/stomp";
import RegistrationReviewModal from "./lecturerRegistration/RegistrationReviewModal";
import useLoadEffect from '../hooks/useLoadEffect';

export default function LecturerRegistrationPage() {
  const location = useLocation();
  const { user } = useAuth();
  const [topicId, setTopicId] = useState(() => new URLSearchParams(location.search).get("topicId") || "");
  const [topics, setTopics] = useState([]);
  const [data, setData] = useState([]);
  const [loadingId, setLoadingId] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState("registered_desc");

  // Status for modal
  const [showModal, setShowModal] = useState(false);
  const [modalAction, setModalAction] = useState(null); // "approve" | "reject" | "grade" | "revision"
  const [selectedReg, setSelectedReg] = useState(null);
  const [notice, setNotice] = useState(null);
  
  const [gradeScore, setGradeScore] = useState(0);
  const [gradeFeedback, setGradeFeedback] = useState("");
  const [gradeReason, setGradeReason] = useState(""); // lý do sửa điểm đã có / lý do yêu cầu chấm lại
  const [historyReg, setHistoryReg] = useState(null);
  const [modalError, setModalError] = useState("");

  const load = useCallback(async () => {
    if (!topicId) return;
    try {
      const result = await registrationApi.getByTopic(topicId);
      setData(result || []);
    } catch {
      setNotice({ type: "danger", message: "Không thể tải danh sách đăng ký." });
    }
  }, [topicId]);

  // Đề tài của giảng viên để chọn; chưa chọn (không có ?topicId=) thì lấy đề tài đầu tiên
  useEffect(() => {
    if (!user?.id) return;
    let cancelled = false;
    topicApi
      .getAllTopics({ lecturerId: user.id, size: 100 })
      .then((res) => {
        if (cancelled) return;
        const list = res?.items || [];
        setTopics(list);
        setTopicId((current) => current || (list[0] ? String(list[0].id) : ""));
      })
      .catch(() => !cancelled && setNotice({ type: "danger", message: "Không tải được danh sách đề tài." }));
    return () => {
      cancelled = true;
    };
  }, [user?.id]);

  // Tải danh sách đăng ký mỗi khi đổi đề tài (trước đây phải bấm "Làm mới" mới thấy dữ liệu)
  useLoadEffect(load);

  useEffect(() => {
    if (!topicId) return;
    const client = connectAndSubscribe({
      destination: `/topic/registration.${topicId}`,
      onMessage: async (payload) => {
        const type = payload?.type;
        const reg = payload?.registration;
        if (!type || !reg) return;
        await load();
        setNotice({ type: "warning", message: "Có cập nhật đăng ký mới (Realtime)" });
      },
    });
    return () => client?.deactivate?.();
  }, [topicId, load]);

  const openApproveModal = (reg) => {
    setSelectedReg(reg);
    setModalAction("approve");
    setShowModal(true);
  };

  const openRejectModal = (reg) => {
    setSelectedReg(reg);
    setModalAction("reject");
    setShowModal(true);
  };

  const openGradeModal = (reg) => {
    setSelectedReg(reg);
    setGradeScore(reg.score ?? "");
    setGradeFeedback(reg.feedback || "");
    setGradeReason("");
    setModalAction("grade");
    setShowModal(true);
  };

  const openRevisionModal = (reg) => {
    setSelectedReg(reg);
    setGradeReason("");
    setModalAction("revision");
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedReg(null);
    setModalAction(null);
    setRejectReason("");
    setModalError("");
  };

  // Kiểm tra sớm ở trình duyệt — cùng quy tắc với backend (GradeService)
  const validateModal = () => {
    if (modalAction === "grade") {
      const score = Number(String(gradeScore).replace(",", "."));
      if (String(gradeScore).trim() === "" || Number.isNaN(score)) return "Vui lòng nhập điểm.";
      if (score < 0 || score > 10) return "Điểm phải từ 0 đến 10.";
      if (Math.abs(score * 4 - Math.round(score * 4)) > 1e-9) return "Điểm chỉ được lẻ đến 0,25 (ví dụ 7,25; 7,5; 7,75).";
      if (selectedReg.score !== null && !gradeReason.trim()) return "Vui lòng nêu lý do chỉnh sửa điểm.";
    }
    if (modalAction === "revision" && !gradeReason.trim()) return "Vui lòng nêu lý do cần chấm lại.";
    return "";
  };

  const handleConfirm = async () => {
    if (!selectedReg || !modalAction) return;
    const invalid = validateModal();
    if (invalid) {
      setModalError(invalid);
      return;
    }

    setLoadingId(selectedReg.id);

    try {
      if (modalAction === "approve") {
        await registrationApi.approve(selectedReg.id);
      } else if (modalAction === "reject") {
        await registrationApi.reject(selectedReg.id, rejectReason);
      } else if (modalAction === "grade") {
        const score = Number(String(gradeScore).replace(",", "."));
        await registrationApi.grade(selectedReg.id, score, gradeFeedback, gradeReason.trim());
      } else if (modalAction === "revision") {
        await registrationApi.requestRevision(selectedReg.id, gradeReason.trim());
      }

      await load();
      setNotice({
        type: "success",
        message: modalAction === "revision" ? "Đã gửi yêu cầu chấm lại tới quản trị khoa." : "Cập nhật dữ liệu thành công!",
      });
      closeModal();
    } catch (e) {
      // giữ modal mở để người dùng sửa lại dữ liệu
      setModalError(e.message || "Thao tác thất bại.");
    } finally {
      setLoadingId(null);
    }
  };

  const filteredData = useMemo(() => {
    const q = search.trim().toLowerCase();
    let arr = data || [];
    if (q) {
      arr = arr.filter((r) =>
        String(r.id).includes(q) ||
        (r.studentCode || "").toLowerCase().includes(q) ||
        (r.studentName || "").toLowerCase().includes(q)
      );
    }

    const cmp = {
      registered_desc: (a, b) => new Date(b.registeredAt) - new Date(a.registeredAt),
      registered_asc: (a, b) => new Date(a.registeredAt) - new Date(b.registeredAt),
      studentCode_asc: (a, b) => String(a.studentCode || "").localeCompare(String(b.studentCode || "")),
      fullName_asc: (a, b) => String(a.studentName || "").localeCompare(String(b.studentName || ""), "vi"),
    };
    const sorter = cmp[sortBy] || cmp.registered_desc;
    return [...arr].sort(sorter);
  }, [data, search, sortBy]);

  const topicTitle = useMemo(() => {
    const t = data?.[0]?.topic;
    return t?.title ? t.title : `Đề tài #${topicId}`;
  }, [data, topicId]);

  const stats = useMemo(() => {
    const total = data.length;
    let pending = 0, approved = 0, rejected = 0;
    data.forEach((r) => {
      if (r.approved === null) pending++;
      else if (r.approved === true) approved++;
      else rejected++;
    });
    return { total, pending, approved, rejected };
  }, [data]);

  const handleExportExcel = async () => {
      try {
          const blob = await registrationApi.exportExcel(topicId);
          const url = window.URL.createObjectURL(new Blob([blob]));
          const link = document.createElement('a');
          link.href = url;
          link.setAttribute('download', `dang-ky-de-tai-${topicId}.xlsx`);
          document.body.appendChild(link);
          link.click();
          link.parentNode.removeChild(link);
      } catch {
          setNotice({ type: "danger", message: "Xuất tệp Excel thất bại." });
      }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header / Hero */}
      <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-8 md:p-10 border border-outline-variant/10 shadow-sm flex flex-col md:flex-row justify-between items-center gap-8 relative overflow-hidden">
         <div className="relative z-10 flex-1">
            <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Duyệt đăng ký</span>
            <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Quản lý Đăng ký</h1>
            <p className="text-sm text-outline mt-3 font-medium max-w-xl">
               Theo dõi danh sách sinh viên đăng ký đề tài của bạn. Kiểm tra thông tin, thực hiện phê duyệt hoặc từ chối và tổng kết điểm số.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
               <div className="px-5 py-2 bg-surface-container rounded-xl flex items-center gap-2">
                  <span className="material-symbols-outlined text-lg text-primary">topic</span>
                  <span className="text-[11px] font-black uppercase tracking-widest text-on-surface truncate max-w-[300px]">{topicTitle}</span>
               </div>
            </div>
         </div>
         <div className="grid grid-cols-2 gap-4 w-full md:w-auto shrink-0 relative z-10">
            <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/10 text-center">
               <p className="text-[9px] font-black text-outline uppercase tracking-widest mb-1">Tổng cộng</p>
               <h3 className="text-2xl font-black text-on-surface">{stats.total}</h3>
            </div>
            <div className="bg-secondary-container p-4 rounded-2xl border border-secondary/10 text-center">
               <p className="text-[9px] font-black text-on-secondary-container uppercase tracking-widest mb-1">Chờ duyệt</p>
               <h3 className="text-2xl font-black text-on-secondary-container">{stats.pending}</h3>
            </div>
            <div className="bg-primary-container p-4 rounded-2xl border border-primary/10 text-center">
               <p className="text-[9px] font-black text-on-primary-container uppercase tracking-widest mb-1">Đã duyệt</p>
               <h3 className="text-2xl font-black text-on-primary-container">{stats.approved}</h3>
            </div>
            <div className="bg-error-container p-4 rounded-2xl border border-error/10 text-center">
               <p className="text-[9px] font-black text-on-error-container uppercase tracking-widest mb-1">Từ chối</p>
               <h3 className="text-2xl font-black text-on-error-container">{stats.rejected}</h3>
            </div>
         </div>
         {/* Decoration */}
         <div className="absolute top-0 right-0 w-64 h-64 bg-primary/5 rounded-full blur-[80px] -mr-32 -mt-32"></div>
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
               <label htmlFor="lecturerregistrationpage-select-274" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Đề tài</label>
               <select id="lecturerregistrationpage-select-274"
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-black transition-all duration-300 outline-none"
                 value={topicId}
                 onChange={(e) => setTopicId(e.target.value)}
                 aria-label="Chọn đề tài"
               >
                 {!topics.length && <option value="">Chưa có đề tài</option>}
                 {topicId && !topics.some((t) => String(t.id) === String(topicId)) && <option value={topicId}>Đề tài #{topicId}</option>}
                 {topics.map((t) => (
                   <option key={t.id} value={t.id}>{t.title}</option>
                 ))}
               </select>
            </div>
            <div className="lg:col-span-3 space-y-1.5">
               <label htmlFor="lecturerregistrationpage-select-289" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Sắp xếp theo</label>
               <select id="lecturerregistrationpage-select-289"
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none"
                 value={sortBy}
                 onChange={(e) => setSortBy(e.target.value)}
               >
                 <option value="registered_desc">Ngày đăng ký (Mới trước)</option>
                 <option value="registered_asc">Ngày đăng ký (Cũ trước)</option>
                 <option value="studentCode_asc">Mã số sinh viên A-Z</option>
                 <option value="fullName_asc">Họ tên sinh viên A-Z</option>
               </select>
            </div>
            <div className="lg:col-span-2 space-y-1.5">
               <label htmlFor="lecturerregistrationpage-input-304" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm sinh viên</label>
               <div className="relative group">
                 <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
                 <input id="lecturerregistrationpage-input-304"
                   className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                   placeholder="Mã số, tên, lớp..."
                   value={search}
                   onChange={(e) => setSearch(e.target.value)}
                 />
               </div>
            </div>
            <div className="lg:col-span-3 flex gap-2">
               <button onClick={load} className="flex-1 px-4 py-3 bg-primary text-on-primary rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all">
                  Làm mới
               </button>
               <button onClick={handleExportExcel} className="flex-1 px-4 py-3 bg-surface-container text-on-surface-variant rounded-xl font-black text-[10px] uppercase tracking-widest hover:bg-surface-container-high transition-all border border-outline-variant/10">
                  Xuất Excel
               </button>
            </div>
         </div>
      </div>

      {/* Main Table Container */}
      <div className="bg-white dark:bg-slate-900 rounded-[2rem] border border-outline-variant/10 shadow-sm overflow-hidden">
        <div className="p-1">
          <RegistrationTable
            data={filteredData}
            loadingId={loadingId}
            onApproveClick={openApproveModal}
            onRejectClick={openRejectModal}
            onGradeClick={openGradeModal}
            onRevisionClick={openRevisionModal}
            onHistoryClick={setHistoryReg}
          />
        </div>
      </div>

      {/* Modern Modal */}
      {showModal && selectedReg && (
        <RegistrationReviewModal closeModal={closeModal} gradeFeedback={gradeFeedback} gradeReason={gradeReason} gradeScore={gradeScore} handleConfirm={handleConfirm} loadingId={loadingId} modalAction={modalAction} modalError={modalError} rejectReason={rejectReason} selectedReg={selectedReg} setGradeFeedback={setGradeFeedback} setGradeReason={setGradeReason} setGradeScore={setGradeScore} setRejectReason={setRejectReason} />
      )}

      {historyReg && (
        <GradeHistoryModal
          registrationId={historyReg.id}
          title={`${historyReg.studentName || ""} — ${historyReg.studentCode || ""}`}
          onClose={() => setHistoryReg(null)}
        />
      )}
    </div>
  );
}
