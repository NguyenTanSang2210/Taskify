import { fmtDateTime } from "../../components/GradeHistoryModal";
import useDialog from "../../hooks/useDialog";

/** Tách từ LecturerRegistrationPage.jsx (P3-11). */
export default function RegistrationReviewModal({ closeModal, gradeFeedback, gradeReason, gradeScore, handleConfirm, loadingId, modalAction, modalError, rejectReason, selectedReg, setGradeFeedback, setGradeReason, setGradeScore, setRejectReason }) {
  const { panelRef, titleId } = useDialog(closeModal);
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-in fade-in duration-300">
      <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={closeModal} aria-hidden="true"></div>
      <div ref={panelRef} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1} className="bg-white dark:bg-slate-900 w-full max-w-lg rounded-3xl shadow-2xl z-10 overflow-hidden outline-none animate-in slide-in-from-bottom-8 duration-500">
        {/* Modal Header */}
        <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30">
           <div className="flex justify-between items-start">
              <div>
                <span className="text-[10px] font-black uppercase tracking-widest text-primary mb-1 block">Yêu cầu xác nhận</span>
                <h2 id={titleId} className="text-xl font-black text-on-surface uppercase tracking-tight font-headline leading-none">
                   {modalAction === "approve" ? "Phê duyệt đăng ký" : modalAction === "reject" ? "Từ chối đăng ký" : modalAction === "revision" ? "Yêu cầu chấm lại" : "Chấm điểm đề tài"}
                </h2>
              </div>
              <button aria-label="Đóng" onClick={closeModal} className="p-2 hover:bg-surface-container-high rounded-xl transition-colors">
                 <span className="material-symbols-outlined">close</span>
              </button>
           </div>
        </div>
    
        {/* Modal Body */}
        <div className="p-8 space-y-6">
           <div className="flex items-center gap-4 p-4 bg-surface-container-low rounded-2xl border border-outline-variant/5">
              <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary font-black text-lg">
                 {selectedReg.studentName?.charAt(0)}
              </div>
              <div>
                 <p className="text-sm font-black text-on-surface uppercase leading-none">{selectedReg.studentName}</p>
                 <p className="text-[10px] text-outline font-bold mt-1.5 uppercase tracking-widest">MSSV: {selectedReg.studentCode}</p>
              </div>
           </div>
    
           {modalAction === "revision" ? (
              <div className="space-y-4">
                 <p className="text-sm font-medium text-on-surface-variant leading-relaxed">
                    Đợt đã kết thúc nên điểm <strong>{String(selectedReg.score ?? "").replace(".", ",")}</strong> đang bị khóa. Yêu cầu sẽ được gửi tới quản trị khoa; khi được duyệt, bạn có thể sửa điểm <strong>một lần</strong> trong thời gian được mở khóa.
                 </p>
                 <div className="space-y-1.5">
                    <label htmlFor="registrationreviewmodal-textarea-45" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lý do cần chấm lại *</label>
                    <textarea id="registrationreviewmodal-textarea-45"
                      className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold outline-none min-h-[110px]"
                      value={gradeReason}
                      maxLength={2000}
                      onChange={(e) => setGradeReason(e.target.value)}
                      placeholder="VD: Nhập nhầm điểm bảo vệ, sinh viên phúc khảo được chấp nhận..."
                    />
                 </div>
              </div>
           ) : modalAction !== "grade" ? (
              <p className="text-sm font-medium text-on-surface-variant leading-relaxed">
                 Bạn đang chuẩn bị hệ thống để <strong>{modalAction === "approve" ? "PHÊ DUYỆT" : "TỪ CHỐI"}</strong> yêu cầu của sinh viên này. Thao tác này sẽ cập nhật trạng thái đề tài và gửi thông báo cho sinh viên.
              </p>
           ) : (
              <div className="space-y-6">
                  <div className="space-y-1.5">
                    <label htmlFor="registrationreviewmodal-input-62" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Điểm đánh giá (Hệ 10)</label>
                    <input id="registrationreviewmodal-input-62"
                      type="number"
                      step="0.25"
                      min="0"
                      max="10"
                      className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-3xl font-black text-primary transition-all duration-300 outline-none text-center"
                      value={gradeScore}
                      onChange={(e) => setGradeScore(e.target.value)}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label htmlFor="registrationreviewmodal-textarea-74" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Nhận xét chi tiết</label>
                    <textarea id="registrationreviewmodal-textarea-74"
                      className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[120px]"
                      value={gradeFeedback}
                      onChange={(e) => setGradeFeedback(e.target.value)}
                      placeholder="Nhận xét về quá trình thực hiện và kết quả của sinh viên..."
                    />
                  </div>
                  {selectedReg.score !== null && (
                    <div className="space-y-1.5">
                      <label htmlFor="registrationreviewmodal-textarea-84" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lý do chỉnh sửa điểm *</label>
                      <textarea id="registrationreviewmodal-textarea-84"
                        className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold outline-none min-h-[80px]"
                        value={gradeReason}
                        maxLength={2000}
                        onChange={(e) => setGradeReason(e.target.value)}
                        placeholder="Mọi lần sửa điểm đều được lưu vào lịch sử"
                      />
                    </div>
                  )}
                  <div className="p-4 bg-secondary-container/20 text-on-secondary-container rounded-xl flex items-center gap-3">
                     <span className="material-symbols-outlined">info</span>
                     <p className="text-[10px] font-black uppercase tracking-widest leading-relaxed">
                       {selectedReg.gradeState === "UNLOCKED" && selectedReg.latestRevision?.unlockUntil
                         ? `Điểm được mở khóa đến ${fmtDateTime(selectedReg.latestRevision.unlockUntil)} — chỉ sửa được một lần, sau đó điểm tự khóa lại.`
                         : "Thang điểm 10, lẻ đến 0,25. Đề tài hoàn thành khi mọi sinh viên đã có điểm."}
                     </p>
                  </div>
              </div>
           )}
    
           {modalAction === "reject" && (
              <div className="space-y-1.5 animate-in slide-in-from-top-4 duration-300">
                <label htmlFor="registrationreviewmodal-textarea-107" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lý do từ chối (Gửi đến sinh viên)</label>
                <textarea id="registrationreviewmodal-textarea-107"
                  className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[100px]"
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Giải thích lý do từ chối để sinh viên hiểu rõ và cải thiện..."
                />
              </div>
           )}
        </div>
    
        {modalError && (
          <p className="mx-8 mb-4 rounded-xl bg-error/10 px-4 py-3 text-xs font-bold text-error" role="alert">{modalError}</p>
        )}
    
        {/* Modal Footer */}
        <div className="px-8 py-6 bg-surface-container-low/30 border-t border-outline-variant/5 flex justify-end gap-3">
           <button onClick={closeModal} className="px-6 py-3 bg-white dark:bg-slate-900 text-on-surface-variant font-black text-[11px] uppercase tracking-widest rounded-xl hover:bg-surface-container transition-all" disabled={!!loadingId}>
              Hủy bỏ
           </button>
           <button
             onClick={handleConfirm}
             className={`px-8 py-3 rounded-xl font-black text-[11px] uppercase tracking-widest transition-all shadow-lg ${
                modalAction === "reject" 
                ? "bg-error text-on-primary shadow-error/20" 
                : "bg-primary text-on-primary shadow-primary/20"
             } hover:scale-105 active:scale-95 disabled:opacity-50`}
             disabled={!!loadingId}
           >
             {loadingId === selectedReg.id ? "Đang xử lý..." : modalAction === "approve" ? "Duyệt ngay" : modalAction === "reject" ? "Từ chối ngay" : modalAction === "revision" ? "Gửi yêu cầu" : "Lưu kết quả"}
           </button>
        </div>
      </div>
    </div>
  );
}
