/** Tab "stats" của DepartmentAdminPage (tách ở P3-11; dữ liệu + xử lý ở useDepartmentAdmin). */
export default function StatsTab({ deptStats, fillRateLabel }) {
    return (
                    <div className="p-10 space-y-12 animate-in fade-in duration-500">
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6 text-center">
                            {[
                                { label: 'SỐ ĐỢT (WORKSPACE)', value: deptStats?.totalWorkspaces, icon: 'hub', color: 'text-primary' },
                                { label: 'TỔNG NHÂN SỰ KHOA', value: deptStats?.totalUsers, icon: 'group', color: 'text-secondary' },
                                { label: 'SINH VIÊN HOẠT ĐỘNG', value: deptStats?.totalStudents, icon: 'person_outline', color: 'text-on-surface' },
                                { label: 'DANH MỤC ĐỀ TÀI', value: deptStats?.totalTopics, icon: 'inventory', color: 'text-amber-500' },
                                { label: 'ĐỀ TÀI ĐANG MỞ', value: deptStats?.openTopics, icon: 'bolt', color: 'text-green-500' }
                            ].map((s, idx) => (
                                <div key={idx} className="p-8 bg-surface-container-low/20 rounded-[2.5rem] border border-outline-variant/5 shadow-sm group hover:scale-[1.05] transition-all duration-300">
                                   <div className={`w-12 h-12 rounded-2xl ${s.color} bg-current/10 flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform`}>
                                      <span className="material-symbols-outlined text-2xl">{s.icon}</span>
                                   </div>
                                   <div className="text-3xl font-black text-on-surface font-headline mb-2">{s.value ?? 0}</div>
                                   <p className="text-[9px] font-black uppercase tracking-widest text-outline leading-tight">{s.label}</p>
                                </div>
                            ))}
                        </div>
                        
                        <div className="bg-primary/5 p-10 rounded-[3rem] border border-primary/10 flex flex-col md:flex-row items-center gap-12">
                           <div className="flex-1 space-y-4">
                              <h3 className="text-2xl font-black text-on-surface uppercase tracking-tight leading-none">Báo cáo Tổng quát Đơn vị</h3>
                              <p className="text-sm font-medium text-outline leading-relaxed">
                                 Các đề tài của khoa có tổng <strong>{deptStats?.capacityTotal ?? 0}</strong> chỗ, đã duyệt <strong>{deptStats?.approvedTotal ?? 0}</strong> sinh viên.
                              </p>
                              <div className="pt-4 flex gap-4">
                                 <div className="px-6 py-3 bg-white dark:bg-slate-900 rounded-2xl border border-outline-variant/10 shadow-sm flex flex-col">
                                    <span className="text-[8px] font-black uppercase tracking-widest text-outline">TỈ LỆ LẤP ĐẦY</span>
                                    <span className="text-lg font-black text-primary">{fillRateLabel}</span>
                                 </div>
                                 <div className="px-6 py-3 bg-white dark:bg-slate-900 rounded-2xl border border-outline-variant/10 shadow-sm flex flex-col">
                                    <span className="text-[8px] font-black uppercase tracking-widest text-outline">GIẢNG VIÊN CHƯA ĐƯỢC PHÂN CÔNG</span>
                                    <span className="text-lg font-black text-secondary">{deptStats?.lecturersWithoutAssignment ?? 0}</span>
                                 </div>
                              </div>
                           </div>
                           <div
                              className="w-48 h-48 rounded-full flex items-center justify-center relative"
                              style={{ background: `conic-gradient(var(--color-primary, #1e3a8a) ${(deptStats?.fillRate ?? 0) * 3.6}deg, rgba(30,58,138,0.1) 0deg)` }}
                              role="img"
                              aria-label={`Tỉ lệ lấp đầy ${fillRateLabel}`}
                           >
                              <div className="w-32 h-32 rounded-full bg-white dark:bg-slate-900 flex items-center justify-center">
                                 <span className="text-2xl font-black text-primary">{fillRateLabel}</span>
                              </div>
                           </div>
                        </div>
                    </div>
                
    );
}
