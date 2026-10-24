# 04 — Luồng A: Xác thực & phiên đăng nhập

[← Mục lục](README.md)

Thành phần chính: `LoginPage` (+ 6 bước trong `pages/login/`) · `AuthContext` · `auth/session.js` · `AuthController` · `AuthService` · `OtpService` · `LoginThrottleService` · `RateLimiter` · `RefreshTokenService` · `SessionCookies` · `RefreshCookie` · `JwtTokenProvider` · `TokenAuthenticator`.

## Tổng quan các bước đăng nhập (máy trạng thái phía giao diện)

```mermaid
stateDiagram-v2
  [*] --> login
  login --> otp: OTP_REQUIRED (ADMIN, DEPT_ADMIN, LECTURER)
  login --> changePassword: PASSWORD_CHANGE_REQUIRED (mật khẩu tạm)
  login --> DA_DANG_NHAP: AUTHENTICATED (STUDENT)
  changePassword --> otp: vai trò cần OTP
  changePassword --> DA_DANG_NHAP: STUDENT
  otp --> DA_DANG_NHAP: mã đúng
  otp --> login: hết phiên tạm (10 phút) / sai 5 lần
  login --> activate: "Kích hoạt tài khoản"
  activate --> login: kích hoạt xong
  login --> forgot: "Quên mật khẩu"
  forgot --> reset: đã gửi mã (luôn báo thành công)
  reset --> login: đặt lại xong
  DA_DANG_NHAP --> [*]
```

Ba loại JWT (`JwtTokenProvider.Scope`), cùng khóa HS256, claim `sub`, `iss=taskify`, `userId`, `scope`, `tv`:

| Scope | Thời hạn | Dùng được cho | Cấp ở |
|---|---|---|---|
| `ACCESS` | 15 phút (`JWT_EXPIRATION`) | mọi API nghiệp vụ, CONNECT WebSocket | `issueSession`, `refresh`, `changePassword` |
| `OTP_PENDING` | 10 phút | `/auth/otp/verify`, `/auth/otp/resend` | sau mật khẩu đúng, vai trò cần OTP |
| `PWD_CHANGE` | 10 phút | `/auth/password/initial` | sau mật khẩu đúng, `mustChangePassword=true` |

---

## A1. Chính sách mật khẩu

```mermaid
flowchart LR
  IN["mật khẩu + username"] --> C1{"rỗng?"}
  C1 -->|có| E["400: gộp mọi lỗi bằng '; '"]
  C1 -->|không| C2["độ dài 8–64 ký tự"]
  C2 --> C3["≤ 72 byte UTF-8 (giới hạn bcrypt, chữ có dấu tốn nhiều byte)"]
  C3 --> C4["có ≥1 chữ cái"]
  C4 --> C5["có ≥1 chữ số"]
  C5 --> C6["không chứa username (không phân biệt hoa thường)"]
  C6 --> R{"có vi phạm?"}
  R -->|có| E
  R -->|không| OK["hợp lệ"]
```
Áp dụng ở: đổi mật khẩu lần đầu, kích hoạt, đặt lại mật khẩu, đổi mật khẩu trong hồ sơ, Admin đặt mật khẩu tạm. Frontend hiển thị trực quan bằng `PasswordChecklist`. Riêng "không trùng mật khẩu hiện tại" kiểm tra ở nơi đổi mật khẩu (cần so băm).

## A2. Đăng nhập bằng mật khẩu

```mermaid
sequenceDiagram
  autonumber
  actor U as Người dùng
  participant FE as LoginPage / LoginStep
  participant C as AuthController
  participant A as AuthService
  participant RL as RateLimiter (RAM)
  participant TH as LoginThrottleService (DB)
  participant AM as AuthenticationManager (bcrypt)
  participant AL as AuditLogService
  participant SC as SessionCookies

  U->>FE: username + mật khẩu
  FE->>C: POST /api/auth/login
  C->>A: login(request, clientIp)
  A->>RL: check("login", ip, 10/phút)
  alt vượt giới hạn
    RL-->>FE: 429 + Retry-After
  end
  A->>TH: assertNotLocked(username, ip)
  alt cặp (username, IP) đang bị khóa
    TH-->>FE: 429 "thử lại sau khoảng N phút" (kể cả khi mật khẩu đúng)
  end
  A->>AM: authenticate(username, password)
  alt sai mật khẩu / không tồn tại / bị khóa / chưa kích hoạt
    A->>TH: recordFailure → khóa 10 phút nếu 5 lần sai trong 15 phút
    A->>AL: LOGIN_FAILED (+ LOGIN_LOCKED)
    A-->>FE: 401 "Sai tên đăng nhập hoặc mật khẩu" (thông báo chung, chống dò tài khoản)
  end
  A->>TH: recordSuccess → xóa bộ đếm
  A->>A: continueAfterPassword(user)
  alt mustChangePassword
    A-->>FE: PASSWORD_CHANGE_REQUIRED + preAuthToken (PWD_CHANGE)
  else vai trò ADMIN / DEPARTMENT_ADMIN / LECTURER
    A->>A: OtpService.issue(LOGIN) → email OTP
    A-->>FE: OTP_REQUIRED + preAuthToken (OTP_PENDING) + email che n***@gmail.com + cooldown 60s
  else STUDENT
    A->>AL: LOGIN_SUCCESS
    A-->>C: AUTHENTICATED + access token
    C->>SC: start() → RefreshTokenService.issue (họ token mới)
    SC-->>FE: Set-Cookie taskify_refresh (HttpOnly, SameSite=Strict, Path=/api/auth, 7 ngày)
    FE->>FE: AuthContext.login(user, token) → vào ứng dụng
  end
```

