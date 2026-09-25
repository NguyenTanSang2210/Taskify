param(
    [Parameter(Mandatory = $true)][string]$Username,
    [string]$Container = "ktpm-db"
)

# Lấy mã OTP mới nhất (chưa dùng) của một tài khoản từ MySQL trong Docker.
# Dùng khi môi trường dev chưa cấu hình email thật (MAIL_USERNAME/MAIL_PASSWORD).
# Ví dụ: .\scripts\get-otp.ps1 admin
$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot "..\.env"
$vars = @{ MYSQL_USER = "ktpm"; MYSQL_PASSWORD = "ktpm123"; MYSQL_DATABASE = "doan_ltmmt" }
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*(MYSQL_[A-Z_]+)\s*=\s*(.*)$' } | ForEach-Object { $vars[$Matches[1]] = $Matches[2] }
}

$query = "SELECT o.code, o.expires_at FROM otp_tokens o JOIN users u ON u.id=o.user_id " +
         "WHERE u.username='$Username' AND o.used=0 ORDER BY o.id DESC LIMIT 1"
$result = docker exec -e "MYSQL_PWD=$($vars.MYSQL_PASSWORD)" $Container mysql "-u$($vars.MYSQL_USER)" $vars.MYSQL_DATABASE -N -B -e $query

if ([string]::IsNullOrWhiteSpace($result)) {
    Write-Host "Không có OTP chưa dùng cho '$Username'. Hãy đăng nhập lại để hệ thống gửi mã mới." -ForegroundColor Yellow
    exit 1
}
$code, $expires = $result -split "`t"
Write-Host "OTP của ${Username}: $code (hết hạn lúc $expires theo giờ máy chủ backend, hiệu lực 5 phút)" -ForegroundColor Green
