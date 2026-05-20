# FiatBravo198_APP — Bağlam (Claude için kalıcı bilgi)

Bu dosya, projeye temas eden her yeni Claude oturumunun **soru sormadan** durumu kavraması içindir.
Hızlı değişen alanlar (HEAD, son tag, son CI) için `git log`/`gh` kullan; bu dosya **yavaş değişen mimari + niyet**'i tutar.

## 1. Proje nedir

Fiat Bravo 198 head unit (K2501, Allwinner T507) için **tek-mono-APK launcher**.
Eski 16 NWD APK'sının (AiBrain, OBD2, CarKit, vs.) işlevlerini tek pakette yeniden inşa ediyor.
İmzalı APK GitHub Releases üzerinden cihaza OTA push'lanır; cihaz da telemetriyi başka bir GitHub reposuna push'lar.

## 2. Yollar (lokal)

| Şey | Yol |
|---|---|
| Repo kökü | `C:\Users\baris.tas\Desktop\projeler\Python Projelerim\FiatBravo198_APP` |
| Android SDK | `$env:LOCALAPPDATA\Android\Sdk` |
| Eski K2501 OTA projesi (ayrı) | `...\Python Projelerim\K2501_NWD_FiatBravo198_CUSTOM` |
| Keystore | `signing/` (CI secret'tan üretiliyor) |
| Cihaz ADB | wireless, `adb connect <IP>:5555` — USB HOST mode engelli |

## 3. GitHub repoları

| Repo | Amaç |
|---|---|
| `Brst62/fiat-bravo-ota` | Bu repo. APK release'leri buraya tag ile basılır. Cihaz `releases/latest`'i poll'lar. |
| `Brst62/fiat-bravo-telemetry` | Cihazın push'ladığı `.tar.gz` dump'ları. Klasör şeması: `telemetry/{platform}/{deviceId}/{YYYYMMDD}/<dosya>.tar.gz` |
| GitHub Actions | `.github/workflows/release.yml` — tag push'unda CI imzalı APK üretir, release'e yükler. |
| PAT'lar | İki ayrı PAT: telemetry repo'su için contents:write, OTA repo'su için contents:read. Cihaza `scripts/install_pat.ps1` ile bir kez kuruluyor. |

## 4. Modüller (Gradle multi-project)

```
:app                    HOME launcher + DiagnosticActivity UI (TEK gerçek UI burada)
:diagnostic:collector   Reader'lar + tar.gz üretici + WorkManager job
:diagnostic:uploader    GitHub Contents API client (PAT + secret store)
:ota:github             GitHub Releases poller + APK installer
:core:*                 (canbus, obd2, ai, common) — common dolu, diğerleri BOŞ stub
:feature:*              (dashboard, radio, settings, ...) — hepsi BOŞ stub
:externalapp:*          eski NWD APK iskelet'leri — kullanılmıyor
```

## 5. Telemetri pipeline'ı (önemli — çalışma şekli)

1. `DiagnosticWorker` (WorkManager) → `DiagnosticCollector.collect()` çağırır
2. Collector tüm **Reader**'ları çağırır (Battery, Logcat, Network, Package, Proc, Storage, SysProp, Bluetooth, Usb, Display, Sensor, Audio, NwdInventory, Dumpsys)
3. `DiagnosticReport` (Moshi/KSP JSON) + logcat dump → `tar.gz` → `filesDir/telemetry-outbox/`
4. `GitHubTelemetryUploader` outbox'taki dosyaları **Contents API PUT** ile telemetry repo'sunda commit'ler
5. Cihaz kimliği: `SHA-256(ANDROID_ID)`'nin ilk 16 hex char'ı → `dev-xxxxxxxxxxxxxxxx` (anonim)
6. `platformTag`: `navimex` / `k2501-t507` / `unknown-<board>` — build'den çıkarılır

**WiFi-only** (`NetworkType.UNMETERED`), **pil düşük değilken** çalışır.

## 6. OTA pipeline'ı

1. Geliştirici `git tag vNN` push'lar
2. `release.yml` keystore ile imzalı APK üretir, GitHub Release'e yükler
3. `GitHubOtaWorker` cihazda periyodik `releases/latest` GET çağırır
4. Yeni `versionCode > kurulu` ise APK indir → `PackageInstaller` → kullanıcı onayı
5. **Tag adı = versionCode**: `v22` → 22, `v23` → 23 (monoton artan)

## 7. Mevcut durum (kontrol et: `git status` + `git log --oneline -5`)

**Son release**: `v22` (commit `38173be`).
**HEAD = v22** (rev-list count = 0).
**Uncommitted çalışma var** (önceki session'dan kalma, henüz commit'lenmedi):
- 7 yeni reader dosyası untracked: BluetoothReader, UsbReader, DisplayReader, SensorReader, AudioReader, NwdInventoryReader, DumpsysReader
- `DiagnosticReport.kt` schema'sına 7 yeni alan eklenmiş (modified, uncommitted)
- `DiagnosticCollector.kt` **eski 7 alanı** dolduruyor — yeni schema ile **derlenmez** durumda. Henüz bağlanmamış.

Yani: **v22 APK telemetride minimal** (Battery/Logcat/Network/Package/Proc/Storage/SysProp), yeni 7 reader **henüz cihaza gitmedi**.

## 8. v23'ün hedefi (sıradaki release)

1. `DiagnosticCollector.kt`'ye 7 yeni reader çağrısını ekle, schema'yı tamamla
2. Worker cadence: 6 saat → **15 dakika** (geliştirici modu — sık veri)
3. Launcher `onResume`'a manuel tetikleyici (her açılışta WorkManager.enqueueUniqueWork)
4. Manifest izinleri: `BLUETOOTH_CONNECT` (API 31+), zaten varsa atla
5. `version_name` "v23", `versionCode = 23`
6. Local `assembleDebug` + `lintDebug` PASS
7. Commit + push + tag `v23` → CI imzalı APK çıkarır
8. APK Desktop'a iner, cihaza kurulur

## 9. Reader imzaları (collector'a bağlarken referans)

| Reader | Çağrı | Dönüş |
|---|---|---|
| `BatteryReader.snapshot(ctx)` | nullable | `BatteryInfo?` |
| `LogcatReader.dump(dir)` | `File` döner; `recentCrashes(file)` ayrı | — |
| `NetworkReader.snapshot(ctx)` | nonnull | `NetworkInfo` |
| `PackageReader.list(ctx)` | nonnull | `List<PackageInfoEntry>` |
| `ProcReader.snapshot()` + `.thermalZones()` | nonnull | maps |
| `StorageReader.snapshot(ctx)` | nonnull | `StorageInfo` |
| `SysPropReader.read()` | nonnull | `Map<String, String>` |
| `BluetoothReader.snapshot(ctx)` | nullable | `BluetoothInfo?` |
| `UsbReader.snapshot(ctx)` | nonnull | `UsbInfo` |
| `DisplayReader.snapshot(ctx)` | nonnull | `List<DisplayInfo>` |
| `SensorReader.snapshot(ctx)` | nonnull | `List<SensorInfo>` |
| `AudioReader.snapshot(ctx)` | nullable | `AudioInfo?` |
| `NwdInventoryReader.snapshot(ctx)` | nonnull | `List<NwdPackageInfo>` |
| `DumpsysReader.snapshot()` | nonnull | `Map<String, String>` |

## 10. Geliştirme akışı (her v++ release için)

```powershell
cd "C:\Users\baris.tas\Desktop\projeler\Python Projelerim\FiatBravo198_APP"
# 1. kod değişikliği
.\gradlew assembleDebug lintDebug    # local doğrula
git add -A
git commit -m "feat: ..."
$next = 23   # versionCode'a göre
git tag v$next
git push origin main "v$next"
# 2. CI 3-5 dk içinde release atar
gh release view "v$next"             # asset URL'sini gör
gh release download "v$next" -p "app-release.apk" -D "$env:USERPROFILE\Desktop"
# 3. cihaza
adb connect <IP>:5555
adb install -r "$env:USERPROFILE\Desktop\app-release.apk"
```

## 11. Önemli notlar (Claude → kendine)

- **API error riski**: tek turda 7+ dosya yazma + 30+ tool call → context patlatır. **Her v++ için adımları böl**: schema fix → cadence → build → release dört ayrı tur olabilir.
- **PAT'lar repo'da saklı değil**: `signing/` dışında local.properties veya CI secrets'tan gelir. Asla commit edilmez.
- **`canLogPath`**: eski `com.nwd.aibrain` APK'sının CSV dump'ını gösterir; CAN okuma değil, sadece path raporlar.
- **Gerçek CAN okuma yok**: `:core:obd2` ve `:core:canbus` BOŞ. ELM327 BT-SPP client v23 sonrası iş.
- **Eski APK'lar referans**: kullanıcı "NWD APK'ları check et" derse → muhtemelen telemetry reposundaki cihaz dump'larında `nwdInventory` alanını okumamı + eski APK davranışını anlamamı istiyor.
