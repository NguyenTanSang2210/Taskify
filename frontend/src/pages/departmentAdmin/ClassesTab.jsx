/** Tab "classes" của DepartmentAdminPage (tách ở P3-11; dữ liệu + xử lý ở useDepartmentAdmin). */
export default function ClassesTab({ classes, classCreate, setClassCreate, createClass }) {
    return (
                    <div className="p-10 space-y-12 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/30 p-8 rounded-[2rem] border border-outline-variant/5 grid grid-cols-1 md:grid-cols-12 gap-8 items-end">
                            <div className="md:col-span-3 space-y-1.5">
                                <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mã lớp định danh</label>
                                <input className="w-full px-6 py-4 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={classCreate.code} onChange={(e) => setClassCreate(s => ({ ...s, code: e.target.value }))} placeholder="VD: DHCNTT16A" />
                            </div>
                            <div className="md:col-span-6 space-y-1.5">
                                <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên lớp học thuật</label>
                                <input className="w-full px-6 py-4 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={classCreate.name} onChange={(e) => setClassCreate(s => ({ ...s, name: e.target.value }))} placeholder="VD: CNTT Khóa 16 - Hệ CLC" />
                            </div>
                            <div className="md:col-span-3">
                                <button onClick={createClass} className="w-full py-4 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all">Khởi tạo lớp mới</button>
                            </div>
                        </section>

                        <section className="space-y-6">
                            <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Danh sách Lớp học hiện có ({classes.length})</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {classes.map(c => (
                                    <div key={c.id} className="p-6 bg-surface-container-low/20 rounded-[2rem] border border-outline-variant/5 flex items-center justify-between group hover:border-primary/20 transition-all">
                                        <div>
                                           <p className="text-[10px] font-black text-primary uppercase tracking-widest mb-1">{c.code}</p>
                                           <h4 className="text-sm font-black uppercase text-on-surface tracking-tight group-hover:text-primary transition-colors">{c.name}</h4>
                                        </div>
                                        <div className={`px-3 py-1.5 rounded-xl text-[8px] font-black uppercase tracking-widest ${c.active ? 'bg-green-500/10 text-green-600' : 'bg-surface-container text-outline'}`}>
                                           {c.active ? 'Sẵn sàng' : 'Không dùng'}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </section>
                    </div>
                
    );
}
