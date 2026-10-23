/** Tách từ LoginPage.jsx (P3-11). */
export default function LoginStep({ loading, password, setError, setPassword, setStep, setSuccessMsg, setUsername, username }) {
  return (
    <>
      <div className="space-y-1.5">
        <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Tên đăng nhập / MSSV</label>
        <div className="relative group">
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">person</span>
          <input
            className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Nhập mã sinh viên hoặc username"
            required
          />
        </div>
      </div>
    
      <div className="space-y-1.5">
        <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mật khẩu</label>
        <div className="relative group">
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">lock</span>
          <input
            type="password"
            className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
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
    
      <div className="relative my-8">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-outline-variant/10"></div>
        </div>
        <div className="relative flex justify-center text-[10px] font-black tracking-[0.3em] uppercase">
          <span className="bg-surface px-4 text-outline/50 font-manrope">HOẶC TIẾP TỤC VỚI</span>
        </div>
      </div>
    
      <div className="grid grid-cols-2 gap-4">
        <button type="button" className="flex items-center justify-center gap-2 py-3 bg-white dark:bg-surface-container border border-outline-variant/10 rounded-xl hover:bg-surface-container-low transition-all active:scale-95 shadow-sm">
          <img alt="Google" className="w-5 h-5" src="https://www.gstatic.com/images/branding/product/2x/googleg_48dp.png" />
          <span className="text-xs font-black uppercase tracking-widest text-on-surface">Google</span>
        </button>
        <button type="button" className="flex items-center justify-center gap-2 py-3 bg-white dark:bg-surface-container border border-outline-variant/10 rounded-xl hover:bg-surface-container-low transition-all active:scale-95 shadow-sm">
           <span className="material-symbols-outlined text-xl">brand_family</span>
          <span className="text-xs font-black uppercase tracking-widest text-on-surface">SSO</span>
        </button>
      </div>
    
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
