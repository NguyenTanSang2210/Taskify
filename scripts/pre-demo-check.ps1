# Lỗi được xác định qua $LASTEXITCODE (stderr của mvn/npm có thể chứa cảnh báo vô hại)
$ErrorActionPreference = "Continue"

# Kiểm tra nhanh trước demo/commit: backend compile + test, frontend lint + build.
# Test backend cần MySQL: đọc DB_URL/DB_USERNAME/DB_PASSWORD... từ Backend/.env nếu có.
$backendDir = Join-Path $PSScriptRoot "..\Backend"
$frontendDir = Join-Path $PSScriptRoot "..\frontend"

$envFile = Join-Path $backendDir ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*([A-Z0-9_]+)\s*=\s*(.*)$' } | ForEach-Object {
        Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2]
    }
}

Write-Host "[1/3] Backend test-compile..." -ForegroundColor Cyan
Push-Location $backendDir -ErrorAction Stop
try {
    .\mvnw.cmd -q -DskipTests test-compile
    if ($LASTEXITCODE -ne 0) { throw "Backend test-compile failed" }

    Write-Host "[2/3] Backend test..." -ForegroundColor Cyan
    .\mvnw.cmd -q test
    if ($LASTEXITCODE -ne 0) { throw "Backend tests failed" }
}
finally {
    Pop-Location
}

Write-Host "[3/3] Frontend lint + build..." -ForegroundColor Cyan
Push-Location $frontendDir -ErrorAction Stop
try {
    npm run check
    if ($LASTEXITCODE -ne 0) { throw "Frontend lint/build failed" }
}
finally {
    Pop-Location
}

Write-Host "Pre-demo check PASSED." -ForegroundColor Green
