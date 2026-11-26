# Benchmarking methodology

This module's contract: **no claimed number without a measurement.** Nothing below is a
result — it is the protocol you run to produce your own results. Fill the template at the
end for every comparison you publish.

## 0. Test hygiene (applies to every protocol)

- Fixed brightness (50%), battery > 50%, charger unplugged, airplane mode on, stable
  temperature: run a 5-minute warm-up before sampling; skip a session if the device is
  thermally warm to the touch.
- Same firmware build, same installed apps, same wallpaper across all conditions.
- 5 repetitions per condition; report the median, not the best.
- Conditions in this order: module disabled in LSPosed (baseline) → module on with the
  profile under test. Wait 10 s between sessions for observer/binder settle.

## 1. Frame timing and deadline misses — `dumpsys gfxinfo`

The authoritative app-process frame accounting:

```
adb shell dumpsys gfxinfo com.android.systemui reset
# ... run the scripted interaction for 60 s ...
adb shell dumpsys gfxinfo com.android.systemui
```

Repeat for `com.sec.android.app.launcher`. Record from the output:

- Total frames rendered, **Janky frames** (count and %),
- the percentile table (50th / 90th / 95th / 99th),
- number Missed Vsync, number Slow UI thread (main-thread-bound jank — the component this
  module targets).

## 2. Frame pacing consistency — Perfetto

```
adb shell perfetto -t 30s -o /data/misc/perfetto-traces/t.pb sched
adb pull /data/misc/perfetto-traces/t.pb
```

Open the trace at https://ui.perfetto.dev. Check `sched_waking` latency for the
`com.android.systemui` main thread during interaction bursts, and main-thread runqueue
delay. For deeper analysis use a config with `track_event` + `ftrace` and inspect
Choreographer `doFrame` slices' inter-arrival distribution.

## 3. SystemUI responsiveness (notification storm)

Scripted interaction: with `adb shell cmd notification post ...` or 100 pre-loaded
notifications, swipe the shade fully open and closed 30 times at fixed pace (use a
metronome or screen-recorded cadence).

Record per condition: gfxinfo jank % (protocol 1) and, if measuring the log-suppression
hook, main-thread time in `LogBuffer` from a Perfetto trace.

## 4. Launcher responsiveness

Open the app drawer and fling-scroll it top to bottom 30 times at fixed pace; then open and
close the drawer 30 times. Record gfxinfo percentiles for
`com.sec.android.app.launcher`.

## 5. Explicit-GC event counting (for the GC hook)

```
adb logcat -c
# ... run the scripted interaction ...
adb logcat -d -v time | grep -i -c "GC explicit"
```

Count explicit full-GC events, module off vs on. Zero explicit events with the hook on is
the direct, expected mechanism-level effect. Frame-level benefit is validated through
protocols 1–2.

## 6. In-module quick sessions

The Diagnostics tab runs a 10 s Choreographer cadence session per process. Interpretation:
median near your display's frame interval with low p99 and low missed-window share = calm
main thread; elevated p90/p99 during the same scripted interaction = congestion. This is a
relative, same-device comparison signal — not a replacement for protocols 1–2.

## 7. Input-to-display latency (where measurable)

Honest scope: not hookable via Xposed, and only externally measurable. The accessible
protocol: 240 fps slow-motion camera, screen and hand in frame, tap a fixed target, count
frames from finger contact to first visual response; 1 frame at 240 fps = 4.17 ms.
Report `median of 30 taps` per condition. Use it to document the *absence* of regression —
this module does not claim input-latency improvement.

## 8. CPU / memory / battery overhead

```
adb shell dumpsys batterystats --charged com.android.systemui | head -40
adb shell top -n 1 -o PID,PCY,%CPU | grep -E "systemui|launcher"
```

Overhead contract: the module contributes ~zero steady-state CPU (no services, no polling;
one io thread blocked on a handler most of the time). Verify with a 30-minute idle screen-on
comparison.

## Result template (fill per condition)

```
Device / firmware:
Module state: disabled | profile: balanced | performance | maximum
 gfxinfo SystemUI: jank % = __ , p50/p90/p99 = __ / __ / __ ms, missedVsync = __
 gfxinfo Home:     jank % = __ , p50/p90/p99 = __ / __ / __ ms
 explicit-GC events during 60s storm: __
 frame-cadence session (SystemUI): median __ ms, p99 __ ms, missed __ %
 input-latency taps: median __ frames @240fps (only if measured)
 notes: __
```

Publishing rules: report the medians, keep the raw logs, and state the module version and
firmware. If a metric did not improve, say so — a null result is a result.
