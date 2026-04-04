import React, { useEffect, useState, useCallback, useMemo } from "react";
import { roleApi } from "../api/roleApi";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";

export default function RoleManagementPage() {
  const { user } = useAuth();
  const [roles, setRoles] = useState([]);
  const [privileges, setPrivileges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null);

  // Modal states
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [editingRole, setEditingRole] = useState(null);
  const [roleForm, setRoleForm] = useState({ name: "", description: "" });
  
  const [showConfirmDelete, setShowConfirmDelete] = useState(null); // roleId to delete
  const [actionLoading, setActionLoading] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [rData, pData] = await Promise.all([
        roleApi.getAllRoles(),
        roleApi.getAllPrivileges()
      ]);
      setRoles(rData || []);
      setPrivileges(pData || []);
    } catch (err) {
      setError("Không thể tải cấu hình quyền hạn từ máy chủ.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleTogglePrivilege = async (role, privilegeId) => {
    const currentPrivs = role.privileges || [];
    const hasPriv = currentPrivs.some(p => p.id === privilegeId);
    
    let newPrivIds;
    if (hasPriv) {
      newPrivIds = currentPrivs.filter(p => p.id !== privilegeId).map(p => p.id);
    } else {
      newPrivIds = [...currentPrivs.map(p => p.id), privilegeId];
    }

    try {
      await roleApi.updateRole(role.id, { 
        description: role.description, 
        privilegeIds: newPrivIds 
      });
      await loadData();
      setNotice({ type: "success", message: "Đã cập nhật quyền hạn thành công!" });
    } catch (err) {
      setNotice({ type: "danger", message: "Cập nhật quyền thất bại." });
    }
  };

  const handleSaveRole = async () => {
    if (!roleForm.name.trim()) {
      setNotice({ type: "warning", message: "Vui lòng nhập tên vai trò." });
      return;
    }
    setActionLoading(true);
    try {
      if (editingRole) {
        await roleApi.updateRole(editingRole.id, { 
           description: roleForm.description 
        });
        setNotice({ type: "success", message: "Đã cập nhật thông tin vai trò." });
      } else {
        await roleApi.createRole(roleForm);
        setNotice({ type: "success", message: "Đã tạo vai trò mới thành công!" });
      }
      setShowRoleModal(false);
      await loadData();
    } catch (err) {
      setNotice({ type: "danger", message: err.response?.data?.message || "Thao tác thất bại." });
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteRole = async () => {
    if (!showConfirmDelete) return;
    setActionLoading(true);
    try {
      await roleApi.deleteRole(showConfirmDelete);
      setNotice({ type: "success", message: "Đã xóa vai trò thành công." });
      setShowConfirmDelete(null);
      await loadData();
    } catch (err) {
      setNotice({ type: "danger", message: "Không thể xóa vai trò này." });
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-20">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Cấu hình hệ thống</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none italic">
            Quản lý Quyền hạn (RBAC)
          </h1>
          <p className="text-sm text-outline mt-3 font-medium max-w-2xl">
            Thiết lập ma trận quyền hạn cho từng cấp bậc. Các thay đổi sẽ có hiệu lực ngay lập tức cho người dùng sau khi họ đăng nhập lại.
          </p>
        </div>
        <button 
          onClick={() => {
            setEditingRole(null);
            setRoleForm({ name: "", description: "" });
            setShowRoleModal(true);
          }}
          className="px-8 py-3.5 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all flex items-center gap-2 group"
        >
          <span className="material-symbols-outlined text-lg group-hover:rotate-90 transition-transform">add</span>
          Thêm Vai trò mới
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

      {/* Matrix Table */}
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center p-32 opacity-40">
             <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary border-t-transparent"></div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface-container-low/50 border-b border-outline-variant/10">
                  <th className="px-10 py-8 min-w-[280px]">
                     <p className="text-[10px] font-black uppercase tracking-widest text-primary">Vai trò & Quyền hạn</p>
                     <p className="text-xs font-bold text-outline mt-1 italic">Click vào các ô để bật/tắt quyền</p>
                  </th>
                  {privileges.map(p => (
                    <th key={p.id} className="px-6 py-8 text-center min-w-[140px] group relative">
                       <span className="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">
                          {p.name.replace("_", " ")}
                       </span>
                       {/* Tooltip on hover */}
                       <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 p-3 bg-slate-900 text-white rounded-xl text-[10px] font-bold opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none shadow-2xl z-20">
                          {p.description || "Không có mô tả chi tiết cho quyền này."}
                          <div className="absolute top-full left-1/2 -translate-x-1/2 border-[6px] border-transparent border-t-slate-900"></div>
                       </div>
                    </th>
                  ))}
                  <th className="px-10 py-8 text-right font-black text-[10px] uppercase tracking-widest text-outline">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/10 text-sm">
                {roles.map(role => (
                  <tr key={role.id} className="group hover:bg-primary/[0.02] transition-colors">
                    <td className="px-10 py-8">
                       <p className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors">
                          {role.name}
                       </p>
                       <p className="text-[11px] text-outline mt-1.5 font-medium italic">
                          {role.description || "Chưa có mô tả chức năng."}
                       </p>
                    </td>
                    {privileges.map(priv => {
                      const hasPriv = role.privileges?.some(p => p.id === priv.id);
                      return (
                        <td key={priv.id} className="px-6 py-8 text-center">
                           <button 
                             onClick={() => handleTogglePrivilege(role, priv.id)}
                             disabled={role.name === 'ADMIN'}
                             className={`w-10 h-10 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                                hasPriv 
                                ? "bg-primary text-white shadow-lg shadow-primary/20 scale-110" 
                                : "bg-surface-container text-outline/30 hover:bg-surface-container-high"
                             } ${role.name === 'ADMIN' ? 'cursor-not-allowed opacity-100' : 'cursor-pointer'}`}
                           >
                              <span className="material-symbols-outlined text-lg">
                                 {hasPriv ? "check_circle" : "circle"}
                              </span>
                           </button>
                        </td>
                      );
                    })}
                    <td className="px-10 py-8 text-right">
                       <div className="flex justify-end gap-2">
                          <button 
                            onClick={() => {
                              setEditingRole(role);
                              setRoleForm({ name: role.name, description: role.description || "" });
                              setShowRoleModal(true);
                            }}
                            className="p-3 rounded-xl bg-surface-container hover:bg-primary-container hover:text-primary transition-all group/btn"
                            title="Sửa thông tin vai trò"
                          >
                             <span className="material-symbols-outlined text-lg">edit</span>
                          </button>
                          {role.name !== 'ADMIN' && role.name !== 'STUDENT' && (
                             <button 
                               onClick={() => setShowConfirmDelete(role.id)}
                               className="p-3 rounded-xl bg-surface-container hover:bg-error-container hover:text-error transition-all"
                               title="Xóa vai trò"
                             >
                                <span className="material-symbols-outlined text-lg">delete</span>
                             </button>
                          )}
                       </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Role Form Modal */}
      {showRoleModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-in fade-in duration-300">
          <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={() => setShowRoleModal(false)}></div>
          <div className="bg-white dark:bg-slate-900 w-full max-w-lg rounded-[2.5rem] shadow-2xl z-10 overflow-hidden animate-in zoom-in-95 duration-300">
             <div className="px-10 py-8 border-b border-outline-variant/5 bg-surface-container-low/30 relative">
                <h2 className="text-xl font-black text-on-surface uppercase tracking-tight">
                   {editingRole ? "Cập nhật vai trò" : "Tạo vai trò mới"}
                </h2>
                <button onClick={() => setShowRoleModal(false)} className="absolute top-7 right-8 p-2 hover:bg-surface-container-high rounded-xl transition-colors">
                   <span className="material-symbols-outlined">close</span>
                </button>
             </div>
             <div className="p-10 space-y-6">
                <div className="space-y-1.5">
                   <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên vai trò (Unique Key)</label>
                   <input
                     className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all outline-none disabled:opacity-50"
                     value={roleForm.name}
                     onChange={(e) => setRoleForm({ ...roleForm, name: e.target.value.toUpperCase() })}
                     disabled={!!editingRole}
                     placeholder="VD: RESEARCH_LEADER"
                   />
                </div>
                <div className="space-y-1.5">
                   <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mô tả chức vụ & ảnh hưởng</label>
                   <textarea
                     className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-bold transition-all outline-none min-h-[120px]"
                     value={roleForm.description}
                     onChange={(e) => setRoleForm({ ...roleForm, description: e.target.value })}
                     placeholder="Mô tả cụ thể người sở hữu vai trò này sẽ có trách nhiệm và quyền hạn gì..."
                   />
                </div>
             </div>
             <div className="px-10 py-8 bg-surface-container-low/30 border-t border-outline-variant/5 flex justify-end gap-3">
                <button onClick={() => setShowRoleModal(false)} className="px-6 py-3 bg-white text-on-surface-variant font-black text-[11px] uppercase tracking-widest rounded-xl hover:bg-surface-container transition-all">
                   Hủy
                </button>
                <button
                  onClick={handleSaveRole}
                  disabled={actionLoading}
                  className="px-8 py-3 bg-primary text-white rounded-xl font-black text-[11px] uppercase tracking-widest shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all disabled:opacity-50"
                >
                  {actionLoading ? "Đang lưu..." : "Lưu dữ liệu"}
                </button>
             </div>
          </div>
        </div>
      )}

      {/* Confirm Delete Modal */}
      {showConfirmDelete && (
         <div className="fixed inset-0 z-[110] flex items-center justify-center p-4 animate-in fade-in duration-300">
            <div className="absolute inset-0 bg-error/20 backdrop-blur-sm" onClick={() => setShowConfirmDelete(null)}></div>
            <div className="bg-white dark:bg-slate-900 w-full max-w-sm rounded-[2rem] shadow-2xl z-10 overflow-hidden text-center p-10 space-y-6">
               <div className="w-20 h-20 bg-error/10 text-error rounded-full flex items-center justify-center mx-auto">
                  <span className="material-symbols-outlined text-4xl">warning</span>
               </div>
               <div>
                  <h3 className="text-xl font-black text-on-surface">Xác nhận xóa vai trò?</h3>
                  <p className="text-sm text-outline mt-2 font-medium">Hành động này sẽ gỡ bỏ vai trò khỏi tất cả người dùng hiện có. Thao tác không thể hoàn tác.</p>
               </div>
               <div className="flex gap-3 pt-2">
                  <button onClick={() => setShowConfirmDelete(null)} className="flex-1 py-3.5 bg-surface-container text-on-surface-variant font-black text-xs uppercase tracking-widest rounded-2xl hover:bg-surface-container-high transition-all">
                     Hủy bỏ
                  </button>
                  <button 
                    onClick={handleDeleteRole}
                    disabled={actionLoading}
                    className="flex-1 py-3.5 bg-error text-white font-black text-xs uppercase tracking-widest rounded-2xl shadow-xl shadow-error/20 hover:scale-105 transition-all disabled:opacity-50"
                  >
                     {actionLoading ? "Đang xóa..." : "Đồng ý xóa"}
                  </button>
               </div>
            </div>
         </div>
      )}
    </div>
  );
}
