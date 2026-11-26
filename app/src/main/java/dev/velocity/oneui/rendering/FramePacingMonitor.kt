package dev.velocity.oneui.rendering

import android.os.SystemClock
import android.view.Choreographer

/**
 * On-demand main-thread frame-cadence sampler used by diagnostic sessions.
 *
 * Design constraints honoured here:
 * - Zero cost while inactive: a Choreographer callback only exists for the
 *   duration of an explicit, user-initiated session (default 10 s).
 * - Runs on the main thread because Choreographer is thread-confined there.
 * - No polling, no wakeups, no permanent services.
 */
object FramePacingMonitor {

    fun runSample(durationMs: Long, completion: (FrameStats) -> Unit) {
        val timestamps = ArrayList<Long>(4096)
        val choreographer = Choreographer.getInstance()
        val deadlineAt = SystemClock.uptimeMillis() + durationMs

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                timestamps.add(frameTimeNanos)
                if (SystemClock.uptimeMillis() < deadlineAt) {
                    choreographer.postFrameCallback(this)
                } else {
                    completion(FrameStats.from(timestamps, durationMs))
                }
            }
        }
        choreographer.postFrameCallback(callback)
    }
}
