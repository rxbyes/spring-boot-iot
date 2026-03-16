$ErrorActionPreference = 'Stop'

$workspace = 'E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui'
$logFile = 'E:\idea\ghatg\spring-boot-iot\logs\frontend-acceptance.log'

New-Item -ItemType Directory -Path 'E:\idea\ghatg\spring-boot-iot\logs' -Force | Out-Null
Set-Location $workspace
$env:npm_config_cache = Join-Path $workspace '.npm-cache'

"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] starting frontend acceptance process" | Tee-Object -FilePath $logFile -Append
node -v 2>&1 | Tee-Object -FilePath $logFile -Append
npm.cmd -v 2>&1 | Tee-Object -FilePath $logFile -Append
npm.cmd install 2>&1 | Tee-Object -FilePath $logFile -Append
npm.cmd run acceptance:dev 2>&1 | Tee-Object -FilePath $logFile -Append
