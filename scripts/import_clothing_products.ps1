param(
  [Parameter(Mandatory = $false)]
  [string]$SourceDir = $env:PETMEET_PRODUCT_IMAGE_SOURCE,

  [Parameter(Mandatory = $false)]
  [string]$MysqlUser = "root",

  [Parameter(Mandatory = $false)]
  [string]$MysqlPassword = $env:PETMEET_DB_PASSWORD,

  [Parameter(Mandatory = $false)]
  [string]$MysqlHost = "localhost",

  [Parameter(Mandatory = $false)]
  [string]$MysqlDatabase = "petmeet",

  [Parameter(Mandatory = $false)]
  [string]$CategoryName = "服饰",

  [Parameter(Mandatory = $false)]
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($SourceDir)) {
  throw "请通过PETMEET_PRODUCT_IMAGE_SOURCE环境变量或-SourceDir参数提供商品图片目录"
}

if ([string]::IsNullOrWhiteSpace($MysqlPassword)) {
  throw "请通过PETMEET_DB_PASSWORD环境变量或-MysqlPassword参数提供MySQL密码"
}

function New-TempMysqlDefaultsFile {
  $path = Join-Path $env:TEMP ("petmeet_mysql_" + [Guid]::NewGuid().ToString("N") + ".cnf")
  $content = @"
[client]
user=$MysqlUser
password=$MysqlPassword
host=$MysqlHost
default-character-set=utf8mb4
"@
  Set-Content -Path $path -Value $content -Encoding ASCII
  return $path
}

function Invoke-MySqlQuery([string]$sql, [switch]$NoDb) {
  $args = @("--defaults-extra-file=$script:MysqlDefaultsFile", "--default-character-set=utf8mb4")
  if (-not $NoDb) { $args += "-D$MysqlDatabase" }
  $args += @("-N", "-s", "-e", $sql)
  $out = & mysql @args 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "mysql failed: $out"
  }
  return $out
}

function Invoke-MySqlExec([string]$sql, [switch]$NoDb) {
  $args = @("--defaults-extra-file=$script:MysqlDefaultsFile", "--default-character-set=utf8mb4")
  if (-not $NoDb) { $args += "-D$MysqlDatabase" }
  $args += @("-e", $sql)
  $out = & mysql @args 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "mysql failed: $out"
  }
  return $out
}

