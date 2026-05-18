# Bravopro release keystore generator.
# Run once, store the .jks under C:\Users\baris.tas\.android-keys
# For CI, convert to base64 and paste into GitHub Secrets.

param(
    [string]$OutDir = "$env:USERPROFILE\.android-keys",
    [string]$Alias = "bravopro",
    [int]$ValidityDays = 9125,
    [string]$Dname = "CN=Baris Tas, O=NWD, C=TR"
)

if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir | Out-Null
}

$Keystore = Join-Path $OutDir "bravopro-release.jks"
if (Test-Path $Keystore) {
    Write-Host "STOP: $Keystore already exists. Back it up before overwriting." -ForegroundColor Red
    exit 1
}

Write-Host "Generating keystore at: $Keystore"
Write-Host "You will be prompted for storepass and keypass. They can be the same."

& keytool -genkeypair `
    -alias $Alias `
    -keyalg RSA -keysize 4096 `
    -validity $ValidityDays `
    -keystore $Keystore `
    -dname $Dname

if ($LASTEXITCODE -ne 0) {
    Write-Host "keytool failed." -ForegroundColor Red
    exit 1
}

# Base64 for CI (GitHub Secrets value)
$Base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($Keystore))
$Base64Path = Join-Path $OutDir "bravopro-release.jks.base64.txt"
$Base64 | Set-Content -Path $Base64Path -Encoding ASCII

Write-Host ""
Write-Host "DONE."
Write-Host "Keystore : $Keystore"
Write-Host "Base64   : $Base64Path"
Write-Host ""
Write-Host "Next step: open the GitHub repo settings page:"
Write-Host "  https://github.com/Brst62/fiat-bravo-ota/settings/secrets/actions"
Write-Host ""
Write-Host "Add these four repository secrets:"
Write-Host "  BRAVOPRO_KEYSTORE_BASE64    = paste content of the base64 file above"
Write-Host "  BRAVOPRO_KEYSTORE_PASSWORD  = storepass you just chose"
Write-Host "  BRAVOPRO_KEY_ALIAS          = $Alias"
Write-Host "  BRAVOPRO_KEY_PASSWORD       = keypass you just chose"
