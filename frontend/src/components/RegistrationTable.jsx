// src/components/RegistrationTable.jsx
export default function RegistrationTable({ data, onApproveClick, onRejectClick, onGradeClick, loadingId }) {
  return (
    <div className="overflow-hidden rounded-[2rem] border border-outline-variant/10 shadow-sm bg-white dark:bg-slate-900">
      <div className="overflow-x-auto no-scrollbar">
        <table className="w-full text-left border-collapse min-w-[1000px]">
          <thead className="bg-surface-container-low/30 border-b border-outline-variant/5">
            <tr>
              <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-outline">Định danh</th>
              <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-outline">Thông tin Sinh viên</th>
              <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-outline">Thời gian</th>
              <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-outline">Người duyệt</th>
              <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-outline">Trạng thái</th>
              <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-outline">Kết quả</th>
              <th className="px-8 py-5 text-right text-[10px] font-black uppercase tracking-widest text-outline">Hành động</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/5">
            {data.length === 0 && (
              <tr>
                <td colSpan="7" className="px-8 py-24 text-center">
                  <div className="flex flex-col items-center gap-4 opacity-30 grayscale select-none">
                    <span className="material-symbols-outlined text-6xl">person_search</span>
                    <p className="text-xs font-black uppercase tracking-[0.2em] leading-relaxed">Chưa có sinh viên đăng ký<br/>tham gia đề tài này</p>
                  </div>
                </td>
              </tr>
            )}

            {data.map((r) => {
              const student = r.student;
              const user = student?.user;

              return (
                <tr key={r.id} className="group hover:bg-primary/[0.01] transition-all duration-300">
                  <td className="px-8 py-5">
                     <span className="text-xs font-black text-primary tracking-widest">#{r.id}</span>
                  </td>
                  <td className="px-8 py-5">
                    <div className="flex items-center gap-3">
                       <div className="w-9 h-9 rounded-xl bg-secondary/10 text-secondary flex items-center justify-center font-black text-xs shrink-0">
                          {user?.fullName?.charAt(0)}
                       </div>
                       <div className="flex flex-col">
                         <span className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors">{user?.fullName}</span>
                         <span className="text-[10px] font-bold text-outline uppercase tracking-tight mt-0.5">
                           {student?.studentCode} • {student?.className}
                         </span>
                       </div>
                    </div>
                  </td>
                  <td className="px-8 py-5">
                     <div className="flex flex-col">
                      <span className="text-[11px] font-black text-on-surface uppercase tracking-tight">{new Date(r.registeredAt).toLocaleDateString()}</span>
                      <span className="text-[9px] text-outline font-black uppercase tracking-widest opacity-60">
                         {new Date(r.registeredAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  </td>
                  <td className="px-8 py-5">
                     <div className="flex flex-col">
                      <span className="text-[11px] font-black text-secondary uppercase tracking-tight">{r.reviewer?.user?.fullName || <span className="opacity-20 italic">Chưa xét</span>}</span>
                      {r.reviewedAt && (
                        <span className="text-[9px] text-outline font-black uppercase tracking-widest mt-0.5">{new Date(r.reviewedAt).toLocaleDateString()}</span>
                      )}
                    </div>
                  </td>
                  <td className="px-8 py-5">
                    {r.approved === null ? (
                      <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-surface-container text-outline text-[9px] font-black uppercase tracking-widest border border-outline-variant/10">
                         <span className="w-1.5 h-1.5 rounded-full bg-outline animate-pulse"></span>
                         Chờ duyệt
                      </div>
                    ) : r.approved === true ? (
                      <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-green-500/10 text-green-600 text-[9px] font-black uppercase tracking-widest border border-green-500/10">
                         <span className="material-symbols-outlined text-[14px]">verified</span>
                         Đã duyệt
                      </div>
                    ) : (
                      <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-error/10 text-error text-[9px] font-black uppercase tracking-widest border border-error/10" title={r.rejectReason || "Không có lý do chi tiết"}>
                         <span className="material-symbols-outlined text-[14px]">cancel</span>
                         Từ chối
                      </div>
                    )}
                  </td>
                  <td className="px-8 py-5">
                    {r.score !== null ? (
                      <span className="px-4 py-2 bg-secondary text-white rounded-xl text-xs font-black shadow-lg shadow-secondary/20">{r.score}</span>
                    ) : (
                      <span className="text-outline/30 font-black uppercase tracking-widest text-[9px]">Chưa chấm</span>
                    )}
                  </td>
                  <td className="px-8 py-5 text-right">
                    <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-all duration-300">
                      {r.approved === null && (
                        <>
                          <button
                            className="p-2.5 rounded-xl bg-primary text-white hover:scale-110 active:scale-95 transition-all shadow-xl shadow-primary/20"
                            disabled={loadingId === r.id}
                            onClick={() => onApproveClick(r)}
                            title="Chấp thuận đăng ký"
                          >
                            <span className="material-symbols-outlined text-lg">check</span>
                          </button>
                          <button
                            className="p-2.5 rounded-xl bg-error/5 text-error border border-error/10 hover:bg-error hover:text-white hover:scale-110 active:scale-95 transition-all"
                            disabled={loadingId === r.id}
                            onClick={() => onRejectClick(r)}
                            title="Từ chối đăng ký"
                          >
                            <span className="material-symbols-outlined text-lg">block</span>
                          </button>
                        </>
                      )}
                      {r.approved === true && (
                        <button
                          className="p-2.5 rounded-xl bg-secondary text-white hover:scale-110 active:scale-95 transition-all shadow-xl shadow-secondary/20"
                          disabled={loadingId === r.id}
                          onClick={() => onGradeClick && onGradeClick(r)}
                          title={r.score !== null ? "Điều chỉnh điểm số" : "Cập nhật điểm bảo vệ"}
                        >
                          <span className="material-symbols-outlined text-lg">{r.score !== null ? 'edit_square' : 'history_edu'}</span>
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
