package dev.velocity.oneui.core

/**
 * Build-level constants. Keep this class dependency-free (no Xposed imports)
 * so it is safe to load inside the configuration app process as well as inside
 * hooked target processes.
 */
object BuildConstants {
    const val VERSION = "1.0.0"
    const val GITHUB_URL = "https://github.com/VexelDevv/oneui-velocity"
    const val TAG = "Velocity"
}