function Get-RepoRoot {
  return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-Utf8Md5Short([string]$text, [int]$len = 10) {
  $md5 = [System.Security.Cryptography.MD5]::Create()
  try {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    $hash = $md5.ComputeHash($bytes)
    $hex = -join ($hash | ForEach-Object { $_.ToString("x2") })
    return $hex.Substring(0, [Math]::Min($len, $hex.Length))
  } finally {
    $md5.Dispose()
  }
}

function Escape-MySqlString([string]$s) {
  if ($null -eq $s) { return "" }
  return $s.Replace("\", "\\").Replace("'", "''")
}

function To-JsonArrayLiteral([string[]]$items) {
  if ($null -eq $items -or $items.Count -eq 0) { return "[]" }
  $escaped = $items | ForEach-Object {
    $t = $_
    if ($null -eq $t) { $t = "" }
    $t = $t.Replace("\", "\\").Replace('"', '\"')
    '"' + $t + '"'
  }
  return "[" + ($escaped -join ",") + "]"
}

function Get-ImageSizeJpeg([byte[]]$bytes) {
  if ($bytes.Length -lt 4) { return $null }
  if ($bytes[0] -ne 0xFF -or $bytes[1] -ne 0xD8) { return $null }
  $i = 2
  while ($i -lt $bytes.Length - 9) {
    if ($bytes[$i] -ne 0xFF) { $i++; continue }
    $marker = $bytes[$i + 1]
    # SOF0/1/2/3 etc
    if (($marker -ge 0xC0 -and $marker -le 0xC3) -or ($marker -ge 0xC5 -and $marker -le 0xC7) -or ($marker -ge 0xC9 -and $marker -le 0xCB) -or ($marker -ge 0xCD -and $marker -le 0xCF)) {
      $len = ($bytes[$i + 2] -shl 8) + $bytes[$i + 3]
      if ($i + 2 + $len -gt $bytes.Length) { break }
      $h = ($bytes[$i + 5] -shl 8) + $bytes[$i + 6]
      $w = ($bytes[$i + 7] -shl 8) + $bytes[$i + 8]
      return @{ Width = $w; Height = $h }
    }
    # Skip markers without length
    if ($marker -eq 0xD9 -or $marker -eq 0xDA) { break }
    $segLen = ($bytes[$i + 2] -shl 8) + $bytes[$i + 3]
    if ($segLen -lt 2) { break }
    $i += 2 + $segLen
  }
  return $null
}

function Get-ImageSizePng([byte[]]$bytes) {
  if ($bytes.Length -lt 24) { return $null }
  $sig = 0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A
  for ($i=0; $i -lt 8; $i++) { if ($bytes[$i] -ne $sig[$i]) { return $null } }
  # IHDR starts at byte 12
  if ($bytes[12] -ne 0x49 -or $bytes[13] -ne 0x48 -or $bytes[14] -ne 0x44 -or $bytes[15] -ne 0x52) { return $null }
  $w = ($bytes[16] -shl 24) + ($bytes[17] -shl 16) + ($bytes[18] -shl 8) + $bytes[19]
  $h = ($bytes[20] -shl 24) + ($bytes[21] -shl 16) + ($bytes[22] -shl 8) + $bytes[23]
  return @{ Width = $w; Height = $h }
}

function Get-ImageSizeGif([byte[]]$bytes) {
  if ($bytes.Length -lt 10) { return $null }
  $hdr = [System.Text.Encoding]::ASCII.GetString($bytes, 0, 6)
  if ($hdr -ne "GIF87a" -and $hdr -ne "GIF89a") { return $null }
  $w = $bytes[6] + ($bytes[7] -shl 8)
  $h = $bytes[8] + ($bytes[9] -shl 8)
  return @{ Width = $w; Height = $h }
}

function Get-ImageSizeBmp([byte[]]$bytes) {
  if ($bytes.Length -lt 26) { return $null }
  if ($bytes[0] -ne 0x42 -or $bytes[1] -ne 0x4D) { return $null } # BM
  $dibSize = [BitConverter]::ToInt32($bytes, 14)
  if ($dibSize -lt 40) { return $null }
  $w = [BitConverter]::ToInt32($bytes, 18)
  $h = [BitConverter]::ToInt32($bytes, 22)
  return @{ Width = [Math]::Abs($w); Height = [Math]::Abs($h) }
}

function Get-ImageSizeWebp([byte[]]$bytes) {
  if ($bytes.Length -lt 30) { return $null }
  # RIFF....WEBP
  if ($bytes[0] -ne 0x52 -or $bytes[1] -ne 0x49 -or $bytes[2] -ne 0x46 -or $bytes[3] -ne 0x46) { return $null }
  if ($bytes[8] -ne 0x57 -or $bytes[9] -ne 0x45 -or $bytes[10] -ne 0x42 -or $bytes[11] -ne 0x50) { return $null }
  $chunk = [System.Text.Encoding]::ASCII.GetString($bytes, 12, 4)
  switch ($chunk) {
    "VP8X" {
      # bytes 24..26 width-1 (24-bit little-endian), 27..29 height-1
      if ($bytes.Length -lt 30) { return $null }
      $w = 1 + ($bytes[24] + ($bytes[25] -shl 8) + ($bytes[26] -shl 16))
      $h = 1 + ($bytes[27] + ($bytes[28] -shl 8) + ($bytes[29] -shl 16))
      return @{ Width = $w; Height = $h }
    }
    "VP8 " {
      # lossy bitstream, width/height stored in frame header at fixed offsets.
      # See: https://developers.google.com/speed/webp/docs/riff_container
      if ($bytes.Length -lt 30) { return $null }
      # Frame header begins at byte 20
      $w = ($bytes[26] + (($bytes[27] -band 0x3F) -shl 8))
      $h = ($bytes[28] + (($bytes[29] -band 0x3F) -shl 8))
      if ($w -le 0 -or $h -le 0) { return $null }
      return @{ Width = $w; Height = $h }
    }
    "VP8L" {
      # lossless: bytes 21..24 contain signature and dimensions
      if ($bytes.Length -lt 25) { return $null }
      if ($bytes[20] -ne 0x2F) { return $null }
      $b0 = $bytes[21]
      $b1 = $bytes[22]
      $b2 = $bytes[23]
      $b3 = $bytes[24]
      $w = 1 + (($b0) + (($b1 -band 0x3F) -shl 8))
      $h = 1 + ((($b1 -shr 6) + ($b2 -shl 2) + (($b3 -band 0x0F) -shl 10)))
      if ($w -le 0 -or $h -le 0) { return $null }
      return @{ Width = $w; Height = $h }
    }
  }
  return $null
}

function Get-ImageSize([string]$path) {
  $ext = [IO.Path]::GetExtension($path)
  if ($null -eq $ext) { $ext = "" }
  $ext = $ext.ToLowerInvariant().TrimStart(".")
  $bytes = [IO.File]::ReadAllBytes($path)
  switch ($ext) {
    "jpg" { return Get-ImageSizeJpeg $bytes }
    "jpeg" { return Get-ImageSizeJpeg $bytes }
    "png" { return Get-ImageSizePng $bytes }
    "gif" { return Get-ImageSizeGif $bytes }
    "bmp" { return Get-ImageSizeBmp $bytes }
    "webp" { return Get-ImageSizeWebp $bytes }
    default { return $null }
  }
}

function Guess-PetType([string]$name) {
  $t = $name.ToLowerInvariant()
  if ($t -match "猫|cat|英短|布偶|金渐层|喵") { return "cat" }
  if ($t -match "狗|犬|dog|柯基|泰迪|比熊|博美|小狗") { return "dog" }
  return "general"
}

function Build-SubTitle([string]$name, [string]$petType) {
  $tokens = New-Object System.Collections.Generic.List[string]
  if ($name -match "新年|新春|过年|春节") { $tokens.Add("新年喜庆") }
  if ($name -match "加绒|保暖|冬") { $tokens.Add("加绒保暖") }
  if ($name -match "背心") { $tokens.Add("轻便背心") }
  if ($name -match "四脚") { $tokens.Add("四脚连体") }
  if ($name -match "牵引扣") { $tokens.Add("带牵引扣") }
  if ($name -match "防掉毛") { $tokens.Add("防掉毛更省心") }
  if ($tokens.Count -eq 0) { $tokens.Add("舒适亲肤") }

  $petLabel = switch ($petType) { "cat" { "猫咪" } "dog" { "狗狗" } default { "宠物" } }
  $core = ($tokens | Select-Object -Unique) -join " · "
  return "$petLabel 服饰 | $core，日常出行更好看"
}

function Pick-Images($imageInfos) {
  if ($null -eq $imageInfos -or $imageInfos.Count -eq 0) {
    return @{ Covers = @(); Details = @() }
  }

  $sorted = $imageInfos | Sort-Object { $_.Name }

  $coverCandidates = $sorted | Where-Object {
    $_.Width -gt 0 -and $_.Height -gt 0 -and
    ($_.Ratio -ge 0.75) -and ($_.Ratio -le 1.33) -and
    ($_.MinSide -ge 400)
  }
  if ($coverCandidates.Count -eq 0) {
    $coverCandidates = $sorted | Where-Object { $_.Width -gt 0 -and $_.Height -gt 0 }
  }

  $covers = $coverCandidates |
    Sort-Object @{ Expression = { [Math]::Abs($_.Ratio - 1.0) }; Ascending = $true },
                @{ Expression = { $_.Area }; Ascending = $false } |
    Select-Object -First 3

  $coverPaths = @($covers | ForEach-Object { $_.FullName })
  $remaining = $sorted | Where-Object { $coverPaths -notcontains $_.FullName }

  $details = $remaining |
    Sort-Object @{ Expression = { $_.Area }; Ascending = $false },
                @{ Expression = { [Math]::Abs($_.Ratio - 1.0) }; Ascending = $false } |
    Select-Object -First 9

  # Ensure at least 1 cover and 1 detail
  if ($covers.Count -eq 0) {
    $covers = $sorted | Sort-Object @{ Expression = { $_.Area }; Ascending = $false } | Select-Object -First 1
    $coverPaths = @($covers | ForEach-Object { $_.FullName })
    $remaining = $sorted | Where-Object { $coverPaths -notcontains $_.FullName }
    $details = $remaining | Select-Object -First 9
  }
  if ($details.Count -eq 0) {
    $details = $covers | Select-Object -First 1
  }

  return @{
    Covers = @($covers | ForEach-Object { $_.FullName })
    Details = @($details | ForEach-Object { $_.FullName })
  }
}

function Ensure-Category([string]$name) {
  $query = "SELECT id FROM pms_category WHERE name = '$(Escape-MySqlString $name)' LIMIT 1;"
  $out = Invoke-MySqlQuery $query
  $id = if ($out) { $out.Trim() } else { "" }
  if ($id) { return [int]$id }

  $insert = @"
INSERT INTO pms_category (name, icon, sort, status)
SELECT '$(Escape-MySqlString $name)', NULL, 7, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM pms_category WHERE name = '$(Escape-MySqlString $name)' LIMIT 1);
"@
  if (-not $DryRun) {
    Invoke-MySqlExec $insert | Out-Null
  }
  $out2 = Invoke-MySqlQuery $query
  $id = if ($out2) { $out2.Trim() } else { "" }
  if (-not $id) { throw "Failed to create/find category: $name" }
  return [int]$id
}

function Product-Exists([string]$name) {
  $q = "SELECT id FROM pms_product WHERE is_deleted = 0 AND name = '$(Escape-MySqlString $name)' LIMIT 1;"
  $out = Invoke-MySqlQuery $q
  $id = if ($out) { $out.Trim() } else { "" }
  return [bool]$id
}

$script:MysqlDefaultsFile = New-TempMysqlDefaultsFile

try {
$repoRoot = Get-RepoRoot
$uploadRoot = Join-Path $repoRoot "uploads"
$now = Get-Date
$yyyy = $now.ToString("yyyy")
$mm = $now.ToString("MM")
$coverOutDir = Join-Path $uploadRoot (Join-Path "product\\cover" (Join-Path $yyyy $mm))
$detailOutDir = Join-Path $uploadRoot (Join-Path "product\\detail" (Join-Path $yyyy $mm))
New-Item -ItemType Directory -Force -Path $coverOutDir | Out-Null
New-Item -ItemType Directory -Force -Path $detailOutDir | Out-Null

if (-not (Test-Path $SourceDir -PathType Container)) {
  throw "SourceDir not found: $SourceDir"
}

$categoryId = Ensure-Category $CategoryName
Write-Host "Category '$CategoryName' id=$categoryId"

$productDirs = Get-ChildItem $SourceDir -Directory | Sort-Object Name
if ($productDirs.Count -eq 0) {
  throw "No product folders found in: $SourceDir"
}

$sqlLines = New-Object System.Collections.Generic.List[string]
$sqlLines.Add("-- Imported clothing products: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))")
$sqlLines.Add("SET NAMES utf8mb4;")

foreach ($dir in $productDirs) {
  $productName = $dir.Name
  if (Product-Exists $productName) {
    Write-Host "Skip existing product: $productName"
    continue
  }

  $petType = Guess-PetType $productName
  $subTitle = Build-SubTitle $productName $petType

  $images = Get-ChildItem $dir.FullName -Recurse -File | Where-Object {
    $_.Extension -match "^\.(?i)(jpg|jpeg|png|webp|gif|bmp)$"
  } | Sort-Object Name

  if ($images.Count -eq 0) {
    Write-Warning "No images found for: $productName"
    continue
  }

  $infos = @()
  foreach ($img in $images) {
    $size = $null
    try { $size = Get-ImageSize $img.FullName } catch { $size = $null }
    $w = 0; $h = 0
    if ($size) { $w = [int]$size.Width; $h = [int]$size.Height }
    $ratio = 0.0
    if ($w -gt 0 -and $h -gt 0) { $ratio = [double]$w / [double]$h }
    $area = [long]$w * [long]$h
    $minSide = if ($w -gt 0 -and $h -gt 0) { [Math]::Min($w, $h) } else { 0 }
    $infos += [pscustomobject]@{
      Name = $img.Name
      FullName = $img.FullName
      Ext = $img.Extension.ToLowerInvariant()
      Width = $w
      Height = $h
      Ratio = $ratio
      Area = $area
      MinSide = $minSide
      Length = $img.Length
    }
  }

  $pick = Pick-Images $infos
  $coverFiles = $pick.Covers
  $detailFiles = $pick.Details

  $key = Get-Utf8Md5Short $productName 10
  $coverUrls = @()
  $detailUrls = @()

  $i = 0
  foreach ($src in $coverFiles) {
    $i++
    $ext = ([IO.Path]::GetExtension($src)).ToLowerInvariant()
    $destName = "p_${key}_cover_${i}${ext}"
    $dest = Join-Path $coverOutDir $destName
    if (-not $DryRun) { Copy-Item -Force -Path $src -Destination $dest }
    $coverUrls += "/images/product/cover/$yyyy/$mm/$destName"
  }

  $j = 0
  foreach ($src in $detailFiles) {
    $j++
    $ext = ([IO.Path]::GetExtension($src)).ToLowerInvariant()
    $destName = "p_${key}_detail_${j}${ext}"
    $dest = Join-Path $detailOutDir $destName
    if (-not $DryRun) { Copy-Item -Force -Path $src -Destination $dest }
    $detailUrls += "/images/product/detail/$yyyy/$mm/$destName"
  }

  $coverImg = $coverUrls[0]
  $coverImgsJson = To-JsonArrayLiteral $coverUrls
  $detailImgsJson = To-JsonArrayLiteral $detailUrls

  $price = switch -Regex ($productName) {
    "猫" { 39.90; break }
    "背心" { 45.90; break }
    "四脚" { 59.90; break }
    default { 49.90 }
  }
  $stock = 300
  $unit = "件"
  $warningStock = 10
  $sortWeight = 50
  $desc = "宠物服饰（$CategoryName）：$subTitle"

  $sql = @"
INSERT INTO pms_product
  (category_id, name, sub_title, price, stock, version, cover_img, cover_imgs, detail_imgs, description, status, is_deleted, create_time, unit, warning_stock, sort_weight, sales, pet_type, views, related_note_count)
SELECT
  $categoryId,
  '$(Escape-MySqlString $productName)',
  '$(Escape-MySqlString $subTitle)',
  $price,
  $stock,
  0,
  '$(Escape-MySqlString $coverImg)',
  '$(Escape-MySqlString $coverImgsJson)',
  '$(Escape-MySqlString $detailImgsJson)',
  '$(Escape-MySqlString $desc)',
  1,
  0,
  NOW(),
  '$(Escape-MySqlString $unit)',
  $warningStock,
  $sortWeight,
  0,
  '$(Escape-MySqlString $petType)',
  0,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM pms_product WHERE is_deleted = 0 AND name = '$(Escape-MySqlString $productName)' LIMIT 1
);
"@
  $sqlLines.Add($sql.Trim())

  Write-Host "Prepared: $productName (petType=$petType) covers=$($coverUrls.Count) details=$($detailUrls.Count)"
}

$outSqlPath = Join-Path $repoRoot "tmp_import_clothing_products.sql"
$sqlText = ($sqlLines -join "`r`n") + "`r`n"
Set-Content -Path $outSqlPath -Value $sqlText -Encoding UTF8
Write-Host "Wrote SQL: $outSqlPath"

if ($DryRun) {
  Write-Host "DryRun: skipped DB insert."
  return
}

if ($sqlLines.Count -le 2) {
  Write-Host "No new products to insert."
  return
}

Invoke-MySqlExec "source $outSqlPath" | Out-Null
Write-Host "Imported products into MySQL."
} finally {
  Remove-Item -Force -ErrorAction SilentlyContinue $script:MysqlDefaultsFile | Out-Null
}
