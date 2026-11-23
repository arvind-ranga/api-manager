# Builds the release APK for API Manager.
# Run with: powershell -ExecutionPolicy Bypass -File build-apk.ps1
#
# Release signing credentials are never stored in this repo.
# Set STORE_PASSWORD / KEY_PASSWORD in your shell, or you will be prompted.
# The keystore itself (*.jks) is gitignored.

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
$env:KEYSTORE_PATH = if ($env:KEYSTORE_PATH) { $env:KEYSTORE_PATH } else { Join-Path $PSScriptRoot 'my-upload-key.jks' }

$gradle = Join-Path $PSScriptRoot 'gradlew.bat'

if (-not (Test-Path $gradle)) {
    Write-Host "Gradle wrapper not found at: $gradle" -ForegroundColor Red
    Read-Host 'Press Enter to close'
    exit 1
}
if (-not (Test-Path $env:KEYSTORE_PATH)) {
    Write-Host "Keystore not found at: $env:KEYSTORE_PATH" -ForegroundColor Red
    Write-Host 'Create one with keytool, or set KEYSTORE_PATH to your existing keystore.' -ForegroundColor Yellow
    Read-Host 'Press Enter to close'
    exit 1
}

function Read-PlainSecret([string]$Prompt) {
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

if (-not $env:STORE_PASSWORD) {
    $env:STORE_PASSWORD = Read-PlainSecret 'Keystore password'
}
if (-not $env:KEY_PASSWORD) {
    $env:KEY_PASSWORD = $env:STORE_PASSWORD
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
