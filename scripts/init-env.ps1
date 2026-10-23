<#
.SYNOPSIS
    Tạo file .env (Docker) và Backend/.env (chạy local) với secret / mật khẩu NGẪU NHIÊN, an toàn.

.DESCRIPTION
    - Đọc .env.docker.example, điền JWT_SECRET (48 byte ngẫu nhiên, base64) và mật khẩu MySQL
      (24 ký tự chữ+số) bằng bộ sinh số ngẫu nhiên mật mã (RandomNumberGenerator).
    - KHÔNG ghi đè file đã tồn tại trừ khi có -Force.
    - Backend/.env dùng cùng thông tin để chạy backend local (không Docker) nối vào MySQL của compose.

.PARAMETER Profile
    dev (mặc định, máy phát triển: có dữ liệu demo + Swagger) hoặc prod.

.PARAMETER OtpEmail
    Email gốc nhận OTP của tài khoản demo (ten@gmail.com -> ten+admin@gmail.com ...). Tùy chọn.

.PARAMETER Force
    Ghi đè file .env đã có (CẨN THẬN: đổi mật khẩu MySQL sẽ không khớp volume dữ liệu đã tạo trước đó).

.EXAMPLE
    .\scripts\init-env.ps1 -Profile dev -OtpEmail ten@gmail.com
#>
param(
    [ValidateSet("dev", "prod")][string]$Profile = "dev",
    [string]$OtpEmail = "",
    [switch]$SkipBackendEnv,
    [switch]$Force,
    [string]$RootDir = ""
)

$ErrorActionPreference = "Stop"

if (-not $RootDir) { $RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path }
$templatePath = Join-Path $RootDir ".env.docker.example"
$envPath = Join-Path $RootDir ".env"
$backendEnvPath = Join-Path $RootDir "Backend\.env"
$backendTemplatePath = Join-Path $RootDir "Backend\.env.example"

if (-not (Test-Path $templatePath)) { throw "Không thấy file mẫu: $templatePath" }

function New-RandomBytes([int]$count) {
    $bytes = New-Object byte[] $count
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return $bytes
}

function New-RandomSecretBase64([int]$byteCount) {
    return [Convert]::ToBase64String((New-RandomBytes $byteCount))
}

# Mật khẩu chữ+số. Loại bỏ giá trị >= 248 để mỗi ký tự có xác suất đều nhau (không lệch modulo).
function New-RandomAlphanumeric([int]$length) {
    $alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    $sb = New-Object System.Text.StringBuilder
    while ($sb.Length -lt $length) {
        foreach ($b in (New-RandomBytes 64)) {
            if ($b -lt 248 -and $sb.Length -lt $length) { [void]$sb.Append($alphabet[$b % $alphabet.Length]) }
        }
    }
    return $sb.ToString()
}

function Set-EnvValues([string[]]$lines, [hashtable]$values) {
    $seen = @{}
    $out = foreach ($line in $lines) {
        if ($line -match '^\s*([A-Z0-9_]+)\s*=') {
            $key = $Matches[1]
            if ($values.ContainsKey($key)) { $seen[$key] = $true; "$key=$($values[$key])"; continue }
        }
        $line
    }
    $missing = $values.Keys | Where-Object { -not $seen.ContainsKey($_) }
    if ($missing) { $out += ""; foreach ($k in $missing) { $out += "$k=$($values[$k])" } }
    return $out
}

function Write-Utf8NoBom([string]$path, [string[]]$lines) {
    [IO.File]::WriteAllText($path, (($lines -join "`n") + "`n"), (New-Object Text.UTF8Encoding $false))
}

$jwtSecret = New-RandomSecretBase64 48
$mysqlPassword = New-RandomAlphanumeric 24
$mysqlRootPassword = New-RandomAlphanumeric 24
$mysqlHostPort = "3307"

# ---------------------------------------------------------------- .env (Docker Compose)
if ((Test-Path $envPath) -and -not $Force) {
    Write-Host "• .env đã tồn tại — giữ nguyên (thêm -Force nếu muốn tạo lại)." -ForegroundColor Yellow
    # Đọc lại để Backend/.env (nếu chưa có) dùng đúng thông tin đang chạy
    $existing = @{}
    Get-Content $envPath | ForEach-Object { if ($_ -match '^\s*([A-Z0-9_]+)\s*=\s*(.*)$') { $existing[$Matches[1]] = $Matches[2] } }
    if ($existing["JWT_SECRET"]) { $jwtSecret = $existing["JWT_SECRET"] }
    if ($existing["MYSQL_PASSWORD"]) { $mysqlPassword = $existing["MYSQL_PASSWORD"] }
    if ($existing["MYSQL_HOST_PORT"]) { $mysqlHostPort = $existing["MYSQL_HOST_PORT"] }
} else {
    $values = @{
        SPRING_PROFILES_ACTIVE = $Profile
        APP_SEED_ENABLED       = $(if ($Profile -eq "dev") { "true" } else { "false" })
        JWT_SECRET             = $jwtSecret
        MYSQL_PASSWORD         = $mysqlPassword
        MYSQL_ROOT_PASSWORD    = $mysqlRootPassword
        # dev chạy qua http:// (kể cả IP LAN) → cookie refresh không đặt Secure; prod bắt buộc HTTPS
        APP_AUTH_REFRESH_COOKIE_SECURE = $(if ($Profile -eq "dev") { "false" } else { "true" })
    }
    if ($OtpEmail) { $values["APP_SEED_OTP_EMAIL"] = $OtpEmail }
    Write-Utf8NoBom $envPath (Set-EnvValues (Get-Content $templatePath -Encoding UTF8) $values)
    Write-Host "✓ Đã tạo .env (profile=$Profile) với JWT_SECRET và mật khẩu MySQL ngẫu nhiên." -ForegroundColor Green
}

# ---------------------------------------------------------------- Backend/.env (chạy local)
if (-not $SkipBackendEnv) {
    if ((Test-Path $backendEnvPath) -and -not $Force) {
        Write-Host "• Backend/.env đã tồn tại — giữ nguyên." -ForegroundColor Yellow
    } elseif (Test-Path $backendTemplatePath) {
        $dbUrl = "jdbc:mysql://localhost:$mysqlHostPort/doan_ltmmt?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
        $values = @{
            SPRING_PROFILES_ACTIVE = $Profile
            DB_URL                 = $dbUrl
            DB_USERNAME            = "ktpm"
            DB_PASSWORD            = $mysqlPassword
            JWT_SECRET             = $jwtSecret
            APP_SEED_ENABLED       = $(if ($Profile -eq "dev") { "true" } else { "false" })
        }
        if ($OtpEmail) { $values["APP_SEED_OTP_EMAIL"] = $OtpEmail }
        Write-Utf8NoBom $backendEnvPath (Set-EnvValues (Get-Content $backendTemplatePath -Encoding UTF8) $values)
        Write-Host "✓ Đã tạo Backend/.env (nối MySQL của compose ở localhost:$mysqlHostPort)." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Tiếp theo:" -ForegroundColor Cyan
Write-Host "  docker compose up -d --build     # chạy toàn bộ hệ thống"
Write-Host "  (đổi MYSQL_PASSWORD sau khi volume MySQL đã tạo sẽ KHÔNG có tác dụng — muốn đổi phải xóa volume: docker compose down -v)"
