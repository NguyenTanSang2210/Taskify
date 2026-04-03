import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { authApi } from "../api/auth";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  
  const [isRegistering, setIsRegistering] = useState(false);
  const [regFullName, setRegFullName] = useState("");
  const [regUsername, setRegUsername] = useState("");
  const [regPassword, setRegPassword] = useState("");
  const [regConfirmPassword, setRegConfirmPassword] = useState("");

  const [otpRequired, setOtpRequired] = useState(false);
  const [otpCode, setOtpCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMsg("");

    if (regPassword !== regConfirmPassword) {
      setError("Mật khẩu xác nhận không khớp");
      return;
    }

    setLoading(true);
    try {
      await authApi.register({
        username: regUsername,
        password: regPassword,
        fullName: regFullName,
        role: "STUDENT"
      });
      setSuccessMsg("Đăng ký thành công! Vui lòng đăng nhập.");
      setIsRegistering(false);
      setUsername(regUsername);
      setPassword("");
    } catch (err) {
      setError(err.message || "Đăng ký thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const data = await authApi.login(username, password);
      const userData = {
        id: data.userId,
        username: data.username,
        fullName: data.fullName,
        role: data.role,
      };

      if (data.otpRequired) {
        localStorage.setItem("temp_user", JSON.stringify(userData));
        localStorage.setItem("temp_token", data.token);
        setOtpRequired(true);
        return;
      }

      login(userData, data.token);
      if (data.role === "LECTURER") navigate("/lecturer");
      else navigate("/");
    } catch (err) {
      setError(err.message || "Đăng nhập thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const resp = await authApi.verifyOtp(username, otpCode);
      if (!resp.verified) {
        throw new Error("OTP không hợp lệ");
      }
      
      const tempUser = JSON.parse(localStorage.getItem("temp_user"));
      const tempToken = localStorage.getItem("temp_token");

      if (tempUser && tempToken) {
          login(tempUser, tempToken);
          localStorage.removeItem("temp_user");
          localStorage.removeItem("temp_token");
          if (tempUser.role === "LECTURER") navigate("/lecturer");
          else navigate("/");
      } else {
          navigate("/login");
      }
    } catch (err) {
      setError(err.message || "Xác thực OTP thất bại");
    } finally {
      setLoading(false);
    }
  };

  const renderLoginForm = () => (
    <div className="w-full max-w-md z-10 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="bg-surface p-8 md:p-10 rounded-2xl shadow-[0_20px_50px_-12px_rgba(0,21,42,0.12)] border border-outline-variant/10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-primary rounded-xl mb-4 shadow-xl shadow-primary/20">
            <span className="material-symbols-outlined text-white text-3xl" style={{ fontVariationSettings: "'FILL' 1" }}>
              school
            </span>
          </div>
          <h1 className="text-3xl font-black font-headline tracking-tight text-primary mb-2">Chào mừng trở lại</h1>
          <p className="text-on-surface-variant text-sm font-medium">Truy cập không gian làm việc học thuật của bạn</p>
        </div>

        {successMsg && (
          <div className="mb-6 p-4 bg-secondary-container text-on-secondary-container rounded-xl text-xs font-black uppercase tracking-widest flex items-center gap-2">
            <span className="material-symbols-outlined text-lg">check_circle</span>
            {successMsg}
          </div>
        )}
        {error && (
          <div className="mb-6 p-4 bg-error-container text-on-error-container rounded-xl text-xs font-black uppercase tracking-widest flex items-center gap-2">
            <span className="material-symbols-outlined text-lg">error</span>
            {error}
          </div>
        )}

        <form className="space-y-5" onSubmit={otpRequired ? handleVerifyOtp : handleLoginSubmit}>
          {!otpRequired ? (
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
                <button type="button" className="text-primary font-black uppercase tracking-widest hover:underline decoration-2 underline-offset-4">Quên mật khẩu?</button>
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
                  Chưa có tài khoản? 
                  <button 
                    className="text-primary font-black ml-2 hover:underline decoration-2 underline-offset-4 transition-all" 
                    type="button"
                    onClick={() => setIsRegistering(true)}
                  >
                    Đăng ký ngay
                  </button>
                </p>
              </div>
            </>
          ) : (
            <>
              <div className="space-y-4 text-center">
                 <div className="w-16 h-16 bg-secondary-container text-on-secondary-container rounded-2xl flex items-center justify-center mx-auto mb-4">
                    <span className="material-symbols-outlined text-3xl">mark_email_unread</span>
                 </div>
                 <h2 className="text-xl font-black text-primary uppercase tracking-widest leading-none">Xác thực OTP</h2>
                 <p className="text-xs text-outline font-medium leading-relaxed">
                   Một mã xác thực đã được gửi đến email đăng ký của bạn. Vui lòng kiểm tra và nhập vào bên dưới.
                 </p>
              </div>
              
              <div className="space-y-1.5 mt-6">
                <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mã OTP (6 chữ số)</label>
                <input
                  className="w-full text-center text-2xl tracking-[0.5em] font-black py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl transition-all duration-300 outline-none"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value)}
                  placeholder="000000"
                  maxLength={6}
                  required
                />
              </div>

              <button 
                className="w-full py-4 bg-primary text-white rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50" 
                disabled={loading} 
                type="submit"
              >
                {loading ? "Đang xác thực..." : "Xác nhận OTP"}
              </button>

              <div className="text-center mt-4">
                 <button 
                    className="text-[10px] font-black text-outline uppercase tracking-widest hover:text-primary transition-colors" 
                    type="button" 
                    onClick={() => setOtpRequired(false)}
                 >
                    Quay lại đăng nhập
                 </button>
              </div>
            </>
          )}
        </form>
      </div>
    </div>
  );

  const renderRegisterForm = () => (
    <div className="w-full max-w-xl z-10 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="bg-surface rounded-2xl shadow-[0_20px_50px_-12px_rgba(0,21,42,0.12)] p-8 md:p-12 border border-outline-variant/10">
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-secondary-container text-on-secondary-container mb-6 shadow-xl shadow-secondary/10">
            <span className="material-symbols-outlined text-3xl" style={{ fontVariationSettings: "'FILL' 1" }}>
               person_add
            </span>
          </div>
          <h1 className="text-3xl font-black text-primary tracking-tight mb-2 font-headline uppercase leading-none">Đăng ký mới</h1>
          <p className="text-on-surface-variant font-body text-sm font-medium">Bắt đầu hành trình nghiên cứu của bạn cùng Taskify</p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-error-container text-on-error-container rounded-xl text-xs font-black uppercase tracking-widest flex items-center gap-2">
            <span className="material-symbols-outlined text-lg">error</span>
            {error}
          </div>
        )}

        <form className="space-y-6" onSubmit={handleRegisterSubmit}>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-outline uppercase tracking-[0.2em] ml-2 mb-1">Họ và tên đầy đủ</label>
            <div className="relative group">
              <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">person</span>
              <input 
                className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300" 
                placeholder="Nguyễn Văn A" 
                value={regFullName}
                onChange={(e) => setRegFullName(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-black text-outline uppercase tracking-[0.2em] ml-2 mb-1">Tên đăng nhập / MSSV</label>
            <div className="relative group">
              <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">badge</span>
              <input 
                className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300" 
                placeholder="Nhập mã số sinh viên" 
                value={regUsername}
                onChange={(e) => setRegUsername(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-outline uppercase tracking-[0.2em] ml-2 mb-1">Mật khẩu</label>
              <div className="relative group">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">lock</span>
                <input 
                  type="password"
                  className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300" 
                  placeholder="••••••••" 
                  value={regPassword}
                  onChange={(e) => setRegPassword(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-outline uppercase tracking-[0.2em] ml-2 mb-1">Xác nhận</label>
              <div className="relative group">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">verified_user</span>
                <input 
                  type="password"
                  className="w-full pl-12 pr-4 py-3.5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300" 
                  placeholder="••••••••" 
                  value={regConfirmPassword}
                  onChange={(e) => setRegConfirmPassword(e.target.value)}
                  required
                />
              </div>
            </div>
          </div>

          <div className="pt-4">
            <button 
              className="w-full bg-gradient-to-br from-primary to-primary-container text-white py-4 rounded-xl font-black text-sm uppercase tracking-widest hover:shadow-2xl hover:shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50" 
              type="submit"
              disabled={loading}
            >
              {loading ? "Đang xử lý..." : "Tạo tài khoản sinh viên"}
            </button>
          </div>
        </form>

        <div className="mt-8 pt-8 border-t border-outline-variant/10 text-center">
          <p className="text-on-surface-variant text-xs font-bold uppercase tracking-widest">
            Đã có tài khoản? 
            <button 
              className="text-primary font-black ml-2 hover:underline decoration-2 underline-offset-4 transition-all" 
              type="button"
              onClick={() => setIsRegistering(false)}
            >
              Quay lại đăng nhập
            </button>
          </p>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen flex flex-col bg-surface font-body overflow-x-hidden">
      {/* Header Overlay */}
      <header className="fixed top-0 w-full z-50 bg-white/60 dark:bg-slate-950/60 backdrop-blur-xl border-b border-outline-variant/5">
        <div className="flex justify-between items-center w-full px-6 md:px-12 py-4 max-w-7xl mx-auto">
          <div className="flex items-center gap-3">
             <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
                <span className="material-symbols-outlined text-white text-xl">school</span>
             </div>
             <span className="text-xl font-black font-headline text-primary tracking-tighter uppercase">Taskify</span>
          </div>
          <div className="hidden lg:flex items-center gap-8">
            <span className="text-[10px] font-black uppercase tracking-[0.2em] text-outline cursor-pointer hover:text-primary transition-colors">Workspace</span>
            <span className="text-[10px] font-black uppercase tracking-[0.2em] text-outline cursor-pointer hover:text-primary transition-colors">Resources</span>
            <span className="text-[10px] font-black uppercase tracking-[0.2em] text-outline cursor-pointer hover:text-primary transition-colors">Help Center</span>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-grow flex items-center justify-center pt-24 pb-12 px-4 relative">
        {/* Animated Background Gradients */}
        <div className="absolute top-20 left-10 w-96 h-96 bg-primary/5 rounded-full blur-[100px] animate-pulse"></div>
        <div className="absolute bottom-20 right-10 w-[500px] h-[500px] bg-secondary/5 rounded-full blur-[120px] animate-pulse delay-700"></div>
        
        {isRegistering ? renderRegisterForm() : renderLoginForm()}

        {/* Floating Academic Icons - Hidden on Small Screens */}
        <div className="hidden xl:block absolute top-[25%] left-[10%] opacity-10 animate-bounce duration-[5000ms]">
           <span className="material-symbols-outlined text-8xl text-primary" style={{ fontVariationSettings: "'FILL' 1" }}>menu_book</span>
        </div>
        <div className="hidden xl:block absolute bottom-[20%] right-[10%] opacity-10 animate-bounce duration-[6000ms] delay-1000">
           <span className="material-symbols-outlined text-9xl text-secondary" style={{ fontVariationSettings: "'FILL' 1" }}>history_edu</span>
        </div>
      </main>

      {/* Footer Area */}
      <footer className="w-full py-10 bg-surface-container-low/50 border-t border-outline-variant/10">
        <div className="flex flex-col md:flex-row justify-between items-center px-12 gap-6 max-w-7xl mx-auto text-outline">
          <div className="flex flex-col items-center md:items-start gap-1">
             <p className="text-[10px] font-black uppercase tracking-[0.2em]">© 2024 Taskify: Academic Management System</p>
             <p className="text-[9px] font-bold uppercase tracking-widest opacity-60">Hệ thống quản lý đề tài và nghiên cứu khoa học</p>
          </div>
          <div className="flex gap-8 text-[10px] font-black uppercase tracking-widest">
            <a href="#" className="hover:text-primary transition-colors">Hỗ trợ</a>
            <a href="#" className="hover:text-primary transition-colors">Bảo mật</a>
            <a href="#" className="hover:text-primary transition-colors">Điều khoản</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
