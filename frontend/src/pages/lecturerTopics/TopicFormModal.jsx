import { MIN_CAPACITY, MAX_CAPACITY } from "./topicForm";
import useDialog from "../../hooks/useDialog";

/** Tách từ LecturerTopicsPage.jsx (P3-11). */
export default function TopicFormModal({ closeModal, editing, form, handleSave, openWorkspaces, setForm }) {
  const { panelRef, titleId } = useDialog(closeModal);
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-in fade-in duration-300">
       <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={closeModal} aria-hidden="true"></div>
       <div ref={panelRef} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1} className="bg-white dark:bg-slate-900 w-full max-w-2xl rounded-[2.5rem] shadow-2xl z-10 overflow-hidden outline-none animate-in zoom-in-95 duration-300">
          {/* Modal Header */}
          <div className="px-10 py-8 border-b border-outline-variant/5 bg-surface-container-low/30 relative">
             <div>
                <span className="text-[10px] font-black uppercase tracking-widest text-primary mb-1 block">Đề tài hướng dẫn</span>
                <h2 id={titleId} className="text-2xl font-black text-on-surface uppercase tracking-tight font-headline leading-none">
                   {editing ? "Cập nhật đề tài" : "Đề xuất đề tài mới"}
                </h2>
             </div>
             <button aria-label="Đóng" onClick={closeModal} className="absolute top-8 right-10 p-2.5 hover:bg-surface-container-high rounded-2xl transition-colors">
                <span className="material-symbols-outlined">close</span>
             </button>
          </div>
    
          {/* Modal Body */}
          <div className="p-10 space-y-8">
             <div className="space-y-2">
                <label htmlFor="topicformmodal-input-28" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tiêu đề nghiên cứu</label>
                <input id="topicformmodal-input-28"
                  className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-black transition-all duration-300 outline-none"
                  value={form.title}
                  onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                  maxLength={255}
                  placeholder="VD: Ứng dụng AI trong phân tích dữ liệu rác thải nhựa..."
                />
             </div>
             
             <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-2 sm:col-span-2">
                   <label htmlFor="topicformmodal-select-45" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Đợt đồ án</label>
                   {editing ? (
                     <div className="w-full px-6 py-4 bg-surface-container-low rounded-2xl text-sm font-bold text-on-surface-variant">
                       {editing.workspaceName || "—"}
                     </div>
                   ) : (
                     <select id="topicformmodal-select-45"
                       className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-bold outline-none"
                       value={form.workspaceId}
                       onChange={(e) => setForm((f) => ({ ...f, workspaceId: e.target.value }))}
                       required
                     >
                       <option value="">{openWorkspaces.length ? "— Chọn đợt —" : "Chưa có đợt nào đang mở nhận đề tài"}</option>
                       {openWorkspaces.map((w) => (
                         <option key={w.id} value={w.id}>{w.name}{w.semester ? ` (${w.semester})` : ""}</option>
                       ))}
                     </select>
                   )}
                </div>
                <div className="space-y-2">
                   <label htmlFor="topicformmodal-input-60" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Số sinh viên tối đa</label>
                   <input id="topicformmodal-input-60"
                     type="number"
                     min={MIN_CAPACITY}
                     max={MAX_CAPACITY}
                     step={1}
                     className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-black outline-none"
                     value={form.capacity}
                     onChange={(e) => setForm((f) => ({ ...f, capacity: e.target.value }))}
                   />
                </div>
             </div>
    
             <div className="space-y-2">
                <label htmlFor="topicformmodal-textarea-74" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mô tả chi tiết & Yêu cầu</label>
                <textarea id="topicformmodal-textarea-74"
                  className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-bold transition-all duration-300 outline-none min-h-[180px]"
                  rows={6}
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  maxLength={10000}
                  placeholder="Mô tả mục tiêu, công nghệ sử dụng, và các yêu cầu đối với sinh viên..."
                />
             </div>
    
             <div className="flex items-center gap-4 p-5 bg-primary/5 rounded-2xl border border-primary/10">
                <div className="w-10 h-10 rounded-xl bg-primary text-on-primary flex items-center justify-center shrink-0">
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
             <button onClick={closeModal} className="px-8 py-3.5 bg-white dark:bg-slate-900 text-on-surface-variant font-black text-xs uppercase tracking-widest rounded-2xl hover:bg-surface-container transition-all">
                Hủy bỏ
             </button>
             <button
               onClick={handleSave}
               className="px-10 py-3.5 bg-primary text-on-primary rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all"
             >
               {editing ? "Lưu thay đổi" : "Khởi tạo đề tài"}
             </button>
          </div>
       </div>
    </div>
  );
}