Bộ đếm khóa đăng nhập lưu ở bảng `login_throttle` với khóa SHA-256 của `username|IP`: khóa theo **cặp** để kẻ xấu ở IP khác không khóa được tài khoản người khác; lưu DB nên còn nguyên sau khi khởi động lại.

## A3. Xác thực 2 bước bằng OTP

```mermaid
sequenceDiagram
  autonumber
  actor U as GV / Quản trị
  participant FE as OtpStep
  participant A as AuthService
  participant O as OtpService
  participant DB as otp_tokens
  participant M as Email

  Note over A,O: Khi đăng nhập: issue() hủy mã LOGIN cũ, tạo mã 6 số (SecureRandom), hạn 5 phút, lưu DB rồi mới gửi email
  O->>DB: INSERT (used=false, attempts=0)
  O->>M: EmailTemplates.loginOtp(code)
  U->>FE: nhập 6 số
  FE->>A: POST /auth/otp/verify {preAuthToken, otp}
  A->>A: rate limit otp-verify 10/phút/IP
  A->>A: requirePreAuthUser: token đúng scope OTP_PENDING, user active + activated, tv khớp
  A->>O: verify(user, LOGIN, code)
  O->>DB: lấy mã còn hiệu lực mới nhất (trong transaction)
  alt hết hạn
    O-->>A: EXPIRED → "Mã OTP đã hết hạn, gửi lại mã mới"
  else attempts ≥ 5
    O-->>A: LOCKED → "Sai quá nhiều lần, đăng nhập lại" (audit OTP_LOCKED)
  else so sánh thời gian hằng — đúng
    O->>DB: used = true
    O-->>A: OK
    A-->>FE: access token + cookie refresh (giống A2 nhánh STUDENT)
  else sai
    O->>DB: attempts + 1 (đạt 5 → used = true)
    O-->>A: INVALID → "Bạn còn N lần thử" (audit OTP_FAILED)
  end

  opt Gửi lại mã
    U->>FE: bấm "Gửi lại" (nút đếm ngược 60s)
    FE->>A: POST /auth/otp/resend {preAuthToken}
    A->>A: rate limit 5 lần / 10 phút / IP
    A->>O: issueWithCooldown → 429 nếu mã gần nhất < 60 giây
    O->>M: mã mới (mã cũ bị hủy)
  end
```

## A4. Đổi mật khẩu bắt buộc lần đầu (mật khẩu tạm)

```mermaid
sequenceDiagram
  autonumber
  actor U as Người dùng có mật khẩu tạm
  participant FE as ChangePasswordStep
  participant A as AuthService
  participant DB as users
  U->>FE: mật khẩu mới (kiểm tra PasswordChecklist)
  FE->>A: POST /auth/password/initial {preAuthToken, newPassword}
  A->>A: rate limit · token PWD_CHANGE hợp lệ · user.mustChangePassword = true
  A->>A: PasswordPolicy + không trùng mật khẩu hiện tại
  A->>DB: password = bcrypt(new), mustChangePassword = false, tokenVersion + 1
  Note over A,DB: tokenVersion + 1 → token PWD_CHANGE vừa dùng cũng hết hiệu lực
  A->>A: audit PASSWORD_INITIAL_SET
  A->>A: continueAfterPassword → OTP (nếu vai trò cần) hoặc cấp phiên
  A-->>FE: OTP_REQUIRED hoặc AUTHENTICATED
```

## A5. Kích hoạt tài khoản

