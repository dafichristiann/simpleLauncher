# 07 - Build & Run

Toolchain is **already installed on this machine (all on D:)** as of Session 2.
This file records the exact environment + commands used.

---

## 0. Installed environment (verified)

| Tool | Path / value |
|---|---|
| JDK 17 (Temurin 17.0.20.1) | `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` |
| `JAVA_HOME` | set (User env) to the JDK path above |
| Android SDK | `D:\Android\Sdk` (`ANDROID_HOME` / `ANDROID_SDK_ROOT`) |
| SDK packages | `platform-tools`, `platforms;android-35`, `build-tools;35.0.0`, `emulator`, `system-images;android-35;google_apis;x86_64` |
| Gradle (standalone) | `D:\Android\gradle-8.11.1` (used once to generate the wrapper) |
| Gradle caches | `GRADLE_USER_HOME=D:\gradle-cache` |
| AVD | `D:\Android\AVD` (`ANDROID_AVD_HOME`), AVD name `soft_home_pixel` |
| Temp / downloads | `TEMP`/`TMP` = `D:\Android\downloads\temp` |
| Android Studio IDE | **not installed** (not needed; cmdline-tools + wrapper suffice) |

> Note: env vars are set at the **User** scope, so a newly opened terminal picks
> them up. In an existing shell, set them for the session (see below).

---

## 1. Set env vars for the current PowerShell session

```powershell
$env:JAVA_HOME       = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME    = "D:\Android\Sdk"
$env:ANDROID_SDK_ROOT= "D:\Android\Sdk"
$env:GRADLE_USER_HOME= "D:\gradle-cache"
$env:ANDROID_AVD_HOME= "D:\Android\AVD"
```

---

## 2. Build

```powershell
# from repo root (D:\Perkuliahan\semester 7\menu)
.\gradlew.bat :app:assembleDebug      # debug APK
.\gradlew.bat :app:installDebug       # build + install to a running device/emulator
.\gradlew.bat test                    # all unit tests
.\gradlew.bat lint                    # android lint
```

APK output: `app\build\outputs\apk\debug\app-debug.apk`
(debug package id is `com.softhome.launcher.debug`.)

---

## 3. Emulator

```powershell
# boot the AVD (created in Session 2)
Start-Process "$env:ANDROID_HOME\emulator\emulator.exe" `
  -ArgumentList "-avd","soft_home_pixel","-no-audio","-no-boot-anim","-gpu","swiftshader_indirect"

# wait for boot
& "$env:ANDROID_HOME\platform-tools\adb.exe" wait-for-device
```

---

## 4. Run & set as default launcher

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start `
  -n com.softhome.launcher.debug/com.softhome.launcher.HomeActivity
```

Set as default (two ways):
- **On device:** Settings -> Apps -> Default apps -> Home app -> SOFT / HOME
- **Or adb (used in Session 2 to verify):**
```powershell
adb shell cmd package set-home-activity com.softhome.launcher.debug/com.softhome.launcher.HomeActivity
adb shell input keyevent KEYCODE_HOME   # should show SOFT / HOME
```

Useful adb:
```powershell
adb devices
adb logcat | Select-String "softhome"
adb shell cmd package query-activities -c android.intent.category.HOME -a android.intent.action.MAIN
```

---

## 5. Import an icon pack (P1.5)

Two ways to apply a pack, both from the drawer's **Icon pack** action (top-right):

**A. From an installed icon-pack app**
Install any pack from the store; it advertises itself via a standard intent
(`org.adw.launcher.THEMES`, `com.novalauncher.THEME`, ...). Reopen the sheet -
it lists them under "Pack apps on this device". Tap one to apply.

**B. From a `.zip` file (how P1.5 was verified on `soft_home_pixel`)**
A pack zip is just `appfilter.xml` at the root plus a drawable folder:

```
SoftMonoTest.zip
├─ appfilter.xml                 # <item component="ComponentInfo{pkg/cls}" drawable="name"/>
├─ res/drawable-xxhdpi/
│   ├─ pack_camera.png
│   └─ ...
```

Push it and pick it in-app:

```powershell
# 1. push a pack to the device's Downloads
& "$env:ANDROID_HOME\platform-tools\adb.exe" push .\SoftMonoTest.zip /sdcard/Download/

# 2. in the app: swipe up (drawer) -> "Icon pack" -> "Import a .zip file"
#    -> the system picker opens -> Downloads -> SoftMonoTest.zip
#    progress is shown inline; on success the grid re-renders with decoded icons.
```

Build a throwaway pack for testing without an emulator app: any zip with a valid
`appfilter.xml` and at least one PNG under `res/drawable*/` for a mapped component
will import. Verify the *components* first with:

```powershell
adb shell pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
```

Notes:
- The active pack is **in-memory for now** (reset on app kill) - see docs/04 #36.
- Picking a `.zip` that isn't a pack (no drawable folder / unreadable) shows
  "Import failed: ..." and leaves the previous state intact.

---

## 6. Troubleshooting

| Symptom | Fix |
|---|---|
| `java` not found | Set `$env:JAVA_HOME` for the session (section 1) or reopen the terminal. |
| Gradle out-of-space | `GRADLE_USER_HOME` must be on D:. |
| Emulator slow / won't boot | Ensure the AVD is on D: and GPU is `swiftshader_indirect`. |
| App not listed as Home app | `HomeActivity` must keep `CATEGORY_HOME` + `DEFAULT` (it does). |
| Icon pack not detected | `appfilter.xml` at zip root or under a folder. See [08](08-ICONPACK-FORMAT.md). |
| `Cmd package set-home-activity` fails | The activity name must be the **debug** package id. |
| Imported pack shows no decoded icons | The `appfilter` `drawable` names must match files under `res/drawable*/`; check casing. |
| Signature mismatch on `install -r` | Debug keystore changed between machines: `adb uninstall com.softhome.launcher.debug` then install fresh. |

---

## 7. CI (later)

A GitHub Actions workflow can run `./gradlew test` + `assembleDebug` on push.
Not set up yet.
