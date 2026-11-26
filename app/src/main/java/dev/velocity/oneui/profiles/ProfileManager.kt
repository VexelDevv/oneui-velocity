package dev.velocity.oneui.profiles

import de.robv.android.xposed.XposedBridge
import dev.velocity.oneui.config.ConfigData
import dev.velocity.oneui.core.HookContext
import dev.velocity.oneui.diagnostics.DiagLog
import dev.velocity.oneui.diagnostics.StatusReporter
import dev.velocity.oneui.hooks.Hook
import dev.velocity.oneui.hooks.HookStatus
import dev.velocity.oneui.hooks.UnsupportedHookException

/**
 * Per-process lifecycle owner of installed hooks. Applying a new config is
 * a diff: hooks that became enabled get installed, hooks that became
 * disabled get unhooked; everything else is left untouched. This is what
 * makes profile switching live (no reboot).
 */
object ProfileManager {
    private val applied = HashMap<String, List<XposedBridge.Unhook>>()

    @Synchronized
    fun apply(context: HookContext, config: ConfigData, hooks: List<Hook>) {
        for (hook in hooks) {
            val enabled = config.isEnabled(hook.id)
            val active = applied[hook.id]
            if (enabled && active == null) {
                install(context, hook)
            } else if (!enabled && active != null) {
                uninstall(hook.id)
            }
        }
    }

    private fun install(context: HookContext, hook: Hook) {
        try {
            if (!hook.isAvailable(context)) {
                StatusReporter.reportStatus(
                    context.process, hook.id, HookStatus.UNSUPPORTED.wireName,
                    "isAvailable() reported false on this firmware",
                )
                DiagLog.w("${hook.id}: UNSUPPORTED (availability check)")
                return
            }
            val unhooks = hook.install(context)
            applied[hook.id] = unhooks
            StatusReporter.reportStatus(context.process, hook.id, HookStatus.ACTIVE.wireName, null)
            DiagLog.i("${hook.id}: ACTIVE (${unhooks.size} interceptors)")
        } catch (u: UnsupportedHookException) {
            StatusReporter.reportStatus(context.process, hook.id, HookStatus.UNSUPPORTED.wireName, u.message)
            DiagLog.w("${hook.id}: UNSUPPORTED - ${u.message}")
        } catch (t: Throwable) {
            applied.remove(hook.id)
            StatusReporter.reportStatus(context.process, hook.id, HookStatus.FAILED.wireName, t.toString())
            DiagLog.e("${hook.id}: FAILED, left uninstalled", t)
        }
    }

    private fun uninstall(hookId: String) {
        val active = applied.remove(hookId)
        active?.forEach { unhook ->
            runCatching { unhook.unhook() }
        }
        DiagLog.i("$hookId: DISABLED (live unhook)")
    }
}
