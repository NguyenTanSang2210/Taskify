import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../api/auth";
import { useAuth } from "../context/AuthContext";
import LoginStep from "./login/LoginStep";
import OtpStep from "./login/OtpStep";
import ChangePasswordStep from "./login/ChangePasswordStep";
import ForgotStep from "./login/ForgotStep";
import ResetStep from "./login/ResetStep";
import ActivateStep from "./login/ActivateStep";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  // Luồng nhiều bước: "login" → ("changePassword" nếu đang dùng mật khẩu tạm) → ("otp" nếu vai trò cần OTP).
  // Ngoài ra "activate": kích hoạt tài khoản do quản trị tạo bằng mã nhận qua email;
  // "forgot" → "reset": quên mật khẩu (nhận mã qua email rồi đặt mật khẩu mới).
  // preAuthToken chỉ nằm trong state React
  // (không lưu localStorage): nó chỉ dùng được cho bước OTP, không gọi được API nghiệp vụ.
  const [step, setStep] = useState("login");
  const [preAuthToken, setPreAuthToken] = useState("");
  const [maskedEmail, setMaskedEmail] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [activationCode, setActivationCode] = useState("");
  const [resetCode, setResetCode] = useState("");
  const [resendIn, setResendIn] = useState(0);
  const [resending, setResending] = useState(false);
  const [loading, setLoading] = useState(false);
  // Bị đưa về đây vì token hết hạn / bị thu hồi (api.js, AuthContext) → báo cho người dùng biết lý do
  const [error, setError] = useState(() =>
    new URLSearchParams(window.location.search).get("reason") === "expired"
      ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
      : ""
  );
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
    setResetCode("");
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

  const handleForgot = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMsg("");
    setLoading(true);
    try {
      const data = await authApi.forgotPassword(username.trim());
      setSuccessMsg(data.message || "Nếu tài khoản tồn tại, mã đã được gửi tới email đăng ký.");
      setResetCode("");
      setNewPassword("");
      setConfirmPassword("");
      setStep("reset");
    } catch (err) {
      setError(err.message || "Không gửi được yêu cầu");
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMsg("");
    if (!passwordsOk()) return;
    setLoading(true);
    try {
      await authApi.resetPassword(username.trim(), resetCode.trim(), newPassword);
      backToLogin();
      setSuccessMsg("Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
    } catch (err) {
      setError(err.message || "Đặt lại mật khẩu thất bại");
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
          onSubmit={{ login: handleLoginSubmit, otp: handleVerifyOtp, changePassword: handleChangeInitialPassword, activate: handleActivate, forgot: handleForgot, reset: handleReset }[step]}
        >
          {step === "login" ? (
            <LoginStep loading={loading} password={password} setError={setError} setPassword={setPassword} setStep={setStep} setSuccessMsg={setSuccessMsg} setUsername={setUsername} username={username} />
          ) : step === "otp" ? (
            <OtpStep backToLogin={backToLogin} handleResendOtp={handleResendOtp} loading={loading} maskedEmail={maskedEmail} otpCode={otpCode} resendIn={resendIn} resending={resending} setOtpCode={setOtpCode} />
          ) : step === "changePassword" ? (
            <ChangePasswordStep backToLogin={backToLogin} confirmPassword={confirmPassword} loading={loading} newPassword={newPassword} setConfirmPassword={setConfirmPassword} setNewPassword={setNewPassword} username={username} />
          ) : step === "forgot" ? (
            <ForgotStep backToLogin={backToLogin} loading={loading} setUsername={setUsername} username={username} />
          ) : step === "reset" ? (
            <ResetStep backToLogin={backToLogin} confirmPassword={confirmPassword} loading={loading} newPassword={newPassword} resetCode={resetCode} setConfirmPassword={setConfirmPassword} setNewPassword={setNewPassword} setResetCode={setResetCode} setStep={setStep} username={username} />
          ) : (
            <ActivateStep activationCode={activationCode} backToLogin={backToLogin} confirmPassword={confirmPassword} loading={loading} newPassword={newPassword} setActivationCode={setActivationCode} setConfirmPassword={setConfirmPassword} setNewPassword={setNewPassword} setUsername={setUsername} username={username} />
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
