import { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { authApi } from "../api/auth";
import { useAuth } from "../context/AuthContext";
import PasswordChecklist, { isPasswordValid } from "../components/PasswordChecklist";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  // Luồng nhiều bước: "login" → ("changePassword" nếu đang dùng mật khẩu tạm) → ("otp" nếu vai trò cần OTP).
  // Ngoài ra "activate": kích hoạt tài khoản do quản trị tạo bằng mã nhận qua email.
  // preAuthToken chỉ nằm trong state React
  // (không lưu localStorage): nó chỉ dùng được cho bước OTP, không gọi được API nghiệp vụ.
  const [step, setStep] = useState("login");
  const [preAuthToken, setPreAuthToken] = useState("");
  const [maskedEmail, setMaskedEmail] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [activationCode, setActivationCode] = useState("");
  const [resendIn, setResendIn] = useState(0);
  const [resending, setResending] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const navigate = useNavigate();
  const { login } = useAuth();

  // Đếm ngược để cho phép "Gửi lại mã"
  useEffect(() => {
    if (resendIn <= 0) return undefined;
    const timer = setTimeout(() => setResendIn((n) => n - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendIn]);

  const finishLogin = (data) => {
    const userData = {
      id: data.userId,
      username: data.username,
      fullName: data.fullName,
      role: data.role,
    };
    login(userData, data.token);
    if (data.role === "LECTURER") navigate("/lecturer");
    else navigate("/");
  };

  const backToLogin = (message = "") => {
    setStep("login");
    setPreAuthToken("");
    setMaskedEmail("");
    setOtpCode("");
    setNewPassword("");
    setConfirmPassword("");
    setActivationCode("");
    setResendIn(0);
    setPassword("");
    setSuccessMsg("");
    setError(message);
  };

  // Quyết định bước kế tiếp từ phản hồi của máy chủ (dùng cho cả đăng nhập lẫn đổi mật khẩu bắt buộc)
  const handleNextStep = (data) => {
    if (data.nextStep === "OTP") {
      setPreAuthToken(data.preAuthToken);
      setMaskedEmail(data.maskedEmail || "");
      setResendIn(data.resendCooldownSeconds ?? 60);
      setOtpCode("");
      setStep("otp");
      return;
    }
    if (data.nextStep === "CHANGE_PASSWORD") {
      setPreAuthToken(data.preAuthToken);
      setNewPassword("");
      setConfirmPassword("");
      setStep("changePassword");
      return;
    }
    finishLogin(data);
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMsg("");
    setLoading(true);
    try {
      handleNextStep(await authApi.login(username, password));
    } catch (err) {
      setError(err.message || "Đăng nhập thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMsg("");
    setLoading(true);
    try {
      const data = await authApi.verifyOtp(preAuthToken, otpCode.trim());
      finishLogin(data);
    } catch (err) {
      // Phiên xác thực hỏng (hết hạn / sai quá số lần / mã bị hủy) → quay lại bước 1 kèm thông báo
      if (err.status === 401 && /đăng nhập lại/i.test(err.message || "")) {
        backToLogin(err.message);
      } else {
        setError(err.message || "Xác thực OTP thất bại");
        setOtpCode("");
      }
    } finally {
      setLoading(false);
    }
  };

  const passwordsOk = () => {
    if (newPassword !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp");
      return false;
    }
    return true;
  };

  const handleChangeInitialPassword = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMsg("");
    if (!passwordsOk()) return;
    setLoading(true);
    try {
      handleNextStep(await authApi.changeInitialPassword(preAuthToken, newPassword));
    } catch (err) {
      if (err.status === 401) {
        backToLogin(err.message);
      } else {
        setError(err.message || "Không đổi được mật khẩu");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleActivate = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMsg("");
    if (!passwordsOk()) return;
    setLoading(true);
    try {
      await authApi.activate(username.trim(), activationCode.trim(), newPassword);
      backToLogin();
      setSuccessMsg("Kích hoạt thành công! Vui lòng đăng nhập bằng mật khẩu vừa đặt.");
    } catch (err) {
      setError(err.message || "Kích hoạt thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    setError("");
    setSuccessMsg("");
    setResending(true);
    try {
      const data = await authApi.resendOtp(preAuthToken);
      setResendIn(data.cooldownSeconds ?? 60);
      setOtpCode("");
      setSuccessMsg("Đã gửi mã mới. Mã cũ không còn hiệu lực.");
    } catch (err) {
      if (err.status === 401) {
        backToLogin(err.message);
      } else {
        setError(err.message || "Không gửi lại được mã");
      }
    } finally {
      setResending(false);
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

        <form
          className="space-y-5"
          onSubmit={{ login: handleLoginSubmit, otp: handleVerifyOtp, changePassword: handleChangeInitialPassword, activate: handleActivate }[step]}
        >
          {step === "login" ? (
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
          ) : step === "otp" ? (
            <>
              <div className="space-y-4 text-center">
                 <div className="w-16 h-16 bg-secondary-container text-on-secondary-container rounded-2xl flex items-center justify-center mx-auto mb-4">
                    <span className="material-symbols-outlined text-3xl">mark_email_unread</span>
                 </div>
                 <h2 className="text-xl font-black text-primary uppercase tracking-widest leading-none">Xác thực OTP</h2>
                 <p className="text-xs text-outline font-medium leading-relaxed">
                   Mã xác thực gồm 6 chữ số đã được gửi tới{" "}
                   {maskedEmail ? <strong className="text-on-surface">{maskedEmail}</strong> : "email đăng ký của bạn"}.
                   Mã có hiệu lực trong 5 phút.
                 </p>
              </div>

              <div className="space-y-1.5 mt-6">
                <label className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mã OTP (6 chữ số)</label>
                <input
                  className="w-full text-center text-2xl tracking-[0.5em] font-black py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl transition-all duration-300 outline-none"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
                  placeholder="000000"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  autoFocus
                  required
                />
              </div>

              <button
                className="w-full py-4 bg-primary text-white rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50"
                disabled={loading || otpCode.length !== 6}
                type="submit"
              >
                {loading ? "Đang xác thực..." : "Xác nhận OTP"}
              </button>

              <div className="flex items-center justify-between mt-4">
                 <button
                    className="text-[10px] font-black text-primary uppercase tracking-widest hover:underline disabled:opacity-40 disabled:no-underline disabled:cursor-not-allowed"
                    type="button"
                    onClick={handleResendOtp}
                    disabled={resendIn > 0 || resending}
                 >
                    {resending ? "Đang gửi..." : resendIn > 0 ? `Gửi lại mã sau ${resendIn}s` : "Gửi lại mã"}
                 </button>
                 <button
                    className="text-[10px] font-black text-outline uppercase tracking-widest hover:text-primary transition-colors"
                    type="button"
                    onClick={() => backToLogin()}
                 >
                    Quay lại đăng nhập
                 </button>
              </div>
            </>
          ) : step === "changePassword" ? (
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
          ) : (
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
          )}
        </form>
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
        
        {renderLoginForm()}

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
