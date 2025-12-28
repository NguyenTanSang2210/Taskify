import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../api/auth";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  // Login states
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
      // Pre-fill login
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
        // Temporarily store user data to be used after OTP
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
      
      // Retrieve temp data
      const tempUser = JSON.parse(localStorage.getItem("temp_user"));
      const tempToken = localStorage.getItem("temp_token");

      if (tempUser && tempToken) {
          login(tempUser, tempToken);
          // Cleanup temp
          localStorage.removeItem("temp_user");
          localStorage.removeItem("temp_token");
          
          if (tempUser.role === "LECTURER") navigate("/lecturer");
          else navigate("/");
      } else {
          // Fallback if temp data missing
          navigate("/login");
      }

    } catch (err) {
      setError(err.message || "Xác thực OTP thất bại");
    } finally {
      setLoading(false);
    }
  };

  const renderLeftPanel = () => (
    <div className="login-left-panel">
      <div className="brand-logo">
        <i className="bi bi-mortarboard-fill" style={{ fontSize: '2.5rem', color: '#ffcc00' }}></i>
        <span>HUTECH eLearning</span>
      </div>
      <div className="brand-slogan">
        Bắt đầu học<br />
        với HUTECH<br />
        eLearning
      </div>
     
    </div>
  );

  const renderLoginForm = () => (
    <div className="login-form-wrapper">
      <h3 className="login-title">Đăng nhập</h3>
      {successMsg && <div className="alert alert-success">{successMsg}</div>}
      {error && <div className="alert alert-danger">{error}</div>}
      
      <form onSubmit={otpRequired ? handleVerifyOtp : handleLoginSubmit}>
        {!otpRequired ? (
          <>
            <div className="form-floating-custom">
              <input
                className="form-control form-control-lg-custom"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Tên đăng nhập hoặc Email"
              />
            </div>

            <div className="form-floating-custom">
              <input
                type="password"
                className="form-control form-control-lg-custom"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Mật khẩu"
              />
            </div>
            
            <div className="d-flex justify-content-between align-items-center mb-4">
               <div className="form-check">
                  <input className="form-check-input" type="checkbox" id="rememberMe" />
                  <label className="form-check-label text-muted" htmlFor="rememberMe">Ghi nhớ đăng nhập</label>
               </div>
               <a href="#" className="text-decoration-none" style={{ color: '#d32f2f' }}>Quên mật khẩu?</a>
            </div>

            <button className="btn btn-login-custom mb-3" disabled={loading} type="submit">
              {loading ? "Đang xử lý..." : "Đăng nhập"}
            </button>

            <div className="divider-text">
              <span>Hoặc đăng nhập với</span>
            </div>

            <div className="row g-2">
               <div className="col-6">
                 <button type="button" className="social-login-btn social-login-apple">
                   <i className="bi bi-apple"></i> Apple
                 </button>
               </div>
               <div className="col-6">
                 <button type="button" className="social-login-btn" style={{ color: '#ea4335', borderColor: '#ddd' }}>
                   <i className="bi bi-google"></i> Google
                 </button>
               </div>
            </div>

            <div className="text-center mt-4">
               <span className="text-muted">Chưa có tài khoản? </span>
               <button 
                  className="btn btn-link p-0 text-decoration-none fw-bold" 
                  style={{ color: '#003366' }}
                  type="button" 
                  onClick={() => setIsRegistering(true)}
                >
                  Đăng ký ngay
               </button>
            </div>
          </>
        ) : (
          <>
             <div className="mb-3">
                <label className="form-label">Mã OTP (đã gửi email)</label>
                <input
                  className="form-control form-control-lg-custom"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value)}
                  placeholder="Nhập 6 chữ số"
                />
              </div>
              <button className="btn btn-login-custom" disabled={loading} type="submit">
                {loading ? "Đang xác thực..." : "Xác thực OTP"}
              </button>
              <div className="text-center mt-3">
                 <button className="btn btn-link text-muted" type="button" onClick={() => setOtpRequired(false)}>
                    Quay lại
                 </button>
              </div>
          </>
        )}
      </form>
    </div>
  );

  const renderRegisterForm = () => (
    <div className="login-form-wrapper">
      <h3 className="login-title">Đăng ký sinh viên</h3>
      {error && <div className="alert alert-danger">{error}</div>}
      
      <form onSubmit={handleRegisterSubmit}>
        <div className="form-floating-custom">
          <input
            className="form-control form-control-lg-custom"
            value={regFullName}
            onChange={(e) => setRegFullName(e.target.value)}
            placeholder="Họ và tên"
            required
          />
        </div>

        <div className="form-floating-custom">
          <input
            className="form-control form-control-lg-custom"
            value={regUsername}
            onChange={(e) => setRegUsername(e.target.value)}
            placeholder="Tên đăng nhập / MSSV"
            required
          />
        </div>

        <div className="form-floating-custom">
          <input
            type="password"
            className="form-control form-control-lg-custom"
            value={regPassword}
            onChange={(e) => setRegPassword(e.target.value)}
            placeholder="Mật khẩu"
            required
          />
        </div>

        <div className="form-floating-custom">
          <input
            type="password"
            className="form-control form-control-lg-custom"
            value={regConfirmPassword}
            onChange={(e) => setRegConfirmPassword(e.target.value)}
            placeholder="Xác nhận mật khẩu"
            required
          />
        </div>
        
        <button className="btn btn-login-custom mb-3" disabled={loading} type="submit">
          {loading ? "Đang xử lý..." : "Đăng ký"}
        </button>

        <div className="text-center mt-3">
           <span className="text-muted">Đã có tài khoản? </span>
           <button 
              className="btn btn-link p-0 text-decoration-none fw-bold" 
              style={{ color: '#003366' }}
              type="button" 
              onClick={() => setIsRegistering(false)}
            >
              Đăng nhập
           </button>
        </div>
      </form>
    </div>
  );

  return (
    <div className="login-page-container">
      {renderLeftPanel()}
      <div className="login-right-panel">
         {isRegistering ? renderRegisterForm() : renderLoginForm()}
      </div>
    </div>
  );
}
