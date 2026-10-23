import PasswordChecklist, { isPasswordValid } from "../../components/PasswordChecklist";

/** Tách từ LoginPage.jsx (P3-11). */
export default function ActivateStep({ activationCode, backToLogin, confirmPassword, loading, newPassword, setActivationCode, setConfirmPassword, setNewPassword, setUsername, username }) {
  return (
    <>
      <div className="space-y-3 text-center">
        <div className="w-16 h-16 bg-secondary-container text-on-secondary-container rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span className="material-symbols-outlined text-3xl">verified_user</span>
        </div>
        <h2 className="text-xl font-black text-primary uppercase tracking-widest leading-none">Kích hoạt tài khoản</h2>
        <p className="text-xs text-outline font-medium leading-relaxed">
          Nhập tên đăng nhập (hoặc email) cùng mã kích hoạt trong email quản trị viên đã gửi cho bạn, rồi đặt mật khẩu.
        </p>
      </div>
      <div className="space-y-1.5">
        <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Tên đăng nhập hoặc email</label>
        <input
          className="w-full px-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
      </div>
      <div className="space-y-1.5">
        <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mã kích hoạt (6 chữ số)</label>
        <input
          className="w-full text-center text-2xl tracking-[0.5em] font-black py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl transition-all duration-300 outline-none"
          value={activationCode}
          onChange={(e) => setActivationCode(e.target.value.replace(/\D/g, ""))}
          placeholder="000000"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          required
        />
      </div>
      <div className="space-y-1.5">
        <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mật khẩu mới</label>
        <input
          type="password"
          className="w-full px-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
        <PasswordChecklist password={newPassword} username={username} />
      </div>
      <div className="space-y-1.5">
        <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Nhập lại mật khẩu mới</label>
        <input
          type="password"
          className="w-full px-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
      </div>
      <button
        className="w-full py-4 bg-primary text-white rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50"
        disabled={loading || activationCode.length !== 6 || !isPasswordValid(newPassword, username) || !confirmPassword}
        type="submit"
      >
        {loading ? "Đang xử lý..." : "Kích hoạt"}
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
