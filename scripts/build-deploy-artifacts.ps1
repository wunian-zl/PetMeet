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

function Invoke-Checked {
  param(
    [scriptblock]$Command,
    [string]$Label
  )

  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "$Label failed with exit code $LASTEXITCODE."
  }
}

if (Test-Path $OutputDir) {
  Remove-Item -LiteralPath $OutputDir -Recurse -Force
}
if (Test-Path $zipPath) {
  Remove-Item -LiteralPath $zipPath -Force
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

Push-Location $backendDir
try {
  Invoke-Checked { & .\mvnw.cmd clean package -DskipTests } "Backend build"
} finally {
  Pop-Location
}

Push-Location $userDir
try {
  Invoke-Checked { & npm.cmd ci } "User frontend npm ci"
  Invoke-Checked { & npm.cmd run build } "User frontend build"
} finally {
  Pop-Location
}

Push-Location $adminDir
try {
  $env:VITE_APP_BASE = "/admin/"
  Invoke-Checked { & npm.cmd ci } "Admin frontend npm ci"
  Invoke-Checked { & npm.cmd run build } "Admin frontend build"
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

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$sourceRoot = (Resolve-Path $OutputDir).Path
$sourcePrefix = $sourceRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
$zipStream = [System.IO.File]::Open($zipPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::ReadWrite)
try {
  $archive = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
  try {
    Get-ChildItem -LiteralPath $sourceRoot -Recurse -File | ForEach-Object {
      $relativePath = $_.FullName.Substring($sourcePrefix.Length)
      $entryName = $relativePath -replace "\\", "/"
      [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $archive,
        $_.FullName,
        $entryName,
        [System.IO.Compression.CompressionLevel]::Optimal
      ) | Out-Null
    }
  } finally {
    $archive.Dispose()
  }
} finally {
  $zipStream.Dispose()
}

Write-Host "Deployment bundle created:"
Write-Host "  $zipPath"
