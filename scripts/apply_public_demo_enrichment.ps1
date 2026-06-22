param(
  [string]$HostName = "124.220.91.57",
  [string]$User = "ubuntu",
  [string]$RemoteSqlPath = "/tmp/seed_public_demo_enrichment.sql"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$sqlPath = Join-Path $repoRoot "PetMeet-backend\sql\seed_public_demo_enrichment.sql"

if (-not (Test-Path -LiteralPath $sqlPath)) {
  throw "SQL file not found: $sqlPath"
}

Write-Host "Uploading SQL to ${User}@${HostName}:$RemoteSqlPath"
& scp $sqlPath "${User}@${HostName}:$RemoteSqlPath"
if ($LASTEXITCODE -ne 0) {
  throw "scp failed with exit code $LASTEXITCODE"
}

$RemoteRunnerPath = "/tmp/apply_public_demo_enrichment.sh"
$remoteScript = @"
set -euo pipefail

if [ ! -f "$RemoteSqlPath" ]; then
  echo "SQL file not found: $RemoteSqlPath" >&2
  exit 1
fi

echo "Applying demo enrichment SQL..."
sudo mysql --default-character-set=utf8mb4 < "$RemoteSqlPath"

if command -v redis-cli >/dev/null 2>&1; then
  echo "Refreshing Redis note caches..."
  redis-cli INCR cache:note:list:ver >/dev/null || true
  redis-cli --scan --pattern 'cache:note:list:*' | xargs -r redis-cli DEL >/dev/null || true
  redis-cli --scan --pattern 'note:like:count:*' | xargs -r redis-cli DEL >/dev/null || true
  redis-cli --scan --pattern 'note:like:set:*' | xargs -r redis-cli DEL >/dev/null || true
fi

echo "Current data summary:"
sudo mysql --default-character-set=utf8mb4 -N -e "
USE petmeet;
SELECT 'published_notes', COUNT(*) FROM cms_note WHERE status = 1;
SELECT 'comments', COUNT(*) FROM cms_comment WHERE status = 0;
SELECT 'orders', COUNT(*) FROM oms_order WHERE admin_deleted = 0;
SELECT 'seed_orders', COUNT(*) FROM oms_order WHERE order_sn LIKE 'PMD20260621%';
"
"@

$tmpScript = New-TemporaryFile
try {
  Set-Content -LiteralPath $tmpScript -Encoding ASCII -Value $remoteScript
  Write-Host "Uploading remote runner to ${User}@${HostName}:$RemoteRunnerPath"
  & scp $tmpScript.FullName "${User}@${HostName}:$RemoteRunnerPath"
  if ($LASTEXITCODE -ne 0) {
    throw "scp remote runner failed with exit code $LASTEXITCODE"
  }

  Write-Host "Running SQL on remote server..."
  & ssh "${User}@${HostName}" "chmod +x $RemoteRunnerPath && bash $RemoteRunnerPath"
  if ($LASTEXITCODE -ne 0) {
    throw "ssh remote execution failed with exit code $LASTEXITCODE"
  }
} finally {
  Remove-Item -LiteralPath $tmpScript -ErrorAction SilentlyContinue
}

Write-Host "Done."
