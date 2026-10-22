<#
.SYNOPSIS
    Chạy test backend trên một DB TEST RIÊNG (doan_ltmmt_test) để không đụng dữ liệu dev.

.DESCRIPTION
    Test tích hợp (@SpringBootTest) cần MySQL. Script này:
      1. Nạp Backend/.env (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET ...) vào biến môi trường.
      2. Tạo DB test trong container MySQL của docker compose nếu chưa có (cần .env gốc có MYSQL_ROOT_PASSWORD).
      3. Trỏ DB_URL sang DB test rồi chạy `mvnw test` (mặc định) hoặc các đối số Maven bạn truyền vào.
    Nếu Docker/container không chạy: cảnh báo và dùng nguyên DB_URL trong Backend/.env.

.PARAMETER TestDb
    Tên DB test. Mặc định doan_ltmmt_test.

.PARAMETER UseDevDb
    Không đổi sang DB test (dùng thẳng DB_URL trong Backend/.env).

.PARAMETER MvnArgs
    Đối số Maven. Mặc định: test. Ví dụ: -MvnArgs test,-Dtest=AuthFlowIT

.EXAMPLE
    .\scripts\run-tests.ps1
    .\scripts\run-tests.ps1 -MvnArgs test,-Dtest=JwtTokenProviderTest
#>
param(
    [string]$TestDb = "doan_ltmmt_test",
    [string]$Container = "ktpm-db",
    [switch]$UseDevDb,
    [string[]]$MvnArgs = @("test")
)

$ErrorActionPreference = "Continue"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendDir = Join-Path $root "Backend"

function Read-EnvFile([string]$path) {
    $map = @{}
    if (Test-Path $path) {
        Get-Content $path -Encoding UTF8 | ForEach-Object {
            if ($_ -match '^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$') { $map[$Matches[1]] = $Matches[2] }
        }
    }
    return $map
}

$backendEnv = Read-EnvFile (Join-Path $backendDir ".env")
$rootEnv = Read-EnvFile (Join-Path $root ".env")
foreach ($k in $backendEnv.Keys) { Set-Item -Path "Env:$k" -Value $backendEnv[$k] }

if (-not $env:DB_URL) {
    Write-Host "Chưa có DB_URL (Backend/.env). Chạy .\scripts\init-env.ps1 trước." -ForegroundColor Yellow
    exit 1
}

if (-not $UseDevDb) {
    $created = $false
    $rootPassword = $rootEnv["MYSQL_ROOT_PASSWORD"]
    $dbUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "ktpm" })
    if ($rootPassword -and (Get-Command docker -ErrorAction SilentlyContinue)) {
        $running = & docker ps --filter "name=^${Container}$" --format "{{.Names}}" 2>$null
        if ($running -eq $Container) {
            $sql = "CREATE DATABASE IF NOT EXISTS ``$TestDb`` CHARACTER SET utf8mb4; GRANT ALL PRIVILEGES ON ``$TestDb``.* TO '$dbUser'@'%'; FLUSH PRIVILEGES;"
            & docker exec -e "MYSQL_PWD=$rootPassword" $Container mysql -uroot -e $sql 2>$null
            if ($LASTEXITCODE -eq 0) { $created = $true }
        }
    }
    if ($created) {
        $env:DB_URL = $env:DB_URL -replace '(jdbc:mysql://[^/]+/)[^?]+', "`${1}$TestDb"
        Write-Host "✓ Dùng DB test riêng: $TestDb (dữ liệu dev không bị đụng)" -ForegroundColor Green
    } else {
        Write-Host "⚠ Không tạo được DB test (Docker/container '$Container' không chạy hoặc thiếu MYSQL_ROOT_PASSWORD) — dùng nguyên DB_URL trong Backend/.env." -ForegroundColor Yellow
    }
}

Push-Location $backendDir
try {
    & .\mvnw.cmd -B @MvnArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
