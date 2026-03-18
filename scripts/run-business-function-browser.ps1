param(
    [string]$FrontendUrl = 'http://127.0.0.1:5174',
    [string]$BackendUrl = 'http://127.0.0.1:9999',
    [switch]$ShowBrowser
)

$ErrorActionPreference = 'Stop'

function Get-ResponseText {
    param([object]$Content)

    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }

    return [string]$Content
}

$workspace = 'E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui'
$env:npm_config_cache = Join-Path $workspace '.npm-cache'
$env:IOT_ACCEPTANCE_FRONTEND_URL = $FrontendUrl
$env:IOT_ACCEPTANCE_BACKEND_URL = $BackendUrl
$env:IOT_ACCEPTANCE_HEADLESS = if ($ShowBrowser.IsPresent) { 'false' } else { 'true' }

Set-Location $workspace

try {
    Invoke-WebRequest "$FrontendUrl/login" -UseBasicParsing -TimeoutSec 5 | Out-Null
} catch {
    throw "Frontend is not reachable at $FrontendUrl. Start it first with scripts/start-frontend-acceptance.ps1 or npm run acceptance:dev."
}

try {
    $health = Invoke-WebRequest "$BackendUrl/actuator/health" -UseBasicParsing -TimeoutSec 5
    $healthText = Get-ResponseText -Content $health.Content
    if (-not ($healthText -match '"status"\s*:\s*"UP"')) {
        throw "Backend health endpoint did not report UP: $healthText"
    }
} catch {
    throw "Backend health check failed at $BackendUrl/actuator/health. Start it first with scripts/start-backend-acceptance.ps1 or mvn spring-boot:run. Detail: $($_.Exception.Message)"
}

npm.cmd run acceptance:browser
