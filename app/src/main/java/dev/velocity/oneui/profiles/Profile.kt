package dev.velocity.oneui.profiles

import dev.velocity.oneui.core.HookIds

/**
 * Performance profiles. Resolution order per hook:
 * explicit user override -> profile default set -> disabled.
 *
 * Balanced intentionally enables nothing: it preserves the original Samsung
 * behaviour exactly, arming only the diagnostics tooling. Profiles apply
 * live (no reboot): toggling a profile installs/unhooks the affected hooks
 * inside running target processes through the config observer.
 */
enum class Profile(
    val key: String,
    val title: String,
    val summary: String,
    val defaultHooks: Set<String>,
) {
    BALANCED(
        key = "balanced",
        title = "Balanced",
        summary = "Original Samsung behaviour preserved. Every optimization stays off unless you enable one individually; only the diagnostics tooling is armed.",
        defaultHooks = emptySet(),
    ),
    PERFORMANCE(
        key = "performance",
        title = "Performance",
        summary = "Adds explicit-GC suppression in SystemUI and Samsung Home so spontaneous full collections stop landing inside frame-critical windows.",
        defaultHooks = setOf(HookIds.NO_OP_EXPLICIT_GC),
    ),
    MAXIMUM(
        key = "maximum",
        title = "Maximum",
        summary = "Adds SystemUI verbose log-buffer suppression on top of Performance. All hooks remain individually reversible and status-reported.",
        defaultHooks = setOf(HookIds.NO_OP_EXPLICIT_GC, HookIds.SYSTEM_UI_LOG_SUPPRESSION),
    );

    companion object {
        fun fromKey(key: String?): Profile =
            values().firstOrNull { it.key == key } ?: BALANCED
    }
}
