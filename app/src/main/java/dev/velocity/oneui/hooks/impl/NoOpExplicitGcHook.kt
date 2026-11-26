package dev.velocity.oneui.hooks.impl

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import dev.velocity.oneui.core.HookContext
import dev.velocity.oneui.core.HookIds
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.diagnostics.DiagLog
import dev.velocity.oneui.hooks.Hook
import dev.velocity.oneui.hooks.UnsupportedHookException

/**
 * Suppresses explicit garbage-collection requests in target UI processes.
 *
 * What it does: turns System.gc() and Runtime.getRuntime().gc() calls issued
 * by SystemUI / Samsung Home code into no-ops, so vendor code cannot request
 * a full stop-the-world collection inside a frame-critical window (for
 * example right after a configuration change or animation completes).
 *
 * Why it is safe:
 * - Explicit GC in Java is advisory; no semantic guarantee is attached to
 *   a completed collection by any correctly written framework code.
 * - Memory pressure is untouched: the ART runtime still runs its own
 *   concurrent collector on its own thresholds. This hook removes only
 *   vendor-initiated requests, never the runtime's own behaviour.
 * - Boot-classpath APIs frozen since API 1; no vendor divergence possible.
 *
 * Measurable effect: count "explicit" full GC events in ART logs during a
 * scripted interaction session, with the hook off vs on (see
 * docs/BENCHMARKING.md).
 */
class NoOpExplicitGcHook : Hook(
    id = HookIds.NO_OP_EXPLICIT_GC,
    title = "Explicit GC suppression",
    description = "No-ops advisory System.gc()/Runtime.gc() calls inside SystemUI and Samsung Home.",
    processes = setOf(Process.SYSTEM_UI, Process.LAUNCHER),
) {
    override fun install(context: HookContext): List<XposedBridge.Unhook> {
        val interceptor = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Skipping an advisory call never changes program semantics.
                param.setResult(null)
            }
        }

        val unhooks = mutableListOf<XposedBridge.Unhook>()
        var failures = 0

        try {
            unhooks += XposedBridge.hookMethod(
                System::class.java.getDeclaredMethod("gc"),
                interceptor,
            )
        } catch (t: Throwable) {
            failures++
            DiagLog.w("System.gc hook unavailable: $t")
        }

        try {
            unhooks += XposedBridge.hookMethod(
                Runtime::class.java.getDeclaredMethod("gc"),
                interceptor,
            )
        } catch (t: Throwable) {
            failures++
            DiagLog.w("Runtime.gc hook unavailable: $t")
        }

        if (unhooks.isEmpty()) {
            throw UnsupportedHookException("java.lang.System/Runtime gc() not resolvable ($failures lookups failed)")
        }
        return unhooks
    }
}
