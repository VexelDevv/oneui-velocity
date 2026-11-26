package dev.velocity.oneui.catalog

import dev.velocity.oneui.core.Process

/**
 * Static, pure-Kotlin description of every optimization the module ships.
 * Mirrored by the hook implementations (see core/HookIds.kt) and rendered by
 * the configuration UI. No Xposed imports: this class must load safely in
 * the module's own app process where the Xposed runtime is absent.
 */

enum class Verification(val label: String, val explanation: String) {
    VERIFIED_STABLE(
        "Stable API",
        "Implemented against boot-classpath APIs that have been frozen for years; deterministic across vendor builds."
    ),
    REQUIRES_CONFIRMATION(
        "AOSP-origin, verify on device",
        "Implemented against an AOSP-origin class that Samsung normally carries forward; the in-app status will auto-report UNSUPPORTED if this firmware relocated it."
    ),
}

data class HookInfo(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val processes: List<Process>,
    val verification: Verification,
    val defaultProfileBehavior: String,
)

object HookCatalog {

    val hooks: List<HookInfo> = listOf(
        HookInfo(
            id = dev.velocity.oneui.core.HookIds.NO_OP_EXPLICIT_GC,
            title = "Explicit GC suppression",
            summary = "Turns advisory System.gc()/Runtime.gc() calls inside SystemUI and Samsung Home into no-ops, so spontaneous full stop-the-world collections cannot land in the middle of frame-critical work. Never touches allocation, memory limits or ART configuration.",
            category = "Memory",
            processes = listOf(Process.SYSTEM_UI, Process.LAUNCHER),
            verification = Verification.VERIFIED_STABLE,
            defaultProfileBehavior = "Performance and Maximum",
        ),
        HookInfo(
            id = dev.velocity.oneui.core.HookIds.SYSTEM_UI_LOG_SUPPRESSION,
            title = "SystemUI verbose log suppression",
            summary = "Drops below-WARNING enqueue work in the SystemUI LogBuffer ring used for diagnostics. During notification storms this trims main-thread bookkeeping while keeping WARNING/ERROR/WTF events intact. Auto-disables itself if this firmware relocated the AOSP LogBuffer class.",
            category = "SystemUI",
            processes = listOf(Process.SYSTEM_UI),
            verification = Verification.REQUIRES_CONFIRMATION,
            defaultProfileBehavior = "Maximum",
        ),
    )
}
