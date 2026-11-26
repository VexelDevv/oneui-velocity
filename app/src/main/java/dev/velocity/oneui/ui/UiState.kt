package dev.velocity.oneui.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.velocity.oneui.catalog.HookCatalog
import dev.velocity.oneui.config.ConfigStore
import dev.velocity.oneui.core.BuildConstants
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.profiles.Profile
import org.json.JSONObject
import java.io.File

/** Per-process hook status as persisted by the provider (UI side model). */
data class HookStatusEntry(
    val hookId: String,
    val status: String,
    val detail: String,
    val at: Long,
)

data class ProcessStatus(
    val process: Process,
    val lastSeen: Long,
    val version: String,
    val hooks: Map<String, HookStatusEntry>,
)

data class BenchReport(
    val process: String,
    val processName: String,
    val requestId: Int,
    val startedAt: Long,
    val durationMs: Long,
    val frameCount: Int,
    val medianMs: Double,
    val p90Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
    val missedCount: Int,
    val missedPercent: Double,
    val logTail: List<String>,
)

/**
 * UI-side state holder. The UI and the store live in the same process, so
 * every user action is a direct store mutation followed by a refresh; the
 * store notifies the CONFIG_URI and target processes re-apply live.
 */
class UiState(context: Context) {

    private val store: ConfigStore = ConfigStore(context)

    var profile by mutableStateOf(Profile.BALANCED)
        private set
    var overrides by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set
    var statuses by mutableStateOf<List<ProcessStatus>>(emptyList())
        private set
    var latestBench by mutableStateOf<Map<Process, BenchReport>>(emptyMap())
        private set
    var pendingIds by mutableStateOf<Map<Process, Int>>(emptyMap())
        private set

    init {
        refresh()
    }

    fun refresh() {
        val config = store.load()
        profile = config.profile
        overrides = config.overrides
        statuses = readStatuses()
        latestBench = readLatestBenches()
        evictCompletedDiagnostics()
    }

    fun setProfile(newProfile: Profile) {
        store.setProfile(newProfile)
        refresh()
    }

    fun setOverride(hookId: String, enabled: Boolean?) {
        store.setOverride(hookId, enabled)
        refresh()
    }

    fun effectiveEnabled(hookId: String): Boolean =
        overrides[hookId] ?: (hookId in profile.defaultHooks)

    fun hasOverride(hookId: String): Boolean = overrides.containsKey(hookId)

    fun requestDiagnostics(process: Process) {
        val request = store.requestDiagnostics(process.key)
        pendingIds = pendingIds + (process to request.id)
    }

    fun activeHooksFor(process: Process): Int =
        statuses.firstOrNull { it.process == process }?.hooks
            ?.count { it.value.status == "active" } ?: 0

    fun statusFor(process: Process, hookId: String): HookStatusEntry? =
        statuses.firstOrNull { it.process == process }?.hooks?.get(hookId)

    private fun readStatuses(): List<ProcessStatus> =
        Process.values().mapNotNull { process ->
            val file = File(store.statusesDir, "${process.key}.json")
            if (!file.exists()) return@mapNotNull null
            runCatching {
                val root = JSONObject(file.readText())
                val hooks = mutableMapOf<String, HookStatusEntry>()
                val hooksObj = root.optJSONObject("hooks")
                if (hooksObj != null) {
                    val keys = hooksObj.keys()
                    while (keys.hasNext()) {
                        val id = keys.next()
                        val entry = hooksObj.optJSONObject(id) ?: continue
                        hooks[id] = HookStatusEntry(
                            hookId = id,
                            status = entry.optString("status", ""),
                            detail = entry.optString("detail", ""),
                            at = entry.optLong("at", 0L),
                        )
                    }
                }
                ProcessStatus(
                    process = process,
                    lastSeen = root.optLong("lastSeen", 0L),
                    version = root.optString("version", ""),
                    hooks = hooks,
                )
            }.getOrNull()
        }

    private fun readLatestBenches(): Map<Process, BenchReport> {
        val result = mutableMapOf<Process, BenchReport>()
        Process.values().forEach { process ->
            val reports = store.benchDir.listFiles()
                ?.filter { it.name.startsWith("${process.key}-") }
                ?.mapNotNull { file -> parseBench(file) }
                ?: emptyList()
            reports.maxByOrNull { it.requestId }?.let { result[process] = it }
        }
        return result
    }

    private fun parseBench(file: File): BenchReport? = runCatching {
        val root = JSONObject(file.readText())
        val tail = mutableListOf<String>()
        val tailArray = root.optJSONArray("logTail")
        if (tailArray != null) {
            for (i in 0 until tailArray.length()) tail.add(tailArray.optString(i))
        }
        BenchReport(
            process = root.optString("process"),
            processName = root.optString("processName"),
            requestId = root.optInt("requestId"),
            startedAt = root.optLong("startedAt"),
            durationMs = root.optLong("durationMs"),
            frameCount = root.optInt("frameCount"),
            medianMs = root.optDouble("medianMs"),
            p90Ms = root.optDouble("p90Ms"),
            p99Ms = root.optDouble("p99Ms"),
            maxMs = root.optDouble("maxMs"),
            missedCount = root.optInt("missedCount"),
            missedPercent = root.optDouble("missedPercent"),
            logTail = tail,
        )
    }.getOrNull()

    private fun evictCompletedDiagnostics() {
        val stillPending = pendingIds.filterKeys { process ->
            latestBench[process]?.requestId?.let { it < (pendingIds[process] ?: 0) } ?: true
        }
        if (stillPending.size != pendingIds.size) pendingIds = stillPending
    }

    val version: String get() = BuildConstants.VERSION
    val hookCatalog = HookCatalog.hooks
}
