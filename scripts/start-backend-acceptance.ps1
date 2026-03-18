$ErrorActionPreference = 'Stop'

$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$workspace = (Resolve-Path (Join-Path $scriptRoot '..')).Path
$logFile = Join-Path $workspace 'logs\backend-acceptance.log'
$jarFile = Join-Path $workspace 'spring-boot-iot-admin\target\spring-boot-iot-admin-1.0.0-SNAPSHOT.jar'
$mvnSettings = Join-Path $workspace '.mvn\settings.xml'

New-Item -ItemType Directory -Path (Join-Path $workspace 'logs') -Force | Out-Null
Set-Location $workspace

$mvnCmd = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
if (-not $mvnCmd) {
    $mvnCmd = (Get-Command mvn -ErrorAction SilentlyContinue).Source
}
if (-not $mvnCmd) {
    throw 'Maven executable was not found in PATH.'
}

"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] packaging backend with real dev baseline" |
    Tee-Object -FilePath $logFile -Append

& $mvnCmd '-s' $mvnSettings 'clean' 'package' '-DskipTests' 2>&1 |
    Tee-Object -FilePath $logFile -Append

if (-not (Test-Path $jarFile)) {
    throw "Backend jar was not generated: $jarFile"
}

"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] starting backend with profile dev" |
    Tee-Object -FilePath $logFile -Append

& java '-jar' $jarFile '--spring.profiles.active=dev' 2>&1 |
    Tee-Object -FilePath $logFile -Append
