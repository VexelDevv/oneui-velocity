# Velocity for One UI 8

A **measurement-first LSPosed performance module for Samsung One UI 8 (Android 16, API 36) this was coded by https://t.me/V3exl**.

Velocity makes SystemUI and Samsung Home more responsive through a small set of safe,
individually verified optimizations, live performance profiles, and built-in frame-cadence
diagnostics that let you validate every claim **on your own device, with your own numbers**.

> **Honesty first.** This project does not promise fake frame rates, does not shorten
> animation durations globally, does not kill background apps, does not bypass thermal
> protections (Samsung GOS is intentionally untouched), and does not publish invented
> benchmark numbers. Every optimization it ships is either a frozen public API or an
> AOSP-origin class verified at runtime — and anything that does not match your firmware
> reports `UNSUPPORTED` instead of pretending to work.

## What it actually does

| Optimization | Process | Mechanism | Verification level |
|---|---|---|---|
| **Explicit GC suppression** | SystemUI, Samsung Home | `java.lang.System.gc()` / `Runtime.gc()` become no-ops inside target processes, so vendor code cannot request a full stop-the-world collection in a frame-critical window. ART's own concurrent collector and all memory limits are untouched. | Stable boot-classpath API (frozen since API 1) |
| **SystemUI verbose log suppression** | SystemUI | Below-WARNING `LogBuffer` enqueue work is dropped during notification storms. WARNING/ERROR/WTF survive; logging can never break — failures pass entries through. | AOSP-origin class (`com.android.systemui.log.core.LogBuffer`); auto-reports `UNSUPPORTED` if this firmware relocated it |
| **Frame-cadence diagnostics** | SystemUI, Samsung Home | On-demand 10 s Choreographer sampling of main-thread vsync callback gaps: median/p90/p99/max + missed-window share. Zero cost while inactive. | Instrumentation, not a modification |

Profiles: **Balanced** (everything off, diagnostics armed), **Performance** (+ GC suppression),
**Maximum** (+ log suppression). Switching is **live** — hooks install and unhook within a
second, no reboot. Per-hook overrides sit on top of profiles.

See [docs/HOOKS.md](docs/HOOKS.md) for the full engineering rationale, including the long
list of **rejected** "speed module" tricks (global animation scaling, input pipeline tampering,
GPU/render-thread "optimizations", thermal tampering) and why each was rejected.

## Architecture

```
dev.velocity.oneui
  core/        Entry point, process allowlist, io thread, constants
  catalog/     Pure-Kotlin hook metadata for the UI (no Xposed imports)
  config/      JSON config + exported ContentProvider (app <-> target bridge)
  hooks/       Hook base, registry, GC suppression hook
  systemui/    SystemUI LogBuffer suppression hook
  profiles/    Profile enum + ProfileManager (diff-based live apply/revert)
  rendering/  FramePacingMonitor, FrameStats (diagnostics only)
  diagnostics/ Ring-buffer log, status reporter, benchmark session controller
  ui/          Compose Material You configuration app
```

Safety properties:

- **Scope discipline**: only the main process of `com.android.systemui` and
  `com.sec.android.app.launcher`. No system_server, no zygote, no arbitrary apps — a failure
  is contained inside a restartable UI process.
- **No services, no polling, no wakeups**: one lazy io thread per target process used solely
  for provider traffic; config changes are pushed via `ContentObserver`, not timers.
- **No hot-path reflection**: `Method` objects are resolved once at install; nothing runs
  per-frame except during an explicit diagnostics session.
- **Every hook is reversible live** and individually status-reported (ACTIVE / OFF /
  UNSUPPORTED / FAILED) to the UI.

## Getting the APK

The repository builds the APK for you:

1. Open the repo on GitHub → **Actions** → **Build APK** → **Run workflow** (or any push to
   `main` triggers it).
2. Download the `velocity-oneui-apk` artifact.

CI builds a release APK signed with the debug key so it installs out of the box. To
redistribute your own build, see [docs/INSTALL.md](docs/INSTALL.md) for keystore steps.

To build locally: Android Studio (or `gradle :app:assembleRelease` with Gradle 8.13,
JDK 17, Android SDK platform 36).

## Install & set up

1. Install the APK.
2. LSPosed → Modules → enable **Velocity for One UI**.
3. Module scope: `com.android.systemui` and `com.sec.android.app.launcher`.
4. Reboot once.
5. Open Velocity: the Dashboard should show both processes as reported. Pick a profile.

Full instructions: [docs/INSTALL.md](docs/INSTALL.md). Problems:
[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md). Publishing your own copy:
[docs/PUBLISHING.md](docs/PUBLISHING.md).

## Benchmarking

The module never claims a number it did not measure. The methodology in
[docs/BENCHMARKING.md](docs/BENCHMARKING.md) covers:

- `dumpsys gfxinfo` jank and percentile capture for SystemUI and Home,
- Perfetto trace protocol (`perfetto -t 30s -o /data/misc/perfetto-traces/t.pb sched`),
- explicit-GC event counting from ART logs,
- in-module 10 s frame-cadence sessions (module off vs on),
- a fixed A/B protocol (warm-up, scripted interaction, repetitions, medians).

Run it, fill in the template, and keep whatever survives honest measurement.

## Compatibility

Targets One UI 8 / Android 16 / API 36. Note: the Galaxy Note10+ never received One UI 8
officially (Samsung ended its line at One UI 5.x), so on that device this module targets a
custom One UI 8 port — class-verification status in the app will tell you what your port
carries. Details: [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## License

MIT. See [LICENSE](LICENSE).
