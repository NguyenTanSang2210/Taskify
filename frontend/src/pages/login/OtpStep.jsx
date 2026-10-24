/** Tách từ LoginPage.jsx (P3-11). */
export default function OtpStep({ backToLogin, handleResendOtp, loading, maskedEmail, otpCode, resendIn, resending, setOtpCode }) {
  return (
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
        <label htmlFor="otpstep-input-19" className="block text-xs font-black tracking-[0.2em] text-outline uppercase ml-2 mb-1">Mã OTP (6 chữ số)</label>
        <input id="otpstep-input-19"
          className="w-full text-center text-2xl tracking-[0.5em] font-black py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-surface-container-lowest rounded-xl transition-all duration-300 outline-none"
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
        className="w-full py-4 bg-primary text-on-primary rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 active:scale-[0.98] transition-all duration-300 disabled:opacity-50"
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
  );
}
