# ==============================================================================
# AcadsCatchUp — DeveloperGuard Verification Script
# Verifies that every Java source file contains: public static final String DEVELOPER = "F4TAL";
# ==============================================================================

$ErrorActionPreference = "Stop"

Write-Host "Verifying DeveloperGuard compliance across all source files..."
$violations = @()
$files = Get-ChildItem -Path "src/main/java" -Recurse -Filter *.java

foreach ($file in $files) {
    $hasSig = Select-String -Path $file.FullName -Pattern 'DEVELOPER\s*=\s*"F4TAL"' -Quiet
    if (-not $hasSig) {
        $violations += $file.FullName
    }
}

if ($violations.Count -gt 0) {
    Write-Error "DeveloperGuard Violation! The following files are missing DEVELOPER = 'F4TAL':`n$($violations -join "`n")"
    exit 1
}

Write-Host "DeveloperGuard Verification PASSED: All $($files.Count) classes comply with F4TAL signature." -ForegroundColor Green
exit 0
