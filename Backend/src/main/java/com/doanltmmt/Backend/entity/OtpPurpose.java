package com.doanltmmt.Backend.entity;

/** Mục đích của một mã OTP. Mã của mục đích này KHÔNG dùng được cho mục đích khác. */
public enum OtpPurpose {
    /** Xác thực bước 2 khi đăng nhập (ADMIN, DEPARTMENT_ADMIN, LECTURER). */
    LOGIN,
    /** Kích hoạt tài khoản do quản trị viên tạo (người dùng tự đặt mật khẩu lần đầu). */
    ACTIVATION,
    /** Đặt lại mật khẩu khi quên (P2-05). */
    RESET_PASSWORD
}
