/** Tách từ LoginPage.jsx (P3-11). */
export default function ForgotStep({ backToLogin, loading, setUsername, username }) {
  return (
    <>
      <div className="space-y-3 text-center">
        <div className="w-16 h-16 bg-secondary-container text-on-secondary-container rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span className="material-symbols-outlined text-3xl">lock_reset</span>
        </div>
        <h2 className="text-xl font-black text-primary uppercase tracking-widest leading-none">Quên mật khẩu</h2>
        <p className="text-xs text-outline font-medium leading-relaxed">
          Nhập tên đăng nhập (hoặc email). Mã đặt lại mật khẩu sẽ được gửi tới email đăng ký của tài khoản, có hiệu lực 10 phút.
        </p>
      </div>
      <div className="space-y-1.5">
        <label htmlFor="forgotstep-input-16" className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Tên đăng nhập hoặc email</label>
        <input id="forgotstep-input-16"
          className="w-full px-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoFocus
          required
        />
      </div>
      <button className="w-full py-4 bg-primary text-on-primary rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50" disabled={loading || !username.trim()} type="submit">
        {loading ? "Đang gửi..." : "Gửi mã đặt lại"}
      </button>
      <button className="w-full text-[10px] font-black text-outline uppercase tracking-widest hover:text-primary transition-colors" type="button" onClick={() => backToLogin()}>
        Quay lại đăng nhập
      </button>
    </>
  );
}
