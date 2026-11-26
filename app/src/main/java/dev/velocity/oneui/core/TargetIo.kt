package dev.velocity.oneui.core

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

/**
 * Single background thread used inside target processes for all
 * provider traffic (config reads, status and benchmark reports).
 *
 * Rationale: ContentResolver.call() is a synchronous Binder call. Running it
 * on the SystemUI main thread would add latency to the exact thread we are
 * trying to protect. One handler thread, started lazily on first use, no
 * wakeups when idle, no polling.
 */
object TargetIo {
    private val thread by lazy {
        HandlerThread("velocity-io").apply { start() }
    }
    private val ioHandler by lazy { Handler(thread.looper) }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun post(runnable: Runnable) = ioHandler.post(runnable)

    fun postDelayed(runnable: Runnable, delayMs: Long) =
        ioHandler.postDelayed(runnable, delayMs)

    fun postToMain(runnable: Runnable) = mainHandler.post(runnable)
}
