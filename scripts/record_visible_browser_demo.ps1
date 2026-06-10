param(
  [int]$DurationSec = 185,
  [string]$FrontendBase = 'http://localhost:5173',
  [string]$AdminBase = 'http://localhost:5174',
  [string]$AdminPassword = $env:PETMEET_ADMIN_PASSWORD
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
  throw 'Provide the admin password through PETMEET_ADMIN_PASSWORD or -AdminPassword'
}

Add-Type -AssemblyName System.Windows.Forms | Out-Null

$desktop = [Environment]::GetFolderPath('Desktop')
$ts = Get-Date -Format 'yyyyMMdd_HHmmss'
$outFile = Join-Path $desktop "PetMeet_browser_visible_demo_$ts.mp4"
$logFile = Join-Path $desktop "PetMeet_browser_visible_demo_$ts.log"
$markerFile = Join-Path $desktop 'PetMeet_last_recording_path.txt'

function Write-Log([string]$msg) {
  $line = "{0} {1}" -f (Get-Date -Format 'HH:mm:ss'), $msg
  $line | Tee-Object -FilePath $logFile -Append | Out-Null
}

$ffmpeg = (Get-Command ffmpeg -ErrorAction Stop).Source
$chrome = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
if (-not (Test-Path $chrome)) {
  throw "Chrome not found: $chrome"
}

function Focus-Chrome {
  param($shellObj)
  for ($i = 0; $i -lt 12; $i++) {
    if ($shellObj.AppActivate('Google Chrome')) {
      Start-Sleep -Milliseconds 350
      return $true
    }
    Start-Sleep -Milliseconds 500
  }
  return $false
}

function Go-Url {
  param(
    [Parameter(Mandatory = $true)][string]$Url,
    [int]$WaitSec = 6,
    [int]$ScrollTimes = 0,
    $ShellObj
  )

  Start-Process -FilePath $chrome -ArgumentList $Url | Out-Null
  if (-not (Focus-Chrome -shellObj $ShellObj)) {
    Write-Log "WARN: failed to focus Chrome after open $Url"
  }
  Write-Log "Navigate: $Url"

  Start-Sleep -Seconds $WaitSec
  for ($i = 0; $i -lt $ScrollTimes; $i++) {
    $ShellObj.SendKeys('{PGDN}')
    Start-Sleep -Milliseconds 780
  }
  if ($ScrollTimes -gt 0) {
    Start-Sleep -Milliseconds 500
    $ShellObj.SendKeys('{HOME}')
    Start-Sleep -Milliseconds 700
  }
}

Write-Log "Start recording: $outFile"
$ffArgs = @(
  '-y',
  '-f', 'gdigrab',
  '-framerate', '30',
  '-i', 'desktop',
  '-t', "$DurationSec",
  '-c:v', 'libx264',
  '-preset', 'veryfast',
  '-crf', '23',
  '-pix_fmt', 'yuv420p',
  '-movflags', '+faststart',
  $outFile
)

$ffProc = Start-Process -FilePath $ffmpeg -ArgumentList $ffArgs -PassThru -WindowStyle Hidden
Start-Sleep -Seconds 2

Write-Log 'Launch Chrome and open frontend'
Start-Process -FilePath $chrome -ArgumentList '--new-window', "$FrontendBase/"
Start-Sleep -Seconds 4

$shell = New-Object -ComObject WScript.Shell

# Frontend main modules
Go-Url -Url "$FrontendBase/" -WaitSec 9 -ScrollTimes 2 -ShellObj $shell
Go-Url -Url "$FrontendBase/shop" -WaitSec 8 -ScrollTimes 1 -ShellObj $shell
Go-Url -Url "$FrontendBase/mall/list?keyword=CFA" -WaitSec 10 -ScrollTimes 2 -ShellObj $shell
Go-Url -Url "$FrontendBase/product/37" -WaitSec 10 -ScrollTimes 3 -ShellObj $shell
Go-Url -Url "$FrontendBase/profile" -WaitSec 10 -ScrollTimes 2 -ShellObj $shell
Go-Url -Url "$FrontendBase/publish" -WaitSec 8 -ScrollTimes 1 -ShellObj $shell

# Admin modules
Go-Url -Url "$AdminBase/admin/login" -WaitSec 7 -ScrollTimes 0 -ShellObj $shell

if (Focus-Chrome -shellObj $shell) {
  $shell.SendKeys('{TAB}')
  Start-Sleep -Milliseconds 300
  $shell.SendKeys('admin')
  Start-Sleep -Milliseconds 260
  $shell.SendKeys('{TAB}')
  Start-Sleep -Milliseconds 260
  $shell.SendKeys($AdminPassword)
  Start-Sleep -Milliseconds 260
  $shell.SendKeys('{ENTER}')
  Write-Log 'Try admin login via keyboard'
  Start-Sleep -Seconds 7
}

Go-Url -Url "$AdminBase/admin/dashboard" -WaitSec 8 -ScrollTimes 1 -ShellObj $shell
Go-Url -Url "$AdminBase/admin/product" -WaitSec 8 -ScrollTimes 2 -ShellObj $shell
Go-Url -Url "$AdminBase/admin/order" -WaitSec 10 -ScrollTimes 2 -ShellObj $shell
Go-Url -Url "$AdminBase/admin/content" -WaitSec 8 -ScrollTimes 1 -ShellObj $shell
Go-Url -Url "$AdminBase/admin/after-sale" -WaitSec 8 -ScrollTimes 1 -ShellObj $shell
Go-Url -Url "$AdminBase/admin/complaint" -WaitSec 8 -ScrollTimes 1 -ShellObj $shell

Write-Log 'Waiting for ffmpeg to complete'
$guard = 0
while (-not $ffProc.HasExited) {
  Start-Sleep -Seconds 1
  $guard++
  if ($guard -gt ($DurationSec + 20)) {
    break
  }
}

if (-not $ffProc.HasExited) {
  try {
    Stop-Process -Id $ffProc.Id -Force
  } catch {
    # ignore
  }
}

Set-Content -Path $markerFile -Value $outFile -Encoding UTF8
Write-Log "Done: $outFile"
