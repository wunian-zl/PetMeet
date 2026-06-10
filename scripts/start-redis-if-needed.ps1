param(
  [Parameter(Mandatory = $false)]
  [string]$RedisDir = $env:PETMEET_REDIS_DIR
)

$ErrorActionPreference = "Stop"

function Resolve-RedisCommand([string]$Name) {
  if (-not [string]::IsNullOrWhiteSpace($RedisDir)) {
    $candidate = Join-Path $RedisDir "$Name.exe"
    if (Test-Path $candidate) {
      return $candidate
    }
  }

  $command = Get-Command $Name -ErrorAction SilentlyContinue
  if ($command) {
    return $command.Source
  }

  throw "$Name was not found. Install Redis and add it to PATH, or set PETMEET_REDIS_DIR."
}

$redisServerPath = Resolve-RedisCommand "redis-server"
$redisCliPath = Resolve-RedisCommand "redis-cli"

try {
  $pingResult = & $redisCliPath -h 127.0.0.1 -p 6379 ping 2>$null
  if ($pingResult -match "PONG") {
    Write-Host "Redis is already running."
    exit 0
  }
}
catch {
  # Continue when Redis is not running yet.
}

$arguments = @()
if (-not [string]::IsNullOrWhiteSpace($RedisDir)) {
  $configPath = Join-Path $RedisDir "redis.windows.conf"
  if (Test-Path $configPath) {
    $arguments += $configPath
  }
}

$startOptions = @{
  FilePath = $redisServerPath
  ArgumentList = $arguments
  WindowStyle = "Hidden"
}
if (-not [string]::IsNullOrWhiteSpace($RedisDir)) {
  $startOptions.WorkingDirectory = $RedisDir
}
Start-Process @startOptions

for ($i = 0; $i -lt 10; $i++) {
  Start-Sleep -Milliseconds 500
  try {
    $pingResult = & $redisCliPath -h 127.0.0.1 -p 6379 ping 2>$null
    if ($pingResult -match "PONG") {
      Write-Host "Redis started successfully."
      exit 0
    }
  }
  catch {
    # Keep waiting while the service initializes.
  }
}

Write-Error "Redis did not respond within 5 seconds. Check the Redis service."
exit 1
