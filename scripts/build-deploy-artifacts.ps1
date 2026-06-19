param(
  [string]$OutputDir
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
  $OutputDir = Join-Path $repoRoot "outputs\petmeet-deploy"
}

$backendDir = Join-Path $repoRoot "PetMeet-backend"
$userDir = Join-Path $repoRoot "PetMeet-frontend\PetMeet-user"
$adminDir = Join-Path $repoRoot "PetMeet-frontend\PetMeet-admin"
$zipPath = "$OutputDir.zip"

if (Test-Path $OutputDir) {
  Remove-Item -LiteralPath $OutputDir -Recurse -Force
}
if (Test-Path $zipPath) {
  Remove-Item -LiteralPath $zipPath -Force
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

Push-Location $backendDir
try {
  & .\mvnw.cmd clean package -DskipTests
} finally {
  Pop-Location
}

Push-Location $userDir
try {
  npm ci
  npm run build
} finally {
  Pop-Location
}

Push-Location $adminDir
try {
  $env:VITE_APP_BASE = "/admin/"
  npm ci
  npm run build
} finally {
  Remove-Item Env:\VITE_APP_BASE -ErrorAction SilentlyContinue
  Pop-Location
}

$jar = Get-ChildItem -Path (Join-Path $backendDir "target") -Filter "*.jar" |
  Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

if ($null -eq $jar) {
  throw "Backend jar not found under PetMeet-backend\target."
}

Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $OutputDir "PetMeet-backend.jar")
Copy-Item -LiteralPath (Join-Path $backendDir "sql") -Destination (Join-Path $OutputDir "sql") -Recurse
Copy-Item -LiteralPath (Join-Path $repoRoot "deploy") -Destination (Join-Path $OutputDir "deploy") -Recurse
Copy-Item -LiteralPath (Join-Path $repoRoot "uploads\demo") -Destination (Join-Path $OutputDir "uploads\demo") -Recurse

New-Item -ItemType Directory -Force -Path (Join-Path $OutputDir "frontend") | Out-Null
Copy-Item -LiteralPath (Join-Path $userDir "dist") -Destination (Join-Path $OutputDir "frontend\user") -Recurse
Copy-Item -LiteralPath (Join-Path $adminDir "dist") -Destination (Join-Path $OutputDir "frontend\admin") -Recurse

Compress-Archive -Path (Join-Path $OutputDir "*") -DestinationPath $zipPath -Force

Write-Host "Deployment bundle created:"
Write-Host "  $zipPath"
