package dev.velocity.oneui.diagnostics

import android.os.SystemClock
import android.util.Log
import de.robv.android.xposed.XposedBridge
import dev.velocity.oneui.core.BuildConstants

/**
 * In-memory ring buffer plus LSPosed log mirroring. Safe to use from any
 * process: when the Xposed runtime is absent (the module's own app process)
 * it falls back to standard Android logging.
 */
object DiagLog {
    private const val MAX_LINES = 400
    private val lines = ArrayDeque<String>()

    fun i(message: String) = add("I", message, null)
    fun w(message: String) = add("W", message, null)
    fun e(message: String, t: Throwable? = null) = add("E", message, t)

    private fun add(level: String, message: String, t: Throwable?) {
        val entry = "${SystemClock.elapsedRealtime()} $level $message" +
            (t?.let { " | ${it.stackTraceToString().take(600)}" } ?: "")
        synchronized(lines) {
            lines.addLast(entry)
            while (lines.size > MAX_LINES) lines.removeFirst()
        }
        try {
            XposedBridge.log("[${BuildConstants.TAG}] $message")
            if (t != null) XposedBridge.log(t)
        } catch (ignored: Throwable) {
            // No Xposed runtime in this process; fall back to logcat.
            Log.i(BuildConstants.TAG, message)
            if (t != null) Log.w(BuildConstants.TAG, message, t)
        }
    }

    /** Last n lines, oldest first; used to attach context to bench reports. */
    fun snapshot(n: Int): List<String> = synchronized(lines) {
        if (lines.size <= n) lines.toList() else lines.takeLast(n)
    }
}
