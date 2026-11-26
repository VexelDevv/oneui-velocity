# Installation guide

## Prerequisites

- Samsung device running **One UI 8 (Android 16, API 36)**. See `COMPATIBILITY.md` for the
  Note10+ port caveat.
- Magisk with **Zygisk enabled**, and **LSPosed** installed and working (LSPosed itself
  requires Zygisk).
- ADB available for the optional verification steps (not required for normal use).

## 1. Get the APK

**From CI (recommended):**

1. Go to the repository → **Actions** → **Build APK** → **Run workflow**.
2. Wait for the run to finish, open it, download the `velocity-oneui-apk` artifact.
3. Unzip it (Actions artifacts are zipped) to get `app-release.apk`.

The CI artifact is debug-signed, so it installs directly.

**Locally:**

```
# JDK 17, Android SDK with platform 36, Gradle 8.13
gradle :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

Android Studio: open the project, let it sync, Build → Build APK(s).

**Redistributing your own release build:** replace the debug signing config in
`app/build.gradle.kts` with your own keystore:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("release.keystore")
        storePassword = System.getenv("KS_PASS")
        keyAlias = "velocity"
        keyPassword = System.getenv("KEY_PASS")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## 2. Install

```
adb install app-release.apk
```

or sideload the APK normally. The app is `minSdk 36`, so it installs only on Android 16+,
which matches the One UI 8 target.

## 3. Enable in LSPosed

1. Open **LSPosed** (from Magisk's Zygisk modules list).
2. **Modules** tab → **Velocity for One UI** → enable it.
3. **Scope**: tick **com.android.systemui** (SystemUI) and **com.sec.android.app.launcher**
   (Samsung Home). Nothing else is required or recommended.

## 4. Reboot once

LSPosed loads the module entry class when each target process starts. A reboot ensures both
SystemUI and Samsung Home restart with the module attached.

## 5. Verify

Open **Velocity for One UI**:

- **Dashboard** should show both processes as *reported* with a recent timestamp and
  the module version. If nothing reports, re-check the scope and reboot again.
- **Optimizations** shows each hook's per-process status:
  - `ACTIVE` — installed and working,
  - `UNSUPPORTED` — the firmware does not carry the expected class/method, hook left
    uninstalled (this is a no-harm outcome, not an error),
  - `FAILED` — unexpected exception during install; the module automatically left the
    hook uninstalled,
  - `NOT REPORTED` — that process has not checked in yet.

## 6. Choose a profile

- **Balanced** — original behaviour, diagnostics armed (default).
- **Performance** — GC suppression active.
- **Maximum** — GC suppression + SystemUI verbose log suppression.

Switching is live: the hooked processes pick up the change within about a second (config is
written, then a `ContentObserver` in each process diffs and applies/reverts hooks).

## 7. Optional: measure

Run a 10 s frame-cadence session in the Diagnostics tab, module off vs on, and follow
`BENCHMARKING.md` for the full A/B protocol. Only keep optimizations that measurably help
your device.
