$ErrorActionPreference = "Stop"

# Chạy frontend dev server tại http://localhost:5175 (proxy /api, /ws sang http://localhost:8080)
Push-Location (Join-Path $PSScriptRoot "..\frontend")
try {
    if (-not (Test-Path "node_modules")) {
        npm ci
    }
    npm run dev
}
finally {
    Pop-Location
}
