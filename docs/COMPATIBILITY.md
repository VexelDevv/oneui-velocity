# Compatibility documentation

## Target environment

| Item | Value |
|---|---|
| UI framework | Samsung One UI 8 |
| Android | 16 (API 36) |
| Architecture | arm64-v8a (any ABI works: module is pure JVM code) |
| Root | Magisk + Zygisk + LSPosed |
| Module API | Legacy Xposed API 82 (`de.robv.android.xposed:api`) — fully supported by current LSPosed |

## Scope of supported processes

| Process | Package | Notes |
|---|---|---|
| SystemUI | `com.android.systemui` | Main process only (child processes are filtered out by process name). |
| Samsung Home | `com.sec.android.app.launcher` | Samsung's One UI launcher package name, stable across One UI generations. |

Deliberately NOT hooked: `android` (system_server), zygote, and arbitrary apps. This bounds
any failure to a UI process that Android restarts automatically.

## Hook-by-hook compatibility

### Explicit GC suppression — confidence: high

`java.lang.System.gc()` and `java.lang.Runtime.gc()` are boot-classpath APIs frozen since
API 1. They resolve on every Android build. No vendor divergence is possible; the hook is
deterministic.

### SystemUI verbose log suppression — confidence: medium-high, device-verified at runtime

Implemented against `com.android.systemui.log.core.LogBuffer`, present in AOSP since
Android 12 and carried into One UI 8's AOSP-16 base. Samsung customizes SystemUI heavily but
generally keeps AOSP package structure for shared infrastructure classes.

Runtime behaviour on divergence:

- If the class or the one-argument `log` method is missing, the hook reports **UNSUPPORTED**
  and installs nothing.
- If the `LogMessage.getLevel()` accessor differs, the interceptor's guarded lookup fails
  per-entry and every log entry passes through untouched (fail-open by design).

So the worst case is "no effect", never "broken logging".

## One UI 8 on Galaxy Note10+ — important caveat

The Galaxy Note10+ (SM-N975/N976) officially ended its One UI lifecycle at One UI 5.x /
Android 13. A Note10+ running One UI 8 is running an **unofficial port**. Consequences:

- The port's fidelity determines class availability. Use the module's own status reporting
  (Optimizations tab) as ground truth for what your build carries.
- The port itself — not this module — is the primary source of any instability risk on that
  device. The module's auto-disable behaviour is designed for exactly this situation.
- ARM64 (arm64-v8a) is fully supported; the module is pure JVM code.

## Known non-goals (by design, not by omission)

- **Samsung GOS / Game Optimizing Service**: intentionally untouched. It is Samsung's
  thermal management; bypassing it violates the project's stability rules.
- **Animation duration scaling** (`ANIMATOR_DURATION_SCALE` etc.): rejected as placebo.
- **Input pipeline**: `InputDispatcher` and `InputReader` are native code in system_server;
  Java Xposed hooks cannot reach them. Documented rather than faked.
- **RenderThread / GPU pipeline**: native and already heavily optimized; Java-layer
  intervention there adds overhead rather than removing it.

## Versioning policy

- `1.x` targets One UI 8 only.
- Breaking One UI version changes (One UI 9 / Android 17) will be a new target matrix with
  re-verification, not silent support.
