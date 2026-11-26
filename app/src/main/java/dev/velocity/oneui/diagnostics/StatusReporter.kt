package dev.velocity.oneui.diagnostics

import dev.velocity.oneui.config.ConfigClient
import dev.velocity.oneui.core.BuildConstants
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.core.TargetIo
import org.json.JSONObject

/**
 * Bridge for pushing per-process hook status / heartbeat / benchmark
 * reports back to the configuration app. All calls are posted to the io
 * thread; failures are swallowed and logged (the UI falls back to
 * "not reported yet" rather than receiving partial data).
 */
object StatusReporter {
    private var client: ConfigClient? = null

    fun attach(configClient: ConfigClient) {
        client = configClient
    }

    fun heartbeat(process: Process) {
        val c = client ?: return
        TargetIo.post {
            c.heartbeat(BuildConstants.VERSION)
        }
    }

    fun reportStatus(process: Process, hookId: String, status: String, detail: String?) {
        val c = client ?: return
        TargetIo.post {
            c.reportStatus(hookId, status, detail, System.currentTimeMillis())
        }
    }

    fun reportBench(process: Process, json: String) {
        val c = client ?: return
        TargetIo.post {
            c.reportBench(json)
        }
    }

    /** Attach a log tail to a benchmark payload for the diagnostics screen. */
    fun enrichWithLogTail(json: JSONObject, key: String, maxLines: Int): JSONObject {
        val tail = org.json.JSONArray()
        DiagLog.snapshot(maxLines).forEach { tail.put(it) }
        json.put(key, tail)
        return json
    }
}
