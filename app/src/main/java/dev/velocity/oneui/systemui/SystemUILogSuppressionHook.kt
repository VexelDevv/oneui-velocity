package dev.velocity.oneui.systemui

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import dev.velocity.oneui.core.HookContext
import dev.velocity.oneui.core.HookIds
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.diagnostics.DiagLog
import dev.velocity.oneui.hooks.Hook
import dev.velocity.oneui.hooks.UnsupportedHookException

/**
 * Suppresses verbose SystemUI log-buffer enqueue work.
 *
 * AOSP SystemUI (12+; One UI 8 is AOSP 16 based) funnels internal logging
 * through com.android.systemui.log.core.LogBuffer rings that are filled on
 * the calling thread, frequently the main thread during notification
 * storms. Dropping below-WARNING messages at enqueue time removes that
 * bookkeeping work while keeping WARNING/ERROR/WTF intact for debugging.
 *
 * Compatibility handling (honest by design):
 * - The class is resolved with findClassIfExists at install time. If this
 *   firmware relocated or replaced the AOSP LogBuffer, the hook reports
 *   UNSUPPORTED and changes nothing.
 * - The log method is located by name and arity rather than by exact
 *   parameter type, so signature detail changes do not break resolution.
 * - Inside the hook, every access is guarded: any failure passes the log
 *   entry through untouched. Suppression can never break SystemUI logging.
 */
class SystemUILogSuppressionHook : Hook(
    id = HookIds.SYSTEM_UI_LOG_SUPPRESSION,
    title = "SystemUI verbose log suppression",
    description = "Drops below-WARNING LogBuffer enqueue work in SystemUI.",
    processes = setOf(Process.SYSTEM_UI),
) {
    private val keepLevels = setOf("WARNING", "WARN", "ERROR", "WTF", "FATAL")

    override fun install(context: HookContext): List<XC_MethodHook.Unhook> {
        val bufferClass = XposedHelpers.findClassIfExists(
            "com.android.systemui.log.core.LogBuffer",
            context.classLoader,
        ) ?: throw UnsupportedHookException(
            "com.android.systemui.log.core.LogBuffer not present on this firmware",
        )

        val logMethod = bufferClass.methods.firstOrNull { method ->
            method.name == "log" && method.parameterTypes.size == 1
        } ?: throw UnsupportedHookException(
            "LogBuffer.log(single-arg) not found on this firmware",
        )

        DiagLog.i("LogBuffer resolved: ${bufferClass.name}#${logMethod.name}(${logMethod.parameterTypes.joinToString()})")

        return listOf(
            XposedBridge.hookMethod(logMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val message = param.args[0] ?: return
                        val level = XposedHelpers.callMethod(message, "getLevel") ?: return
                        if (level.toString() !in keepLevels) {
                            param.setResult(null)
                        }
                    } catch (t: Throwable) {
                        // Never break SystemUI logging on a lookup miss.
                    }
                }
            }),
        )
    }
}
