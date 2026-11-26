package dev.velocity.oneui.config

import android.content.Context
import android.util.Log
import dev.velocity.oneui.core.BuildConstants
import dev.velocity.oneui.profiles.Profile
import org.json.JSONObject
import java.io.File

/**
 * File-backed store inside the module's own app data directory.
 * The UI writes through this class directly (same process); target
 * processes write status/benchmark reports through ConfigProvider,
 * which delegates here. Every mutation notifies the CONFIG_URI so
 * observers inside SystemUI / Samsung Home re-apply configuration live.
 */
class ConfigStore(private val context: Context) {

    private val configFile: File
    val statusesDir: File
    val benchDir: File

    init {
        val files = context.filesDir
        configFile = File(files, "config.json")
        statusesDir = File(files, "statuses").apply { mkdirs() }
        benchDir = File(files, "bench").apply { mkdirs() }
    }

    fun load(): ConfigData =
        runCatching { ConfigData.fromJson(configFile.readText()) }.getOrNull()
            ?: ConfigData.default()

    fun save(config: ConfigData) {
        runCatching {
            configFile.writeText(config.toJson())
        }.onFailure { Log.w(BuildConstants.TAG, "config save failed: ${it}") }
        notifyChange()
    }

    fun setProfile(profile: Profile) {
        save(load().copy(profile = profile))
    }

    /** Set or clear a per-hook override; null removes the override. */
    fun setOverride(hookId: String, enabled: Boolean?) {
        val current = load()
        val next = current.overrides.toMutableMap()
        if (enabled == null) next.remove(hookId) else next[hookId] = enabled
        save(current.copy(overrides = next))
    }

    /** Issue a diagnostic session for one target process; ids are monotonic. */
    fun requestDiagnostics(processKey: String): DiagnosticsRequest {
        val current = load()
        val id = (current.diagnosticsRequest?.id ?: 0) + 1
        val req = DiagnosticsRequest(id = id, process = processKey, requestedAt = System.currentTimeMillis())
        save(current.copy(diagnosticsRequest = req))
        return req
    }

    /** Merge one hook-status report into statuses/<process>.json. */
    fun recordStatus(processKey: String, json: String) {
        runCatching {
            val incoming = JSONObject(json)
            val file = File(statusesDir, "$processKey.json")
            val root = if (file.exists()) parseOrNew(file) else JSONObject()
            root.put("process", processKey)
            root.put("lastSeen", System.currentTimeMillis())
            root.put("version", incoming.optString("version"))
            val hooks = root.optJSONObject("hooks") ?: JSONObject().also { root.put("hooks", it) }
            val entry = JSONObject()
            entry.put("status", incoming.optString("status"))
            entry.put("detail", incoming.optString("detail"))
            entry.put("at", incoming.optLong("at"))
            hooks.put(incoming.optString("hookId"), entry)
            file.writeText(root.toString())
        }.onFailure { Log.w(BuildConstants.TAG, "recordStatus failed: ${it}") }
    }

    /** Record a process heartbeat (module loaded, version). */
    fun recordHeartbeat(processKey: String, json: String) {
        runCatching {
            val incoming = JSONObject(json)
            val file = File(statusesDir, "$processKey.json")
            val root = if (file.exists()) parseOrNew(file) else JSONObject()
            root.put("process", processKey)
            root.put("lastSeen", System.currentTimeMillis())
            root.put("version", incoming.optString("version"))
            file.writeText(root.toString())
        }.onFailure { Log.w(BuildConstants.TAG, "recordHeartbeat failed: ${it}") }
    }

    /** Persist a benchmark session report; keeps the latest five per process. */
    fun recordBench(processKey: String, json: String) {
        runCatching {
            val incoming = JSONObject(json)
            val id = incoming.optInt("requestId", 0)
            File(benchDir, "$processKey-$id.json").writeText(json)
            // Prune old files for this process, keep newest five by id.
            benchDir.listFiles()
                ?.filter { it.name.startsWith("$processKey-") }
                ?.sortedByDescending { nameOfId(it.name) }
                ?.drop(5)
                ?.forEach { it.delete() }
        }.onFailure { Log.w(BuildConstants.TAG, "recordBench failed: ${it}") }
    }

    fun notifyChange() {
        runCatching {
            context.contentResolver.notifyChange(ProviderContract.CONFIG_URI, null)
        }
    }

    private fun parseOrNew(file: File): JSONObject =
        runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())

    private fun nameOfId(name: String): Int =
        name.removeSuffix(".json").substringAfterLast('-').toIntOrNull() ?: 0
}
