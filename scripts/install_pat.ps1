# Install GitHub PAT on device via adb setprop.
# Launcher reads these props on first run, stores them in EncryptedSharedPreferences,
# then clears the props.
#
# Usage:
#   .\install_pat.ps1 -DeviceIp 192.168.1.42 -TelemetryPat ghp_xxx -OtaPat ghp_yyy
#
# Do NOT paste the PAT into chat or commit it to git.

param(
    [Parameter(Mandatory=$true)][string]$DeviceIp,
    [int]$Port = 5555,
    [Parameter(Mandatory=$true)][string]$TelemetryPat,
    [string]$OtaPat = ""
)

$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $Adb)) {
    Write-Host "adb not found at: $Adb" -ForegroundColor Red
    exit 1
}

& $Adb connect "$($DeviceIp):$Port"
& $Adb shell "setprop persist.nwd.telemetry_pat '$TelemetryPat'"
if ($OtaPat) {
    & $Adb shell "setprop persist.nwd.ota_pat '$OtaPat'"
}
Write-Host "PAT installed. The launcher will pick it up at next start."
Write-Host "Verify:  adb shell getprop persist.nwd.telemetry_pat"
