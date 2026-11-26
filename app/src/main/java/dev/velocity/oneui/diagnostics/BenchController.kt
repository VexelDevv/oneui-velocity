package dev.velocity.oneui.diagnostics

import dev.velocity.oneui.config.ConfigData
import dev.velocity.oneui.core.Constants
import dev.velocity.oneui.core.HookContext
import dev.velocity.oneui.core.TargetIo
import dev.velocity.oneui.rendering.FramePacingMonitor
import org.json.JSONObject

/**
 * Consumes on-demand diagnostic sessions issued by the UI. Exactly one
 * process consumes a given request; each process remembers the last id it
 * consumed so observer re-delivery never double-runs a session.
 */
object BenchController {
    private var lastConsumedId = 0

    fun maybeRun(context: HookContext, config: ConfigData) {
        val request = config.diagnosticsRequest ?: return
        if (request.id <= lastConsumedId) return
        if (request.process != context.process.key) return
        lastConsumedId = request.id

        DiagLog.i("Diagnostics session #${request.id} starting in ${context.process.display}")
        TargetIo.postToMain {
            try {
                FramePacingMonitor.runSample(Constants.DIAGNOSTIC_DURATION_MS) { stats ->
                    TargetIo.post {
                        try {
                            val payload = JSONObject()
                                .put("process", context.process.key)
                                .put("processName", context.process.display)
                                .put("requestId", request.id)
                                .put("startedAt", System.currentTimeMillis())
                                .put("durationMs", stats.durationMs)
                                .put("frameCount", stats.frameCount)
                                .put("medianMs", stats.medianMs)
                                .put("p90Ms", stats.p90Ms)
                                .put("p99Ms", stats.p99Ms)
                                .put("maxMs", stats.maxMs)
                                .put("missedCount", stats.missedCount)
                                .put("missedPercent", stats.missedPercent)
                            StatusReporter.enrichWithLogTail(payload, "logTail", 40)
                            StatusReporter.reportBench(context.process, payload.toString())
                            DiagLog.i("Diagnostics session #${request.id} finished")
                        } catch (t: Throwable) {
                            DiagLog.e("Failed to report diagnostics session", t)
                        }
                    }
                }
            } catch (t: Throwable) {
                DiagLog.e("Diagnostics session failed to start", t)
            }
        }
    }
}
