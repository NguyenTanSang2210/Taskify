$ErrorActionPreference = "Stop"

Write-Host "[1/3] Backend test-compile..." -ForegroundColor Cyan
Push-Location "../Backend"
try {
    .\mvnw.cmd -q -DskipTests test-compile

    Write-Host "[2/3] Backend test..." -ForegroundColor Cyan
    .\mvnw.cmd -q test
}
finally {
    Pop-Location
}

Write-Host "[3/3] Frontend lint + build..." -ForegroundColor Cyan
Push-Location "../frontend"
try {
    npm run check
}
finally {
    Pop-Location
}

Write-Host "Pre-demo check PASSED." -ForegroundColor Green
