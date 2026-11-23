# Builds the release APK for API Manager.
# Run with: powershell -ExecutionPolicy Bypass -File build-apk.ps1

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$env:JAVA_HOME      = 'C:\Program Files\Android\Android Studio\jbr'
$env:STORE_PASSWORD = 'apimanager'
$env:KEY_PASSWORD   = 'apimanager'
$env:KEYSTORE_PATH  = Join-Path $PSScriptRoot 'my-upload-key.jks'

$gradle = Join-Path $PSScriptRoot 'gradlew.bat'

if (-not (Test-Path $gradle)) {
    Write-Host "Gradle wrapper not found at: $gradle" -ForegroundColor Red
    Read-Host 'Press Enter to close'
    exit 1
}
if (-not (Test-Path $env:KEYSTORE_PATH)) {
    Write-Host "Keystore not found at: $env:KEYSTORE_PATH" -ForegroundColor Red
    Read-Host 'Press Enter to close'
    exit 1
}

Write-Host 'Building API Manager release APK...' -ForegroundColor Cyan
& $gradle assembleRelease --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nBUILD FAILED (exit code $LASTEXITCODE)" -ForegroundColor Red
} else {
    $apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\release\app-release.apk'
    if (Test-Path $apk) {
        $item = Get-Item $apk
        Write-Host "`nBUILD SUCCESSFUL" -ForegroundColor Green
        Write-Host "APK     : $($item.FullName)" -ForegroundColor Cyan
        Write-Host ("Size    : {0:N2} MB" -f ($item.Length / 1MB)) -ForegroundColor Cyan
        Write-Host "SHA-256 : $((Get-FileHash -Algorithm SHA256 $apk).Hash)" -ForegroundColor Cyan
    } else {
        Write-Host "`nBuild reported success but APK was not found at: $apk" -ForegroundColor Yellow
    }
}

Read-Host "`nPress Enter to close"
