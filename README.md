# FiatBravo198 — Unified Launcher

Fiat Bravo 198 head unit'leri (Navimex + K2501/T507) için tek paketli launcher.
Eski 16 ayrı NWD APK yerine mono-APK + 3 yüksek-riskli izolat APK mimarisi.

## Topoloji

```
:app                 → launcher host (FiatLauncherActivity, HOME)
:core:common         → BuildInfo, DeviceId, NwdResult
:core:ipc            → AIDL + ContentResolver wrapper (TODO)
:core:canbus         → FiatCanDecoder + CanLogger (TODO)
:core:obd2           → ELM327 BT-SPP client (TODO)
:core:ai             → AiBrain orchestrator (TODO)
:diagnostic:collector→ DiagnosticWorker + Proc/SysProp/Battery/Network/Logcat readers
:diagnostic:uploader → GitHubApi + GitHubTelemetryUploader + SecretStore
:ota:github          → GitHubOtaUpdater + GitHubOtaWorker + ApkInstaller
:feature:*           → 11 in-process feature modülü (TODO: dashboard, radio, ...)
:externalapp:smartcamera | emergency | voice → ayrı imzalı APK (izin grupları)
```

## Saha Telemetri + GitHub OTA Döngüsü

```
Araç → DiagnosticWorker (6s) → tar.gz outbox → GitHubTelemetryUploader →
PUT /repos/Brst62/fiat-bravo-telemetry/contents/{platform}/{deviceId}/{day}/{file}
        ↓
Geliştirici telemetri reposunda log inceler, fix yazar, "git push --tags"
        ↓
GH Actions release.yml → assembleRelease → apksigner → gh release create vNN
        ↓
Araçta GitHubOtaWorker (12s) → latest release → versionCode > current?
        ↓
APK indir → REQUEST_INSTALL_PACKAGES → PackageInstaller → reboot? hayır, açık kalır
```

## Hızlı Başlangıç

### 1) Keystore üret (TEK SEFER)
```powershell
.\scripts\generate_keystore.ps1
# Çıktı: %USERPROFILE%\.android-keys\bravopro-release.jks
#        %USERPROFILE%\.android-keys\bravopro-release.jks.base64.txt
```

### 2) Lokal build için keystore'u bağla
```powershell
Copy-Item .\signing\keystore.properties.example .\signing\keystore.properties
# keystore.properties'i editle:
#   storeFile=C:\Users\baris.tas\.android-keys\bravopro-release.jks
#   storePassword=...
#   keyAlias=bravopro
#   keyPassword=...
```

### 3) GitHub repo + secrets
Repolar:
- `Brst62/fiat-bravo-ota` (public) — kaynak + CI + release APK'ları
- `Brst62/fiat-bravo-telemetry` (private) — cihaz log dump'ları

Secrets (`fiat-bravo-ota` repo > Settings > Secrets and variables > Actions):
- `BRAVOPRO_KEYSTORE_BASE64`     — base64 jks dosyası
- `BRAVOPRO_KEYSTORE_PASSWORD`   — storepass
- `BRAVOPRO_KEY_ALIAS`           — `bravopro`
- `BRAVOPRO_KEY_PASSWORD`        — keypass

### 4) PAT üret + cihaza yükle
GitHub > Settings > Developer settings > Fine-grained tokens > Generate new:
- Telemetry PAT: Repository access = `fiat-bravo-telemetry` only.
  Permissions: Contents = RW, Metadata = R.
- OTA PAT (opsiyonel, rate-limit için): Repository access = `fiat-bravo-ota`.
  Permissions: Contents = R, Metadata = R.

Cihaza kur:
```powershell
.\scripts\install_pat.ps1 -DeviceIp 192.168.1.42 -TelemetryPat ghp_xxx -OtaPat ghp_yyy
```

### 5) Lokal build + cihaza yükle
```powershell
.\gradlew :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

### 6) İlk OTA release'i tetikle
```powershell
git tag v22 ; git push origin v22
# CI release.yml çalışır → vCode 22 üzerinde APK + GH Release oluşur.
```

## Tag → versionCode Sözleşmesi

CI tag'den versionCode parse eder. Desteklenen formatlar:
- `v22`     → 22
- `v22.0.1` → 22
- `vc22`    → 22
- `10.0.0-vc22` → 22

Telefonda kurulu versionCode'dan büyükse OtaWorker indirir.

## Yapılacaklar (Sırada)

- [ ] `:core:canbus` — Fiat CAN ID tablosu (FIAT_CAN_ID_TEST_RAPORU.md'den)
- [ ] `:core:obd2` — ELM327 BT-SPP client
- [ ] `:core:ai` — AiBrain orchestrator (eski `com.nwd.aibrain` davranışı in-process)
- [ ] `:core:ipc` — Eski ContentProvider authority'lerini koru (3rd party uyumluluk)
- [ ] 11 `:feature` modülünün gerçek implementasyonu
- [ ] `:externalapp:voice` — Vosk TR model runtime download flow
- [ ] LegacyIpcRouter aksiyon routing tablosu
- [ ] Ayarlar > "Teşhis verisi gönder" toggle UI
- [ ] Migration installer: eski 13 paketi sessizce kaldır (vCode 22 ilk açılış)

## Güvenlik

- `bravopro-release.jks` cihazlara dağıtılan tüm APK'ları imzalar. Kaybedersen OTA göndermek için yeniden imzalı sürüm gerekir. **3 farklı yerde yedekle** (yerel SSD, harici disk, şifreli bulut).
- PAT'lar HER ZAMAN fine-grained, scope = ilgili tek repo. Asla `repo` scope'lu classic PAT kullanma.
- `local.properties`, `signing/keystore.properties`, `*.jks` — .gitignore'da.
