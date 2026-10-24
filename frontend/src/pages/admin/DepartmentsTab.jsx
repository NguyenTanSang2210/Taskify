/** Tab "departments" của AdminPage (tách ở P3-11; dữ liệu + xử lý ở useAdmin). */
export default function DepartmentsTab({ departments, departmentCreate, setDepartmentCreate, createDepartment }) {
    return (
                    <div className="p-10 space-y-12 animate-in fade-in duration-500">
                        <section className="space-y-6">
                            <div className="bg-surface-container-low/30 p-8 rounded-[2.5rem] border border-outline-variant/10">
                               <div className="flex items-center gap-4 mb-8 text-primary">
                                  <span className="material-symbols-outlined text-3xl">domain_add</span>
                                  <h3 className="text-xl font-black uppercase tracking-tight font-headline">Thiết lập Khoa/Đơn vị mới</h3>
                               </div>
                               <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-end">
                                   <div className="md:col-span-3 space-y-1.5">
                                       <label htmlFor="departmentstab-input-14" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mã định danh khoa</label>
                                       <input id="departmentstab-input-14" className="w-full px-6 py-4 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={departmentCreate.code} onChange={(e) => setDepartmentCreate(s => ({ ...s, code: e.target.value }))} placeholder="VD: CNTT, CK..." />
                                   </div>
                                   <div className="md:col-span-6 space-y-1.5">
                                       <label htmlFor="departmentstab-input-18" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên đơn vị đầy đủ</label>
                                       <input id="departmentstab-input-18" className="w-full px-6 py-4 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={departmentCreate.name} onChange={(e) => setDepartmentCreate(s => ({ ...s, name: e.target.value }))} placeholder="VD: Công nghệ Thông tin" />
                                   </div>
                                   <div className="md:col-span-3">
                                       <button onClick={createDepartment} className="w-full py-4 bg-primary text-on-primary rounded-2xl font-black text-[10px] uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all">Xác nhận tạo khoa</button>
                                   </div>
                               </div>
                            </div>
                        </section>

                        <section className="space-y-6">
                            <div className="flex justify-between items-center px-4">
                               <h3 className="text-sm font-black text-outline uppercase tracking-widest">Danh mục đơn vị hiện hành ({departments.length})</h3>
                            </div>
                            <div className="overflow-x-auto bg-surface-container-low/10 rounded-[2.5rem] border border-outline-variant/5">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="bg-surface-container-low/50">
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Mã Khoa</th>
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Tên đơn vị chi tiết</th>
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Trạng thái</th>
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Id</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-outline-variant/5">
                                        {departments.map(d => (
                                           <tr key={d.id} className="group hover:bg-primary/[0.01] transition-colors">
                                              <td className="px-8 py-5">
                                                 <span className="text-sm font-black text-primary uppercase tracking-widest">{d.code}</span>
                                              </td>
                                              <td className="px-8 py-5">
                                                 <p className="text-base font-black text-on-surface uppercase tracking-tight">{d.name}</p>
                                              </td>
                                              <td className="px-8 py-5">
                                                 <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest ${d.active ? 'bg-green-500/10 text-green-700 dark:text-green-400' : 'bg-surface-container text-outline'}`}>
                                                    {d.active ? 'Đang hoạt động' : 'Tạm ngưng'}
                                                 </div>
                                              </td>
                                              <td className="px-8 py-5 text-right font-black text-[10px] text-outline">#{d.id}</td>
                                           </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </div>
                
    );
}
