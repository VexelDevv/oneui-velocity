package dev.velocity.oneui.core

/**
 * Processes this module operates in. Deliberately limited to SystemUI and
 * Samsung Home: no system_server, no zygote, no arbitrary apps. That keeps
 * the blast radius of any failure to a restartable UI process.
 */
enum class Process(
    val key: String,
    val display: String,
    val packageName: String,
) {
    SYSTEM_UI("systemui", "SystemUI", "com.android.systemui"),
    LAUNCHER("launcher", "Samsung Home", "com.sec.android.app.launcher");

    companion object {
        fun fromPackageName(packageName: String): Process? =
            values().firstOrNull { it.packageName == packageName }

        fun fromKey(key: String): Process? =
            values().firstOrNull { it.key == key }
    }
}
