import PasswordChecklist, { isPasswordValid } from "../../components/PasswordChecklist";

/** Tách từ LoginPage.jsx (P3-11). */
export default function ChangePasswordStep({ backToLogin, confirmPassword, loading, newPassword, setConfirmPassword, setNewPassword, username }) {
  return (
    <>
      <div className="space-y-3 text-center">
        <div className="w-16 h-16 bg-secondary-container text-on-secondary-container rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span className="material-symbols-outlined text-3xl">key</span>
        </div>
        <h2 className="text-xl font-black text-primary uppercase tracking-widest leading-none">Đặt mật khẩu mới</h2>
        <p className="text-xs text-outline font-medium leading-relaxed">
          Bạn đang dùng mật khẩu tạm do quản trị viên cấp. Hãy đặt mật khẩu của riêng bạn để tiếp tục.
        </p>
      </div>
      <div className="space-y-1.5">
        <label htmlFor="changepasswordstep-input-18" className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mật khẩu mới</label>
        <input id="changepasswordstep-input-18"
          type="password"
          className="w-full px-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
        <PasswordChecklist password={newPassword} username={username} />
      </div>
      <div className="space-y-1.5">
        <label htmlFor="changepasswordstep-input-30" className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Nhập lại mật khẩu mới</label>
        <input id="changepasswordstep-input-30"
          type="password"
          className="w-full px-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
      </div>
      <button
        className="w-full py-4 bg-primary text-on-primary rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50"
        disabled={loading || !isPasswordValid(newPassword, username) || !confirmPassword}
        type="submit"
      >
        {loading ? "Đang xử lý..." : "Đổi mật khẩu và tiếp tục"}
      </button>
      <button
        className="w-full text-[10px] font-black text-outline uppercase tracking-widest hover:text-primary transition-colors"
        type="button"
        onClick={() => backToLogin()}
      >
        Quay lại đăng nhập
      </button>
    </>
  );
}
