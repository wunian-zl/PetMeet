[CmdletBinding()]
param(
    [Parameter()]
    [string]$FrontendRoot = "PetMeet-frontend",
    [Parameter()]
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$frontendBasePath = if ([System.IO.Path]::IsPathRooted($FrontendRoot)) {
    $FrontendRoot
} else {
    Join-Path (Get-Location) $FrontendRoot
}

$frontendPath = [System.IO.Path]::GetFullPath($frontendBasePath)

if (-not (Test-Path -LiteralPath $frontendPath -PathType Container)) {
    throw "Frontend directory not found: $frontendPath"
}

$projects = @(
    [PSCustomObject]@{
        Name = "admin"
        Path = Join-Path $frontendPath "PetMeet-admin"
    },
    [PSCustomObject]@{
        Name = "user"
        Path = Join-Path $frontendPath "PetMeet-user"
    }
)

foreach ($project in $projects) {
    if (-not (Test-Path -LiteralPath $project.Path -PathType Container)) {
        throw "Project directory not found: $($project.Path)"
    }

    $packageJsonPath = Join-Path $project.Path "package.json"
    if (-not (Test-Path -LiteralPath $packageJsonPath -PathType Leaf)) {
        throw "package.json not found: $packageJsonPath"
    }
}

if ($DryRun) {
    foreach ($project in $projects) {
        Write-Host ("Would start {0} from {1}" -f $project.Name, $project.Path)
    }
    exit 0
}

$started = @()

foreach ($project in $projects) {
    $proc = Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList @("-NoExit", "-Command", "npm run dev") `
        -WorkingDirectory $project.Path `
        -PassThru

    $started += $proc
    Write-Host ("Started {0} dev window (PID {1})" -f $project.Name, $proc.Id)
    Write-Host ("  {0}" -f $project.Path)
}

Write-Host ""
Write-Host "Admin and user dev servers are starting in separate PowerShell windows."
