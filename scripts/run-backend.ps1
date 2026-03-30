$ErrorActionPreference = "Stop"

Push-Location "../Backend"
try {
    .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
}
