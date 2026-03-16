$ErrorActionPreference = 'Stop'

$workspace = 'E:\idea\ghatg\spring-boot-iot'
$logFile = Join-Path $workspace 'logs\backend-acceptance.log'
$jarFile = Join-Path $workspace 'spring-boot-iot-admin\target\spring-boot-iot-admin-1.0.0-SNAPSHOT.jar'

New-Item -ItemType Directory -Path (Join-Path $workspace 'logs') -Force | Out-Null

Set-Location $workspace

"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] starting backend acceptance process" | Tee-Object -FilePath $logFile -Append
# 验收统一走真实 dev 环境，不再保留 e2e/H2 专用启动路径。
& java '-jar' $jarFile '--spring.profiles.active=dev' 2>&1 | Tee-Object -FilePath $logFile -Append
