import PasswordChecklist from '../../components/PasswordChecklist';
import { getRoleLabel } from './helpers';

/** Tab "createUser" của AdminPage (tách ở P3-11; dữ liệu + xử lý ở useAdmin). */
export default function CreateUserTab({ roles, departments, academicClasses, userCreate, setUserCreate, createRoleName, createRoleKey, createUser }) {
    return (
                    <div className="p-10 space-y-10 animate-in slide-in-from-bottom-4 duration-500">
                        <div className="flex justify-between items-center bg-surface-container-low/30 p-6 rounded-[2rem] border border-outline-variant/5">
                            <div>
                                <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Thông tin tài khoản mới</h2>
                                <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Thiết lập định danh và phân quyền cơ sở</p>
                            </div>
                            <div className="px-5 py-2 bg-primary/10 text-primary rounded-xl text-[10px] font-black uppercase tracking-widest border border-primary/20">
                                {getRoleLabel(createRoleName || 'VAI TRÒ')}
                            </div>
                        </div>

                        <form onSubmit={createUser} className="space-y-8">
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                                <div className="space-y-1.5">
                                    <label htmlFor="createusertab-input-22" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên đăng nhập (Username)</label>
                                    <input id="createusertab-input-22" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.username} onChange={(e) => setUserCreate(s => ({ ...s, username: e.target.value }))} placeholder="VD: 202160...." required />
                                </div>
                                <div className="space-y-1.5">
                                    <label htmlFor="createusertab-input-26" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mật khẩu tạm (không bắt buộc)</label>
                                    <input id="createusertab-input-26" type="password" autoComplete="new-password" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.password} onChange={(e) => setUserCreate(s => ({ ...s, password: e.target.value }))} placeholder="Để trống = gửi mã kích hoạt qua email" />
                                    <p className="text-[10px] text-outline font-bold ml-1 leading-snug">
                                        {userCreate.password
                                            ? 'Người dùng sẽ bị buộc đổi mật khẩu ở lần đăng nhập đầu.'
                                            : 'Người dùng nhận mã kích hoạt qua email (cần nhập email) rồi tự đặt mật khẩu.'}
                                    </p>
                                    {userCreate.password && <PasswordChecklist password={userCreate.password} username={userCreate.username} />}
                                </div>
                                <div className="space-y-1.5 lg:col-span-1">
                                    <label htmlFor="createusertab-input-36" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Họ và tên đầy đủ</label>
                                    <input id="createusertab-input-36" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.fullName} onChange={(e) => setUserCreate(s => ({ ...s, fullName: e.target.value }))} placeholder="Nguyễn Văn A" required />
                                </div>
                                
                                <div className="space-y-1.5">
                                    <label htmlFor="createusertab-input-41" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Địa chỉ Email</label>
                                    <input id="createusertab-input-41" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.email} onChange={(e) => setUserCreate(s => ({ ...s, email: e.target.value }))} placeholder="example@email.com" />
                                </div>
                                <div className="space-y-1.5">
                                    <label htmlFor="createusertab-input-45" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Số điện thoại</label>
                                    <input id="createusertab-input-45" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.phone} onChange={(e) => setUserCreate(s => ({ ...s, phone: e.target.value }))} placeholder="090..." />
                                </div>
                                <div className="space-y-1.5">
                                    <label htmlFor="createusertab-select-49" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Phân quyền vai trò</label>
                                    <select id="createusertab-select-49" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-xs font-black uppercase tracking-widest transition-all duration-300 outline-none cursor-pointer" value={userCreate.roleId} onChange={(e) => setUserCreate(s => ({ ...s, roleId: e.target.value }))}>
                                        <option value="">Chọn vai trò...</option>
                                        {roles.map(r => <option key={r.id} value={String(r.id)}>{getRoleLabel(r.name)}</option>)}
                                    </select>
                                </div>

                                <div className="space-y-1.5">
                                    <label htmlFor="createusertab-select-57" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Đơn vị Khoa quản lý</label>
                                    <select id="createusertab-select-57" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-2xl text-xs font-black uppercase tracking-widest transition-all duration-300 outline-none cursor-pointer disabled:opacity-30" value={userCreate.departmentId} onChange={(e) => setUserCreate(s => ({ ...s, departmentId: e.target.value }))} disabled={createRoleKey === 'ADMIN'}>
                                        <option value="">Chọn khoa...</option>
                                        {departments.map(d => <option key={d.id} value={String(d.id)}>{d.code} - {d.name}</option>)}
                                    </select>
                                </div>

                                <div className="flex items-center gap-4 lg:pt-8">
                                   <button 
                                      type="button" 
                                      onClick={() => setUserCreate(s => ({ ...s, active: !s.active }))}
                                      className={`flex items-center gap-3 px-6 py-4 rounded-2xl border transition-all ${
                                        userCreate.active 
                                        ? "bg-primary/5 border-primary text-primary" 
                                        : "bg-surface-container border-outline-variant/20 text-outline"
                                      }`}
                                   >
                                      <span className="material-symbols-outlined">{userCreate.active ? 'check_circle' : 'do_not_disturb_on'}</span>
                                      <span className="text-[10px] font-black uppercase tracking-widest">{userCreate.active ? 'Kích hoạt' : 'Bản nháp (Khóa)'}</span>
                                   </button>
                                </div>
                            </div>

                            {createRoleKey === 'STUDENT' && (
                                <div className="p-8 bg-surface-container-low/30 rounded-[2rem] border border-outline-variant/10 grid grid-cols-1 md:grid-cols-3 gap-8 animate-in zoom-in-95 duration-300">
                                   <div className="space-y-1.5">
                                       <label htmlFor="createusertab-input-83" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mã số sinh viên (MSSV)</label>
                                       <input id="createusertab-input-83" className="w-full px-6 py-4 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.studentCode} onChange={(e) => setUserCreate(s => ({ ...s, studentCode: e.target.value }))} required />
                                   </div>
                                   <div className="space-y-1.5">
                                       <label htmlFor="createusertab-select-87" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn lớp học cố định</label>
                                       <select id="createusertab-select-87" className="w-full px-6 py-4 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-2xl text-[10px] font-black uppercase tracking-widest outline-none" value={userCreate.academicClassId} onChange={(e) => setUserCreate(s => ({ ...s, academicClassId: e.target.value }))}>
                                           <option value="">-- Chọn danh sách --</option>
                                           {academicClasses.filter(c => c.active).map(c => <option key={c.id} value={String(c.id)}>{c.code} - {c.name}</option>)}
                                       </select>
                                   </div>
                                   <div className="space-y-1.5">
                                       <label htmlFor="createusertab-input-94" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên lớp học tùy chỉnh</label>
                                       <input id="createusertab-input-94" className="w-full px-6 py-4 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.className} onChange={(e) => setUserCreate(s => ({ ...s, className: e.target.value }))} placeholder="VD: CNTT1-K25" />
                                   </div>
                                </div>
                            )}

                            {createRoleKey === 'LECTURER' && (
                                <div className="p-8 bg-surface-container-low/30 rounded-[2rem] border border-outline-variant/10 grid grid-cols-1 md:grid-cols-3 gap-8 animate-in zoom-in-95 duration-300">
                                   <div className="space-y-1.5 col-span-1">
                                       <label htmlFor="createusertab-input-103" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Học hàm / Học vị</label>
                                       <input id="createusertab-input-103" className="w-full px-6 py-4 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.degree} onChange={(e) => setUserCreate(s => ({ ...s, degree: e.target.value }))} placeholder="VD: Tiến sĩ" />
                                   </div>
                                   <div className="space-y-1.5 col-span-2">
                                       <label htmlFor="createusertab-input-107" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lĩnh vực chuyên môn nghiên cứu</label>
                                       <input id="createusertab-input-107" className="w-full px-6 py-4 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.speciality} onChange={(e) => setUserCreate(s => ({ ...s, speciality: e.target.value }))} placeholder="VD: Trí tuệ nhân tạo, Big Data..." />
                                   </div>
                                </div>
                            )}

                            <div className="pt-8 border-t border-outline-variant/5 flex justify-end">
                                <button type="submit" className="px-12 py-4 bg-primary text-on-primary rounded-2xl font-black text-xs uppercase tracking-[0.2em] shadow-2xl shadow-primary/30 hover:scale-105 active:scale-95 transition-all flex items-center gap-3">
                                   <span className="material-symbols-outlined">how_to_reg</span>
                                   Khởi tạo tài khoản hệ thống
                                </button>
                            </div>
                        </form>
                    </div>
                
    );
}
