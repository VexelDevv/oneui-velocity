package dev.velocity.oneui.core

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.velocity.oneui.diagnostics.DiagLog
import dev.velocity.oneui.hooks.HookRegistry

/**
 * LSPosed entry point (legacy Xposed API).
 *
 * Scope discipline:
 * - Only the main process of com.android.systemui and
 *   com.sec.android.app.launcher is ever touched (no :remote-style child
 *   processes, no system_server, no zygote).
 * - Anything unexpected during init leaves the module inert; the entry
 *   point cannot throw into the host process.
 */
class VelocityEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Main process only; secondary processes inherit no hooks.
            if (lpparam.processName != lpparam.packageName) return
            val process = Process.fromPackageName(lpparam.packageName) ?: return
            DiagLog.i("Velocity ${BuildConstants.VERSION} loading in ${process.display}")
            HookRegistry.bootstrap(HookContext(process, lpparam.classLoader))
        } catch (t: Throwable) {
            DiagLog.e("Fatal during module init; module inert for this process", t)
        }
    }
}
