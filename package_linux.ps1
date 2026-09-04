param(
    [string]$DistDir = "dist",
    [string]$LinuxDir = "dist\AcadsCatchUp-Linux",
    [string]$IconPath = "dist\AcadsCatchUp-Linux\app_icon.png"
)

$ErrorActionPreference = "Stop"

Write-Host "==================================================="
Write-Host " Packaging AcadsCatchUp-Linux Distribution..."
Write-Host "==================================================="

if (!(Test-Path $LinuxDir)) {
    New-Item -ItemType Directory -Path $LinuxDir -Force | Out-Null
}

# 1. Copy latest Standalone Fat JAR
Write-Host "  -> Copying freshly compiled AcadsCatchUp.jar..."
Copy-Item "$DistDir\AcadsCatchUp.jar" "$LinuxDir\AcadsCatchUp.jar" -Force

# 2. Copy Database configuration fallback
if (Test-Path "$DistDir\AcadsCatchUp-Portable\database.properties") {
    Copy-Item "$DistDir\AcadsCatchUp-Portable\database.properties" "$LinuxDir\database.properties" -Force
}

# 3. Copy Application Icon
$targetIcon = "$LinuxDir\app_icon.png"
if (Test-Path $IconPath) {
    if (!(Test-Path $targetIcon) -or ((Resolve-Path $IconPath).Path -ne (Resolve-Path $targetIcon).Path)) {
        Copy-Item $IconPath $targetIcon -Force
    }
} elseif (Test-Path "src\main\resources\com\acadscatchup\img\book_icon_blue.png") {
    Copy-Item "src\main\resources\com\acadscatchup\img\book_icon_blue.png" $targetIcon -Force
} elseif (Test-Path "dist\app_icon.ico") {
    Copy-Item "dist\app_icon.ico" "$LinuxDir\app_icon.ico" -Force
}

# 4. Normalize Unix LF line endings for all text/shell files
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$textFiles = @("Launch_AcadsCatchUp.sh", "AcadsCatchUp.desktop", "HOW_TO_RUN_LINUX.txt", "database.properties")

foreach ($file in $textFiles) {
    $fullPath = Join-Path $LinuxDir $file
    if (Test-Path $fullPath) {
        $content = [System.IO.File]::ReadAllText($fullPath)
        $unixContent = $content.Replace("`r`n", "`n").Replace("`r", "`n")
        [System.IO.File]::WriteAllText($fullPath, $unixContent, $utf8NoBom)
    }
}
Write-Host "  -> Verified Unix LF line endings for scripts and desktop entry."

# 5. Create AcadsCatchUp-Linux.zip
$zipOutput = Join-Path $DistDir "AcadsCatchUp-Linux.zip"
if (Test-Path $zipOutput) { Remove-Item $zipOutput -Force }

$has7z = (Get-Command 7z -ErrorAction SilentlyContinue) -ne $null
$hasTar = (Get-Command tar -ErrorAction SilentlyContinue) -ne $null

if ($has7z) {
    Write-Host "  -> Creating $zipOutput using 7z..."
    & 7z a -tzip $zipOutput "$LinuxDir\*" | Out-Null
} else {
    Write-Host "  -> Creating $zipOutput using Compress-Archive..."
    Compress-Archive -Path "$LinuxDir\*" -DestinationPath $zipOutput -Force
}
$zipSize = [math]::Round(((Get-Item $zipOutput).Length / 1MB), 2)
Write-Host "  -> AcadsCatchUp-Linux.zip created successfully ($zipSize MB)"

# 6. Create AcadsCatchUp-Linux.tar.gz
$tarOutput = Join-Path $DistDir "AcadsCatchUp-Linux.tar"
$tarGzOutput = Join-Path $DistDir "AcadsCatchUp-Linux.tar.gz"
if (Test-Path $tarOutput) { Remove-Item $tarOutput -Force }
if (Test-Path $tarGzOutput) { Remove-Item $tarGzOutput -Force }

if ($hasTar) {
    Write-Host "  -> Creating $tarGzOutput using tar..."
    & tar -czf $tarGzOutput -C $DistDir "AcadsCatchUp-Linux"
    $tarSize = [math]::Round(((Get-Item $tarGzOutput).Length / 1MB), 2)
    Write-Host "  -> AcadsCatchUp-Linux.tar.gz created successfully ($tarSize MB)"
} elseif ($has7z) {
    Write-Host "  -> Creating $tarGzOutput using 7z..."
    & 7z a -ttar $tarOutput "$LinuxDir\*" | Out-Null
    & 7z a -tgzip $tarGzOutput $tarOutput | Out-Null
    Remove-Item $tarOutput -Force
    $tarSize = [math]::Round(((Get-Item $tarGzOutput).Length / 1MB), 2)
    Write-Host "  -> AcadsCatchUp-Linux.tar.gz created successfully ($tarSize MB)"
}

Write-Host "==================================================="
Write-Host " [SUCCESS] AcadsCatchUp-Linux packaging completed!"
Write-Host "==================================================="
