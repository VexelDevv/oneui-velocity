# Hook reference and engineering rationale

This document is the ground truth for what the module does, why, and — equally important —
what it refuses to do. It follows one rule: **a hook ships only if it has a defensible,
measurable benefit AND a contained failure mode.**

## Shipped hooks

### 1. `no_op_explicit_gc` — Explicit GC suppression

- **Processes**: SystemUI, Samsung Home.
- **Intercepts**: `java.lang.System#gc()`, `java.lang.Runtime#gc()`.
- **Mechanism**: `beforeHookedMethod` → `param.setResult(null)` (skips the original call).
  The call is advisory; skipping it never changes program semantics.
- **Rationale**: vendor UI code occasionally issues explicit full-GC requests (for example
  after configuration changes). A full stop-the-world collection landing inside a
  frame-critical window is one of the few *guaranteed* jank producers a Java-level module
  can eliminate without touching the heap, thresholds, or the runtime collector.
- **Safety**: ART's own concurrent collector, heap growth and zygote limits are untouched.
  Only explicit requests are suppressed.
- **Unhook**: fully reversible at runtime.
- **How to measure**: count explicit full-GC events in ART logs during a scripted session,
  module off vs on; compare `dumpsys gfxinfo` jank percentiles (BENCHMARKING.md).

### 2. `system_ui_log_suppression` — SystemUI verbose log suppression

- **Process**: SystemUI only.
- **Intercepts**: `com.android.systemui.log.core.LogBuffer#log(...)` (one-arg), resolved by
  name and arity at install time.
- **Mechanism**: reads the message's log level via the `getLevel` accessor; below-WARNING
  entries are skipped (`setResult(null)`). WARNING/ERROR/WTF always pass. Every access is
  guarded: on any lookup failure the entry passes through untouched.
- **Rationale**: AOSP SystemUI funnels internal logging into LogBuffer rings filled on the
  calling thread — frequently the main thread, during notification storms. Dropping verbose
  enqueue work removes main-thread bookkeeping while keeping error visibility.
- **Samsung caveat**: if this firmware relocated the class, the hook reports UNSUPPORTED
  and installs nothing. Never fails loudly.
- **How to measure**: notification-storm A/B (BENCHMARKING.md §3) with jank percentiles.

### 3. Frame-cadence diagnostics (instrumentation, not a modification)

On-demand 10 s Choreographer sampling per target process. Measures main-thread vsync
callback gap distribution (median/p90/p99/max, share of windows missed > 1.5x median).
Zero cost while inactive; no polling. Explicitly labelled a *proxy* for main-thread
congestion — SurfaceFlinger frame-deadline accounting remains the authority (`gfxinfo`,
Perfetto).

## Design rules honoured by the implementation

1. **Method objects resolved once** at install; nothing per-frame, no repeated reflection.
2. **No hot-path hooks** outside diagnostics sessions.
3. **Fail-open semantics**: exceptions inside interceptors pass the original call through.
4. **No service, no receiver, no polling**; one io thread per target for provider traffic.
5. **Live reversal**: profile/toggle changes unhook within a second, no reboot.

## Rejected candidates (and why)

| Candidate | Why rejected |
|---|---|
| Global animation scale (`ANIMATOR_DURATION_SCALE` etc.) | The classic placebo: it *removes* One UI's animation design rather than making it faster. The project goal is smoothness with the original visual identity, so this is disqualified by requirement, not feasibility. |
| Input event latency "optimization" | `InputReader`/`InputDispatcher` are native system_server code; Java Xposed cannot hook them. Touch latency is dominated by touchpanel firmware, driver batching, and vsync-aligned dispatch — none reachable from app processes. Any module claiming to "fix touch latency" via Xposed is fake. |
| `Choreographer` / `AnimationHandler` scheduling tricks | Animations are vsync-aligned by design; the earliest possible first frame is the next vsync. There is no scheduling slack to reclaim at the Java layer. |
| RenderThread / HWUI "optimization" | RenderThread is native, already THREAD_PRIORITY-classed appropriately, and GPU-bound. A Java hook in the draw path adds overhead exactly where the frame budget lives. |
| `SurfaceFlinger` property overrides (`debug.sf.*`, refresh-rate pinning) | System-property and native level; belongs to init/root scripting, not an LSPosed module's reach or responsibility. Also risks display-power regressions on Samsung's adaptive refresh. |
| Samsung GOS / thermal tampering | GOS is thermal management. Bypassing it violates the project's hard stability rules. |
| Background app killing / memory "boosters" | Aggressive process killing trades battery and reliability for placebo responsiveness and breaks Android's memory management; explicitly out of scope. |
| Zygote preloading, class-preload injection | Out of module reach; no verified benefit; high boot-loop risk. |
| Binder thread pool expansion in SystemUI | Affects scheduling of the whole process group with no attributable frame-path benefit; unverifiable win, unbounded risk. |

## Future candidates (blocked on on-device verification)

These are plausible but intentionally **not shipped** until someone verifies them against
One UI 8 firmware and measures the effect:

- `Surface#setFrameRate` voting strategy for SystemUI surfaces (requires decompiling One UI 8
  SystemUI's refresh-rate vote logic; wrong votes regress battery).
- Notification icon/shell inflation reuse (requires verifying Samsung's inflation classes).
- Launcher `RecyclerView` prefetch tuning (needs One UI 8 Home decompilation; wrong values
  regress scroll smoothness).

Adding any of them without on-device verification would violate the project's core rule, so
they stay in this list instead of the code.
