package dev.velocity.oneui.hooks

import de.robv.android.xposed.XposedBridge
import dev.velocity.oneui.core.HookContext
import dev.velocity.oneui.core.Process

/** Lifecycle state of a hook in a given process, surfaced to the UI. */
enum class HookStatus {
    ACTIVE, DISABLED, UNSUPPORTED, FAILED;

    val wireName: String get() = name.lowercase()
}

/** Thrown when a required class/method does not exist on this firmware. */
class UnsupportedHookException(message: String) : Exception(message)

/**
 * Base class for every optimization hook.
 *
 * Contract:
 * - [install] runs once per process when the hook becomes enabled; Method
 *   objects are resolved once here and cached in the returned Unhook list,
 *   so no reflection or lookup happens on any hot path afterwards.
 * - Throwing [UnsupportedHookException] marks the hook UNSUPPORTED
 *   (firmware divergence) instead of FAILED (unexpected error).
 * - The implementation must be safe to unhook completely at runtime.
 */
abstract class Hook(
    val id: String,
    val title: String,
    val description: String,
    val processes: Set<Process>,
) {
    /** Processes this hook applies to; checked before install. */
    abstract fun install(context: HookContext): List<XposedBridge.Unhook>

    open fun isAvailable(context: HookContext): Boolean = true
}