```mermaid
sequenceDiagram
  autonumber
  participant ADM as Admin / Import Excel / Bootstrap
  participant O as OtpService
  actor U as Người dùng mới
  participant FE as ActivateStep
  participant A as AuthService
  ADM->>O: issue(user, ACTIVATION) — mã 6 số hạn 72 giờ
  O-->>U: email "Kích hoạt tài khoản Taskify"
  U->>FE: tên đăng nhập HOẶC email + mã + mật khẩu mới
  FE->>A: POST /auth/activate
  A->>A: rate limit 10 lần / 10 phút / IP
  A->>A: tìm user theo username, rồi theo email
  A->>A: PasswordPolicy TRƯỚC khi tiêu thụ mã (mật khẩu yếu không làm mất mã)
  alt không tồn tại / đã kích hoạt / bị khóa / mã sai
    A-->>FE: 401 "Mã kích hoạt không đúng hoặc đã hết hạn" (cùng một câu cho mọi lỗi)
  else hợp lệ
    A->>A: password mới, activated = true, mustChangePassword = false, tokenVersion + 1
    A->>A: audit ACTIVATE_ACCOUNT
    A-->>FE: thành công → quay về bước đăng nhập
  end
```

## A6. Quên mật khẩu → đặt lại

```mermaid
sequenceDiagram
  autonumber
  actor U as Người dùng
  participant FE as ForgotStep / ResetStep
  participant A as AuthService
  participant O as OtpService
  participant RT as RefreshTokenService
  U->>FE: username hoặc email
  FE->>A: POST /auth/password/forgot
  A->>A: rate limit 10 / 15 phút / IP và 3 / 15 phút / định danh
  alt tài khoản không tồn tại / khóa / chưa kích hoạt / không email / đang trong 60s chờ
    A-->>FE: im lặng bỏ qua
  else hợp lệ
    A->>O: issue(RESET_PASSWORD) — hạn 10 phút
    A->>A: audit PASSWORD_RESET_REQUESTED
  end
  A-->>FE: luôn "Nếu tài khoản tồn tại, mã đã được gửi" (chống dò)
  U->>FE: mã + mật khẩu mới
  FE->>A: POST /auth/password/reset
  A->>A: PasswordPolicy trước, rồi verify mã
  alt mã sai
    A-->>FE: 400 "Mã xác nhận không đúng hoặc đã hết hạn" (audit PASSWORD_RESET_FAILED)
  else đúng
    A->>A: đổi mật khẩu, tokenVersion + 1
    A->>RT: revokeAll(userId) — đăng xuất mọi thiết bị
    A->>A: audit PASSWORD_RESET_DONE
    A-->>FE: thành công → đăng nhập lại
  end
```

## A7. Làm mới phiên (refresh token xoay vòng)

```mermaid
sequenceDiagram
  autonumber
  participant FE as auth/session.js
  participant C as AuthController /refresh
  participant SC as SessionCookies
  participant RC as RefreshCookie
  participant A as AuthService
  participant R as RefreshTokenService
  participant DB as refresh_tokens
  FE->>C: POST /api/auth/refresh (cookie, withCredentials)
  C->>SC: refresh()
  SC->>RC: requireSameOrigin — Origin phải được phép hoặc cùng host (chống CSRF)
  SC->>A: refresh(raw, userAgent, ip)
  A->>A: rate limit 300 / phút / IP
  A->>R: rotate(raw)
  R->>DB: tìm theo SHA-256(raw)
  alt không tìm thấy
    R-->>SC: 401
  else token đã bị thu hồi và đã được thay
    alt bị thay trong 30 giây gần đây (2 tab cùng làm mới) và vẫn dùng được
      R->>DB: tạo token mới cùng familyId
    else quá 30 giây → nghi bị đánh cắp
      R->>DB: thu hồi MỌI token của user
      R->>R: tokenVersion + 1 (access token đang lưu hành cũng chết)
      R->>R: audit REFRESH_TOKEN_REUSE
      R-->>SC: 401
    end
  else hết hạn / user khóa / tokenVersion lệch
    R->>DB: revokedAt = now (không rollback khi ném 401)
    R-->>SC: 401
  else hợp lệ
    R->>DB: token mới (256 bit ngẫu nhiên, chỉ lưu băm), cũ: revokedAt + replacedById
  end
  A-->>SC: access token mới + raw token mới
  SC-->>FE: Set-Cookie mới + {token, userId, username, fullName, role}
  Note over SC,FE: 401 → SessionCookies xóa cookie để trình duyệt không gửi lại
```

## A8. Khôi phục phiên, làm mới chủ động, đồng bộ nhiều tab

