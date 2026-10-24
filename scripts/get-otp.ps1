param(
    [Parameter(Mandatory = $true)][string]$Username,
    [ValidateSet("LOGIN", "ACTIVATION", "RESET_PASSWORD")][string]$Purpose = "LOGIN",
    [string]$Container = "taskify-db"
)

# Lấy mã OTP mới nhất (chưa dùng) của một tài khoản từ MySQL trong Docker.
# Dùng khi môi trường dev chưa cấu hình email thật (MAIL_USERNAME/MAIL_PASSWORD).
#   .\scripts\get-otp.ps1 admin                      # mã đăng nhập (mặc định)
#   .\scripts\get-otp.ps1 sv001 -Purpose ACTIVATION  # mã kích hoạt tài khoản
$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot "..\.env"
$vars = @{ MYSQL_USER = "ktpm"; MYSQL_DATABASE = "doan_ltmmt" }
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*(MYSQL_[A-Z_]+)\s*=\s*(.*)$' } | ForEach-Object { $vars[$Matches[1]] = $Matches[2] }
}
if (-not $vars["MYSQL_PASSWORD"]) {
    Write-Host "Không tìm thấy MYSQL_PASSWORD trong .env — chạy .\scripts\init-env.ps1 trước." -ForegroundColor Yellow
    exit 1
}

# Chỉ chấp nhận tên đăng nhập gồm chữ, số, . _ - @ + (chặn ký tự đặc biệt chèn vào câu SQL)
if ($Username -notmatch '^[A-Za-z0-9._@+-]{1,60}$') {
    Write-Host "Tên đăng nhập không hợp lệ." -ForegroundColor Red
    exit 1
}

$query = "SELECT o.code, o.expires_at FROM otp_tokens o JOIN users u ON u.id=o.user_id " +
         "WHERE u.username='$Username' AND o.used=0 AND o.purpose='$Purpose' ORDER BY o.id DESC LIMIT 1"
$result = docker exec -e "MYSQL_PWD=$($vars.MYSQL_PASSWORD)" $Container mysql "-u$($vars.MYSQL_USER)" $vars.MYSQL_DATABASE -N -B -e $query

if ([string]::IsNullOrWhiteSpace($result)) {
    Write-Host "Không có mã $Purpose chưa dùng cho '$Username'. Hãy thực hiện bước đăng nhập/kích hoạt để hệ thống gửi mã mới." -ForegroundColor Yellow
    exit 1
}
$code, $expires = $result -split "`t"
Write-Host "Mã $Purpose của ${Username}: $code (hết hạn lúc $expires theo giờ máy chủ backend)" -ForegroundColor Green
