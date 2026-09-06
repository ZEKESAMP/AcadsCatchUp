param(
    [string]$JarExe = "",
    [string]$ClassesDir = "target\classes",
    [string]$LibsDir = "target\libs",
    [string]$StagingDir = "target\fatjar_staging",
    [string]$Manifest = "target\MANIFEST.MF",
    [string]$OutputJar = "dist\AcadsCatchUp.jar"
)

$ErrorActionPreference = "Stop"

# Auto-detect jar executable for local or CI/CD runner environments
if (-not $JarExe -or -not (Test-Path $JarExe)) {
    $found = Get-Command jar -ErrorAction SilentlyContinue
    if ($found) {
        $JarExe = $found.Source
    } elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\jar.exe"))) {
        $JarExe = Join-Path $env:JAVA_HOME "bin\jar.exe"
    } elseif (Test-Path "C:\Program Files\Java\jdk-26.0.2.1\bin\jar.exe") {
        $JarExe = "C:\Program Files\Java\jdk-26.0.2.1\bin\jar.exe"
    } else {
        $JarExe = "jar"
    }
}

# Auto-generate Manifest if missing
if (-not (Test-Path $Manifest)) {
    $manifestParent = Split-Path -Parent $Manifest
    if ($manifestParent -and -not (Test-Path $manifestParent)) {
        New-Item -ItemType Directory -Path $manifestParent -Force | Out-Null
    }
    $manifestContent = "Manifest-Version: 1.0`r`nMain-Class: com.acadscatchup.AppLauncher`r`nCreated-By: F4TAL`r`n"
    [System.IO.File]::WriteAllText($Manifest, $manifestContent)
}

Write-Host "  -> Preparing staging directory..."
if (Test-Path $StagingDir) {
    Remove-Item -Recurse -Force $StagingDir
}
New-Item -ItemType Directory -Path $StagingDir -Force | Out-Null

Write-Host "  -> Copying application classes and resources..."
Copy-Item -Path "$ClassesDir\*" -Destination $StagingDir -Recurse -Force

Write-Host "  -> Unpacking all dependencies and cross-platform native libraries (Windows, Linux, macOS)..."
$jars = Get-ChildItem -Path "$LibsDir\*.jar"
$hasTar = (Get-Command tar -ErrorAction SilentlyContinue) -ne $null
foreach ($jar in $jars) {
    if ($hasTar) {
        tar -xf $jar.FullName -C $StagingDir
    } else {
        Push-Location $StagingDir
        try {
            & $JarExe -J-Xmx384m -xf $jar.FullName
        } finally {
            Pop-Location
        }
    }
}

# Remove signature files to ensure clean execution as an unsigned standalone fat jar
$metaInf = Join-Path $StagingDir "META-INF"
if (Test-Path $metaInf) {
    Get-ChildItem -Path $metaInf -Filter "*.SF" | Remove-Item -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Path $metaInf -Filter "*.DSA" | Remove-Item -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Path $metaInf -Filter "*.RSA" | Remove-Item -Force -ErrorAction SilentlyContinue
}

Write-Host "  -> Assembling standalone executable Fat JAR: $OutputJar..."
& $JarExe -J-Xmx384m -cfm $OutputJar $Manifest -C $StagingDir .

$sizeMb = [math]::Round(((Get-Item $OutputJar).Length / 1MB), 2)
Write-Host "  -> Stand-alone executable JAR created successfully! ($sizeMb MB)"

# Assembling Pure Java Windows Installer JAR
$installerJar = "dist\AcadsCatchUp-Setup.jar"
$installerManifest = "target\INSTALLER_MANIFEST.MF"
$installerManifestContent = "Manifest-Version: 1.0`r`nMain-Class: com.acadscatchup.installer.AcadsCatchUpInstaller`r`nCreated-By: F4TAL`r`n"
[System.IO.File]::WriteAllText($installerManifest, $installerManifestContent)

Write-Host "  -> Assembling pure Java Setup & Installation Wizard: $installerJar..."
& $JarExe -J-Xmx384m -cfm $installerJar $installerManifest -C $StagingDir .
$setupSizeMb = [math]::Round(((Get-Item $installerJar).Length / 1MB), 2)
Write-Host "  -> Pure Java Setup JAR created successfully! ($setupSizeMb MB)"

# Generate 1-Click Pure Java Installer Batch Script
$installBat = "dist\Install_AcadsCatchUp.bat"
$batContent = "@echo off`r`nstart javaw -jar `"%~dp0AcadsCatchUp-Setup.jar`" %*`r`n"
[System.IO.File]::WriteAllText($installBat, $batContent)
Write-Host "  -> Generated 1-click Pure Java setup batch script: $installBat"

# Clean up staging directory to keep project structure lightweight
Write-Host "  -> Cleaning up staging directory..."
Remove-Item -Recurse -Force $StagingDir -ErrorAction SilentlyContinue