```mermaid
flowchart TB
  subgraph RELOAD["Tải lại trang"]
    R1["AuthProvider mount"] --> R2["refreshSession()"]
    R2 -->|"OK"| R3["user + token → ready"]
    R2 -->|"401"| R4["clearLocal → ready → /login"]
  end
  subgraph PRO["Làm mới chủ động"]
    P1["token đổi"] --> P2["đọc exp trong JWT"]
    P2 --> P3["setTimeout(exp − 60s, tối thiểu 5s)"]
    P3 --> P4["refreshSession()"]
    P4 -->|"lỗi"| P5["clearLocal → /login?reason=expired"]
  end
  subgraph REACT["Phản ứng khi gặp 401"]
    Q1["request bất kỳ 401"] --> Q2["interceptor: refreshSession() 1 lần<br/>(nhiều request chờ chung 1 promise)"]
    Q2 --> Q3["gửi lại request"]
    Q2 -->|"lỗi"| Q4["sessionExpired → logout server + /login"]
  end
  subgraph TABS["Nhiều tab"]
    T1["tab A đăng xuất → xóa localStorage.user"] --> T2["tab B nhận storage event → xóa token, user"]
    T3["tab A đăng nhập → ghi user"] --> T4["tab B chưa có token → refreshSession() bằng cookie chung"]
  end
```

## A9 · A10. Đăng xuất / đăng xuất mọi thiết bị

```mermaid
sequenceDiagram
  autonumber
  actor U as Người dùng
  participant FE as TopBar / AuthContext
  participant C as AuthController
  participant R as RefreshTokenService
  participant A as AuthService
  U->>FE: Đăng xuất
  FE->>C: POST /auth/logout (cookie)
  C->>C: requireSameOrigin
  C->>R: revoke(raw) — chỉ phiên này
  C-->>FE: xóa cookie
  FE->>FE: setAccessToken(null), xóa localStorage.user → /login
  Note over U,A: Đăng xuất mọi thiết bị (API có, chưa có nút trên UI)
  U->>C: POST /auth/logout-all (Bearer)
  C->>A: logoutAll(username)
  A->>A: tokenVersion + 1 → mọi access token chết ngay
  A->>R: revokeAll(userId)
  A->>A: audit LOGOUT_ALL
  C-->>U: xóa cookie
```

## A11. Xác thực từng request & thu hồi tức thì bằng `tokenVersion`

```mermaid
flowchart LR
  E1["Đổi mật khẩu"] --> TV["users.tokenVersion + 1"]
  E2["Đặt lại mật khẩu"] --> TV
  E3["Kích hoạt / đổi mật khẩu lần đầu"] --> TV
  E4["Admin khóa tài khoản"] --> TV
  E5["Admin đổi vai trò"] --> TV
  E6["Đăng xuất mọi thiết bị"] --> TV
  E7["Phát hiện refresh token bị dùng lại"] --> TV
  TV --> CHK["Mỗi request: claim tv trong JWT == tokenVersion hiện tại?"]
  CHK -->|"lệch"| DENY["coi như không đăng nhập → 401<br/>refresh token có tokenVersion cũ cũng bị từ chối"]
  CHK -->|"khớp + active + activated + scope ACCESS"| ALLOW["SecurityContext = AuthUser"]
```
Cùng một `TokenAuthenticator` dùng cho filter HTTP và `CONNECT` WebSocket nên hai nơi không bao giờ lệch nhau.

## A12. Tạo ADMIN đầu tiên cho bản cài mới

```mermaid
flowchart TB
  S["Backend khởi động (ApplicationRunner)"] --> C0{"có APP_BOOTSTRAP_ADMIN_USERNAME<br/>hoặc _EMAIL?"}
  C0 -->|"không"| X0["bỏ qua"]
  C0 -->|"có"| C1{"đã có tài khoản vai trò ADMIN?"}
  C1 -->|"có"| X1["bỏ qua + log 'có thể xóa biến'"]
  C1 -->|"chưa"| C2{"đủ username và email hợp lệ,<br/>chưa bị tài khoản khác dùng?"}
  C2 -->|"không"| X2["bỏ qua + cảnh báo"]
  C2 -->|"có"| MK["tạo ADMIN: activated = false,<br/>mật khẩu ngẫu nhiên không ai biết"]
  MK --> AU["audit BOOTSTRAP_ADMIN (actor SYSTEM)"]
  AU --> OTP["OtpService.issue(ACTIVATION) → email"]
  OTP --> ACT["Người quản trị dùng luồng A5 để tự đặt mật khẩu"]
```
Môi trường dev dùng `DataSeeder` thay thế: tạo khoa CNTT, lớp CNTT-K15, đợt mẫu và các tài khoản `admin`, `deptadmin`, `lecturer`, `student`, `student2` (mật khẩu `123456`; email dạng `ten+admin@gmail.com` để nhận OTP thật).
