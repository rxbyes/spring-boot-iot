$ErrorActionPreference = 'Stop'

$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$repoRoot = (Resolve-Path (Join-Path $scriptRoot '..')).Path
$uiRoot = Join-Path $repoRoot 'spring-boot-iot-ui'
$logDir = Join-Path $repoRoot 'logs'
$logFile = Join-Path $logDir 'quality-gates.log'
$mvnSettings = Join-Path $repoRoot '.mvn\settings.xml'

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Resolve-Executable {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Candidates,
        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    foreach ($candidate in $Candidates) {
        $command = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }

    throw "$Description executable was not found in PATH."
}

function Write-Log {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message" |
        Tee-Object -FilePath $logFile -Append
}

function Invoke-LoggedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Step,
        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,
        [Parameter(Mandatory = $true)]
        [string]$Executable,
        [string[]]$Arguments = @()
    )

    Write-Log "START $Step"
    $stdoutFile = [System.IO.Path]::GetTempFileName()
    $stderrFile = [System.IO.Path]::GetTempFileName()
    try {
        $process = Start-Process -FilePath $Executable `
            -ArgumentList $Arguments `
            -WorkingDirectory $WorkingDirectory `
            -NoNewWindow `
            -Wait `
            -PassThru `
            -RedirectStandardOutput $stdoutFile `
            -RedirectStandardError $stderrFile

        foreach ($streamFile in @($stdoutFile, $stderrFile)) {
            if ((Test-Path $streamFile) -and (Get-Item $streamFile).Length -gt 0) {
                Get-Content -Path $streamFile -Encoding UTF8 |
                    Tee-Object -FilePath $logFile -Append
            }
        }

        if ($process.ExitCode -ne 0) {
            throw "$Step failed with exit code $($process.ExitCode)"
        }
    } finally {
        foreach ($streamFile in @($stdoutFile, $stderrFile)) {
            if (Test-Path $streamFile) {
                Remove-Item -LiteralPath $streamFile -Force
            }
        }
    }
    Write-Log "PASS $Step"
}

function Get-PythonUnittestArgs {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PythonExecutablePath
    )

    $pythonBasename = [System.IO.Path]::GetFileName($PythonExecutablePath).ToLowerInvariant()
    if ($pythonBasename -eq 'py' -or $pythonBasename -eq 'py.exe') {
        return @('-3', '-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v')
    }

    return @('-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v')
}

$mvnCmd = Resolve-Executable -Candidates @('mvn.cmd', 'mvn') -Description 'Maven'
$npmCmd = Resolve-Executable -Candidates @('npm.cmd', 'npm') -Description 'npm'
$nodeCmd = Resolve-Executable -Candidates @('node') -Description 'Node'
$pythonCmd = Resolve-Executable -Candidates @('py', 'python', 'python3') -Description 'Python'

$env:npm_config_cache = Join-Path $uiRoot '.npm-cache'

$mavenArgs = @()
if (Test-Path $mvnSettings) {
    Write-Log "Detected Maven settings: $mvnSettings"
    $mavenArgs += @('-s', $mvnSettings)
} else {
    Write-Log '.mvn/settings.xml not found, fallback to plain mvn'
}
$mavenArgs += @('clean', 'package', '-DskipTests')

Write-Log 'Running local minimum quality gates'
Invoke-LoggedCommand -Step 'maven clean package -DskipTests' -WorkingDirectory $repoRoot -Executable $mvnCmd -Arguments $mavenArgs
Invoke-LoggedCommand -Step 'frontend build' -WorkingDirectory $uiRoot -Executable $npmCmd -Arguments @('run', 'build')
Invoke-LoggedCommand -Step 'frontend component guard' -WorkingDirectory $uiRoot -Executable $npmCmd -Arguments @('run', 'component:guard')
Invoke-LoggedCommand -Step 'frontend list guard' -WorkingDirectory $uiRoot -Executable $npmCmd -Arguments @('run', 'list:guard')
Invoke-LoggedCommand -Step 'frontend style guard' -WorkingDirectory $uiRoot -Executable $npmCmd -Arguments @('run', 'style:guard')
$schemaArgs = Get-PythonUnittestArgs -PythonExecutablePath $pythonCmd
Invoke-LoggedCommand -Step 'schema baseline guard' -WorkingDirectory $repoRoot -Executable $pythonCmd -Arguments $schemaArgs
Invoke-LoggedCommand -Step 'governance registry guard' -WorkingDirectory $repoRoot -Executable $pythonCmd -Arguments @('scripts/governance/check_governance_registry.py')
Invoke-LoggedCommand -Step 'governance contract gates' -WorkingDirectory $repoRoot -Executable $nodeCmd -Arguments @('scripts/run-governance-contract-gates.mjs')
Invoke-LoggedCommand -Step 'docs topology check' -WorkingDirectory $repoRoot -Executable $nodeCmd -Arguments @('scripts/docs/check-topology.mjs')
Write-Log 'All local minimum quality gates passed'
