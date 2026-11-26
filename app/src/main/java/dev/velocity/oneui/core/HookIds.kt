package dev.velocity.oneui.core

/**
 * Stable identifiers for every optimization hook. Pure Kotlin (no Xposed
 * imports) so both the UI catalog and the hook implementations share them
 * without dragging Xposed classes into the app process.
 */
object HookIds {
    /** No-op explicit System.gc()/Runtime.gc() in target processes. */
    const val NO_OP_EXPLICIT_GC = "no_op_explicit_gc"

    /** Drop below-WARNING SystemUI LogBuffer enqueue work. */
    const val SYSTEM_UI_LOG_SUPPRESSION = "system_ui_log_suppression"
}
