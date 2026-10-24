/** Tách từ LoginPage.jsx (P3-11). */
export default function LoginStep({ loading, password, setError, setPassword, setStep, setSuccessMsg, setUsername, username }) {
  return (
    <>
      <div className="space-y-1.5">
        <label htmlFor="loginstep-input-9" className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Tên đăng nhập / MSSV</label>
        <div className="relative group">
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">person</span>
          <input id="loginstep-input-9"
            className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Nhập mã sinh viên hoặc username"
            required
          />
        </div>
      </div>
    
      <div className="space-y-1.5">
        <label htmlFor="loginstep-input-23" className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mật khẩu</label>
        <div className="relative group">
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">lock</span>
          <input id="loginstep-input-23"
            type="password"
            className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl text-sm font-bold transition-all duration-300 outline-none"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </div>
      </div>
    
      <div className="flex items-center justify-between text-xs">
        <label className="flex items-center gap-2 cursor-pointer group">
          <input className="w-4 h-4 rounded border-outline-variant text-primary focus:ring-primary/20 cursor-pointer" type="checkbox" />
          <span className="text-outline font-bold group-hover:text-on-surface transition-colors uppercase tracking-tighter">Ghi nhớ tôi</span>
        </label>
        <button
          type="button"
          className="text-primary font-black uppercase tracking-widest hover:underline decoration-2 underline-offset-4"
          onClick={() => { setError(""); setSuccessMsg(""); setStep("forgot"); }}
        >
          Quên mật khẩu?
        </button>
      </div>
    
      <button 
        className="w-full py-4 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl font-black text-sm uppercase tracking-[0.2em] shadow-xl shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50" 
        disabled={loading} 
        type="submit"
      >
        {loading ? "Đang xác thực..." : "Đăng nhập"}
      </button>
    
      <div className="mt-10 text-center">
        <p className="text-on-surface-variant text-xs font-bold uppercase tracking-widest">
          Tài khoản do quản trị viên cấp.
          <button
            className="text-primary font-black ml-2 hover:underline decoration-2 underline-offset-4 transition-all"
            type="button"
            onClick={() => { setError(""); setSuccessMsg(""); setStep("activate"); }}
          >
            Kích hoạt tài khoản
          </button>
        </p>
      </div>
    </>
  );
}
