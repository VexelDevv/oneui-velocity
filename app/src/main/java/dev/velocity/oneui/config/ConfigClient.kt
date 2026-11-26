package dev.velocity.oneui.config

import android.content.Context
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.core.TargetIo
import dev.velocity.oneui.diagnostics.DiagLog
import org.json.JSONObject

/**
 * Client used inside hooked target processes. All provider traffic runs on
 * the velocity-io handler thread (see TargetIo) so no Binder call ever lands
 * on the SystemUI main thread.
 */
class ConfigClient(
    private val process: Process,
    private val context: Context,
) {
    private val resolver = context.contentResolver

    fun read(): ConfigData? = try {
        val result = resolver.call(
            ProviderContract.CONFIG_URI,
            ProviderContract.METHOD_GET,
            null,
            null,
        )
        val json = result?.getString(ProviderContract.KEY_CONFIG)
        ConfigData.fromJson(json)
    } catch (t: Throwable) {
        DiagLog.w("config read failed: $t")
        null
    }

    /** Live re-apply: observe config changes; callback runs on the io thread. */
    fun observe(onConfig: (ConfigData) -> Unit) {
        runCatching {
            resolver.registerContentObserver(
                ProviderContract.CONFIG_URI,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        TargetIo.post {
                            read()?.let { cfg -> onConfig(cfg) }
                        }
                    }
                },
            )
        }.onFailure { DiagLog.w("observer registration failed: $it") }
    }

    fun reportStatus(hookId: String, status: String, detail: String?, at: Long): Boolean =
        report(
            ProviderContract.METHOD_REPORT_STATUS,
            JSONObject()
                .put("hookId", hookId)
                .put("status", status)
                .put("detail", detail ?: "")
                .put("at", at)
                .toString(),
        )

    fun heartbeat(version: String): Boolean =
        report(
            ProviderContract.METHOD_HEARTBEAT,
            JSONObject().put("version", version).toString(),
        )

    fun reportBench(json: String): Boolean =
        report(ProviderContract.METHOD_REPORT_BENCH, json)

    private fun report(method: String, json: String): Boolean = try {
        val extras = Bundle().apply { putString(ProviderContract.KEY_JSON, json) }
        resolver.call(ProviderContract.CONFIG_URI, method, process.key, extras)
        true
    } catch (t: Throwable) {
        DiagLog.w("provider call $method failed: $t")
        false
    }
}
