# Troubleshooting guide

## The module shows nothing in LSPosed

- LSPosed requires Zygisk enabled in Magisk. Confirm Zygisk is on and the device is arm64.
- Confirm the APK actually installed (`adb shell pm list packages | grep velocity`).
- Reinstall or reboot after enabling the module.

## Dashboard shows "not reported" for one or both processes

- The module scope must include `com.android.systemui` and
  `com.sec.android.app.launcher` (LSPosed → Velocity → Scope).
- A reboot is needed once after scope changes so the processes restart with the module.
- If you force-stopped the module's app, its ContentProvider is unavailable and target
  processes cannot reach configuration. Open the Velocity app once, or reboot.

## A hook shows UNSUPPORTED

This is a no-harm outcome, not an error: your firmware does not carry the expected class or
method, so the module left that optimization uninstalled. The GC-suppression hook cannot be
UNSUPPORTED (it targets frozen boot-classpath APIs); the SystemUI log-suppression hook can
be, if Samsung's One UI 8 build relocated the AOSP LogBuffer. Check the Optimizations tab —
the status chip tells you exactly which process reported it.

## A hook shows FAILED

The module already left the hook uninstalled — the host process is not in a broken state.
Read the detail text in the Optimizations tab and the LSPosed log (Modules → Velocity →
Log) to see the exception. Report it with your firmware version.

## SystemUI misbehaves after enabling something

1. Switch the profile to **Balanced** (or turn the suspect hook off). It unhooks within a
   second — no reboot needed.
2. If SystemUI still looks wrong: LSPosed → Velocity → disable the module, then reboot.
3. The module never hooks system_server or zygote, so a boot loop is not an expected failure
   mode. If your device ever boot-loops with LSPosed enabled, disable LSPosed in Magisk
   first — that isolates the cause definitively.

## Collecting logs for a bug report

- LSPosed → Modules → Velocity → Log (everything the module did, per process).
- `adb logcat -d > logcat.txt` right after reproducing.
- Screenshots of the Dashboard and Optimizations tabs.
- Firmware: Settings → About → build number and One UI version.

## Resetting all configuration

Clear the app's data (Settings → Apps → Velocity → Storage → Clear data) or uninstall and
reinstall. Configuration lives entirely in the app's private files directory — nothing is
left in any other process after the module is disabled.

## Known behavioural notes

- Configuration changes while a diagnostics session is running apply after the session
  finishes (the session holds a main-thread callback for its 10 s).
- If the module app is force-stopped, live switching stops working until it is opened once
  (its provider must be reachable for target processes to fetch config).
