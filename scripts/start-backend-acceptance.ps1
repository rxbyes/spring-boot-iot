param(
    [int]$Port = 0
)

$ErrorActionPreference = 'Stop'

$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$workspace = (Resolve-Path (Join-Path $scriptRoot '..')).Path
$logFile = Join-Path $workspace 'logs\backend-acceptance.log'
$jarFile = Join-Path $workspace 'spring-boot-iot-admin\target\spring-boot-iot-admin-1.0.0-SNAPSHOT.jar'
$runtimeJarDir = Join-Path $workspace 'logs\backend-runtime'
$mvnSettings = Join-Path $workspace '.mvn\settings.xml'

function Invoke-LoggedNativeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CommandPath,
        [string[]]$Arguments = @(),
        [Parameter(Mandatory = $true)]
        [string]$LogFilePath
    )

    $stdoutFile = [System.IO.Path]::GetTempFileName()
    $stderrFile = [System.IO.Path]::GetTempFileName()

    try {
        $process = Start-Process -FilePath $CommandPath `
            -ArgumentList $Arguments `
            -NoNewWindow `
            -Wait `
            -PassThru `
            -RedirectStandardOutput $stdoutFile `
            -RedirectStandardError $stderrFile
        $exitCode = $process.ExitCode

        foreach ($streamFile in @($stdoutFile, $stderrFile)) {
            if ((Test-Path $streamFile) -and (Get-Item $streamFile).Length -gt 0) {
                Get-Content -Path $streamFile -Encoding UTF8 |
                    Tee-Object -FilePath $LogFilePath -Append
            }
        }
    } finally {
        foreach ($streamFile in @($stdoutFile, $stderrFile)) {
            if (Test-Path $streamFile) {
                Remove-Item -LiteralPath $streamFile -Force
            }
        }
    }

    if ($exitCode -ne 0) {
        $commandPreview = @($CommandPath) + $Arguments
        throw "Native command failed with exit code ${exitCode}: $($commandPreview -join ' ')"
    }
}

function Resolve-BackendAcceptancePort {
    param(
        [int]$RequestedPort
    )

    if ($RequestedPort -gt 0) {
        return $RequestedPort
    }

    if ([string]::IsNullOrWhiteSpace($env:IOT_BACKEND_ACCEPTANCE_PORT)) {
        return 0
    }

    $parsedPort = 0
    if (-not [int]::TryParse($env:IOT_BACKEND_ACCEPTANCE_PORT, [ref]$parsedPort) -or $parsedPort -le 0) {
        throw "IOT_BACKEND_ACCEPTANCE_PORT must be a positive integer: $($env:IOT_BACKEND_ACCEPTANCE_PORT)"
    }

    return $parsedPort
}

New-Item -ItemType Directory -Path (Join-Path $workspace 'logs') -Force | Out-Null
New-Item -ItemType Directory -Path $runtimeJarDir -Force | Out-Null
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

$mavenArgs = @()
if (Test-Path $mvnSettings) {
    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] detected Maven settings: $mvnSettings" |
        Tee-Object -FilePath $logFile -Append
    $mavenArgs += @('-s', $mvnSettings)
} else {
    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] .mvn/settings.xml not found, fallback to plain mvn" |
        Tee-Object -FilePath $logFile -Append
}

$mavenArgs += @('clean', 'package', '-DskipTests')
Invoke-LoggedNativeCommand -CommandPath $mvnCmd -Arguments $mavenArgs -LogFilePath $logFile

if (-not (Test-Path $jarFile)) {
    throw "Backend jar was not generated: $jarFile"
}

$backendPort = Resolve-BackendAcceptancePort -RequestedPort $Port
$runtimeJarName = "spring-boot-iot-admin-dev-$(Get-Date -Format 'yyyyMMddHHmmss').jar"
$runtimeJarFile = Join-Path $runtimeJarDir $runtimeJarName
Copy-Item -LiteralPath $jarFile -Destination $runtimeJarFile -Force
"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] copied runtime jar to $runtimeJarFile" |
    Tee-Object -FilePath $logFile -Append

$javaArgs = @('-jar', $runtimeJarFile, '--spring.profiles.active=dev')
if ($backendPort -gt 0) {
    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] overriding backend port to $backendPort" |
        Tee-Object -FilePath $logFile -Append
    $javaArgs += "--server.port=$backendPort"
}

"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] starting backend with profile dev" |
    Tee-Object -FilePath $logFile -Append

Invoke-LoggedNativeCommand -CommandPath 'java' -Arguments $javaArgs -LogFilePath $logFile
