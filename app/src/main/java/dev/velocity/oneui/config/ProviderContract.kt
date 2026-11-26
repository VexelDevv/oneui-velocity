package dev.velocity.oneui.config

import android.net.Uri
import dev.velocity.oneui.core.Constants

/** Contract shared between the provider (app side) and clients (target processes). */
object ProviderContract {
    val CONFIG_URI: Uri = Uri.parse("content://${Constants.CONFIG_AUTHORITY}/config")

    const val METHOD_GET = "get"
    const val METHOD_PING = "ping"
    const val METHOD_HEARTBEAT = "heartbeat"
    const val METHOD_REPORT_STATUS = "reportStatus"
    const val METHOD_REPORT_BENCH = "reportBench"

    const val KEY_CONFIG = "config"
    const val KEY_JSON = "json"
}
