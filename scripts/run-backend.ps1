$ErrorActionPreference = "Stop"

# Chạy backend local (profile dev). Cấu hình đọc từ Backend/.env (tạo từ Backend/.env.example).
Push-Location (Join-Path $PSScriptRoot "..\Backend")
try {
    if (-not (Test-Path ".env")) {
        Write-Host "Chưa có Backend/.env — hãy tạo từ Backend/.env.example" -ForegroundColor Yellow
    }
    .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
}
