import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useLocation } from "react-router-dom";
import { registrationApi } from "../api/registrationApi";
import RegistrationTable from "../components/RegistrationTable";
import InlineNotice from "../components/InlineNotice";
import { connectAndSubscribe } from "../ws/stomp";

export default function LecturerRegistrationPage() {
  const location = useLocation();
  const [topicId, setTopicId] = useState("1");
  const [data, setData] = useState([]);
  const [loadingId, setLoadingId] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState("registered_desc");

  // Status for modal
  const [showModal, setShowModal] = useState(false);
  const [modalAction, setModalAction] = useState(null); // "approve" | "reject" | "grade"
  const [selectedReg, setSelectedReg] = useState(null);
  const [notice, setNotice] = useState(null);
  
  const [gradeScore, setGradeScore] = useState(0);
  const [gradeFeedback, setGradeFeedback] = useState("");

  const load = useCallback(async () => {
    try {
      const result = await registrationApi.getByTopic(topicId);
      setData(result || []);
    } catch {
      setNotice({ type: "danger", message: "Không thể tải danh sách đăng ký." });
    }
  }, [topicId]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const qId = params.get("topicId");
    if (qId) {
      setTopicId(qId);
    }
  }, [location.search]);

  useEffect(() => {
    if (!topicId) return;
    const client = connectAndSubscribe({
      destination: `/topic/registration/${topicId}`,
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
    setGradeScore(reg.score || 0);
    setGradeFeedback(reg.feedback || "");
    setModalAction("grade");
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedReg(null);
    setModalAction(null);
    setRejectReason("");
  };

  const handleConfirm = async () => {
    if (!selectedReg || !modalAction) return;

    setLoadingId(selectedReg.id);

    try {
      if (modalAction === "approve") {
        await registrationApi.approve(selectedReg.id);
      } else if (modalAction === "reject") {
        await registrationApi.reject(selectedReg.id, rejectReason);
      } else if (modalAction === "grade") {
        await registrationApi.grade(selectedReg.id, gradeScore, gradeFeedback);
      }

      await load();
      setNotice({ type: "success", message: "Cập nhật dữ liệu thành công!" });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Thao tác thất bại." });
    } finally {
      setLoadingId(null);
      closeModal();
    }
  };

  const filteredData = useMemo(() => {
    const q = search.trim().toLowerCase();
    let arr = data || [];
    if (q) {
      arr = arr.filter((r) => {
        const student = r.student || {};
        const user = student.user || {};
        return (
          String(r.id).includes(q) ||
          (student.studentCode || "").toLowerCase().includes(q) ||
          (student.className || "").toLowerCase().includes(q) ||
          (user.fullName || "").toLowerCase().includes(q)
        );
      });
    }

    const cmp = {
      registered_desc: (a, b) => new Date(b.registeredAt) - new Date(a.registeredAt),
      registered_asc: (a, b) => new Date(a.registeredAt) - new Date(b.registeredAt),
      studentCode_asc: (a, b) => String(a.student?.studentCode || "").localeCompare(String(b.student?.studentCode || "")),
      fullName_asc: (a, b) => String(a.student?.user?.fullName || "").localeCompare(String(b.student?.user?.fullName || "")),
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
      } catch (error) {
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
            <div className="lg:col-span-2 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mã Đề Tài</label>
               <input
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-black transition-all duration-300 outline-none"
                 value={topicId}
                 onChange={(e) => setTopicId(e.target.value)}
               />
            </div>
            <div className="lg:col-span-3 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Sắp xếp theo</label>
               <select
                 className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 cursor-pointer outline-none"
                 value={sortBy}
                 onChange={(e) => setSortBy(e.target.value)}
               >
                 <option value="registered_desc">Ngày đăng ký (Mới trước)</option>
                 <option value="registered_asc">Ngày đăng ký (Cũ trước)</option>
                 <option value="studentCode_asc">Mã số sinh viên A-Z</option>
                 <option value="fullName_asc">Họ tên sinh viên A-Z</option>
               </select>
            </div>
            <div className="lg:col-span-4 space-y-1.5">
               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm sinh viên</label>
               <div className="relative group">
                 <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">search</span>
                 <input
                   className="w-full pl-12 pr-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                   placeholder="Mã số, tên, lớp..."
                   value={search}
                   onChange={(e) => setSearch(e.target.value)}
                 />
               </div>
            </div>
            <div className="lg:col-span-3 flex gap-2">
               <button onClick={load} className="flex-1 px-4 py-3 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all">
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
          />
        </div>
      </div>

      {/* Modern Modal */}
      {showModal && selectedReg && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-in fade-in duration-300">
          <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={closeModal}></div>
          <div className="bg-white dark:bg-slate-900 w-full max-w-lg rounded-3xl shadow-2xl z-10 overflow-hidden animate-in slide-in-from-bottom-8 duration-500">
            {/* Modal Header */}
            <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30">
               <div className="flex justify-between items-start">
                  <div>
                    <span className="text-[10px] font-black uppercase tracking-widest text-primary mb-1 block">Yêu cầu xác nhận</span>
                    <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline leading-none">
                       {modalAction === "approve" ? "Phê duyệt đăng ký" : modalAction === "reject" ? "Từ chối đăng ký" : "Chấm điểm đề tài"}
                    </h2>
                  </div>
                  <button onClick={closeModal} className="p-2 hover:bg-surface-container-high rounded-xl transition-colors">
                     <span className="material-symbols-outlined">close</span>
                  </button>
               </div>
            </div>

            {/* Modal Body */}
            <div className="p-8 space-y-6">
               <div className="flex items-center gap-4 p-4 bg-surface-container-low rounded-2xl border border-outline-variant/5">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary font-black text-lg">
                     {selectedReg.student?.user?.fullName?.charAt(0)}
                  </div>
                  <div>
                     <p className="text-sm font-black text-on-surface uppercase leading-none">{selectedReg.student?.user?.fullName}</p>
                     <p className="text-[10px] text-outline font-bold mt-1.5 uppercase tracking-widest">MSSV: {selectedReg.student?.studentCode} • Lớp: {selectedReg.student?.className}</p>
                  </div>
               </div>

               {modalAction !== "grade" ? (
                  <p className="text-sm font-medium text-on-surface-variant leading-relaxed">
                     Bạn đang chuẩn bị hệ thống để <strong>{modalAction === "approve" ? "PHÊ DUYỆT" : "TỪ CHỐI"}</strong> yêu cầu của sinh viên này. Thao tác này sẽ cập nhật trạng thái đề tài và gửi thông báo cho sinh viên.
                  </p>
               ) : (
                  <div className="space-y-6">
                      <div className="space-y-1.5">
                        <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Điểm đánh giá (Hệ 10)</label>
                        <input
                          type="number"
                          step="0.1"
                          min="0"
                          max="10"
                          className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-3xl font-black text-primary transition-all duration-300 outline-none text-center"
                          value={gradeScore}
                          onChange={(e) => setGradeScore(e.target.value)}
                        />
                      </div>
                      <div className="space-y-1.5">
                        <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Nhận xét chi tiết</label>
                        <textarea
                          className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[120px]"
                          value={gradeFeedback}
                          onChange={(e) => setGradeFeedback(e.target.value)}
                          placeholder="Nhận xét về quá trình thực hiện và kết quả của sinh viên..."
                        />
                      </div>
                      <div className="p-4 bg-secondary-container/20 text-on-secondary-container rounded-xl flex items-center gap-3">
                         <span className="material-symbols-outlined">info</span>
                         <p className="text-[10px] font-black uppercase tracking-widest leading-relaxed">Sau khi lưu điểm, đề tài sẽ tự động chuyển sang trạng thái HOÀN THÀNH.</p>
                      </div>
                  </div>
               )}

               {modalAction === "reject" && (
                  <div className="space-y-1.5 animate-in slide-in-from-top-4 duration-300">
                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lý do từ chối (Gửi đến sinh viên)</label>
                    <textarea
                      className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[100px]"
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      placeholder="Giải thích lý do từ chối để sinh viên hiểu rõ và cải thiện..."
                    />
                  </div>
               )}
            </div>

            {/* Modal Footer */}
            <div className="px-8 py-6 bg-surface-container-low/30 border-t border-outline-variant/5 flex justify-end gap-3">
               <button onClick={closeModal} className="px-6 py-3 bg-white text-on-surface-variant font-black text-[11px] uppercase tracking-widest rounded-xl hover:bg-surface-container transition-all" disabled={!!loadingId}>
                  Hủy bỏ
               </button>
               <button
                 onClick={handleConfirm}
                 className={`px-8 py-3 rounded-xl font-black text-[11px] uppercase tracking-widest transition-all shadow-lg ${
                    modalAction === "reject" 
                    ? "bg-error text-white shadow-error/20" 
                    : "bg-primary text-white shadow-primary/20"
                 } hover:scale-105 active:scale-95 disabled:opacity-50`}
                 disabled={!!loadingId}
               >
                 {loadingId === selectedReg.id ? "Đang xử lý..." : modalAction === "approve" ? "Duyệt ngay" : modalAction === "reject" ? "Từ chối ngay" : "Lưu kết quả"}
               </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
