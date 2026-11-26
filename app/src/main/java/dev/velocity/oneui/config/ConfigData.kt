package dev.velocity.oneui.config

import dev.velocity.oneui.core.HookIds
import dev.velocity.oneui.profiles.Profile
import org.json.JSONObject

/**
 * A single on-demand diagnostic session requested by the UI and consumed
 * by exactly one target process.
 */
data class DiagnosticsRequest(
    val id: Int,
    val process: String,
    val requestedAt: Long,
)

/**
 * Effective module configuration. Serialized as JSON through the config
 * provider; parsed identically on both sides of the process boundary.
 *
 * Resolution rule for a hook: per-hook override wins, otherwise the active
 * profile's default set.
 */
data class ConfigData(
    val profile: Profile = Profile.BALANCED,
    val overrides: Map<String, Boolean> = emptyMap(),
    val diagnosticsRequest: DiagnosticsRequest? = null,
) {
    fun isEnabled(hookId: String): Boolean =
        overrides[hookId] ?: (hookId in profile.defaultHooks)

    fun toJson(): String {
        val root = JSONObject()
        root.put("profile", profile.key)
        val ov = JSONObject()
        overrides.forEach { (k, v) -> ov.put(k, v) }
        root.put("overrides", ov)
        diagnosticsRequest?.let { req ->
            val r = JSONObject()
            r.put("id", req.id)
            r.put("process", req.process)
            r.put("requestedAt", req.requestedAt)
            root.put("diagnosticsRequest", r)
        }
        return root.toString()
    }

    companion object {
        fun fromJson(json: String?): ConfigData? {
            if (json.isNullOrBlank()) return null
            return try {
                val root = JSONObject(json)
                val profile = Profile.fromKey(root.optString("profile"))
                val overrides = mutableMapOf<String, Boolean>()
                val ov = root.optJSONObject("overrides")
                if (ov != null) {
                    val keys = ov.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        overrides[k] = ov.optBoolean(k)
                    }
                }
                val req = root.optJSONObject("diagnosticsRequest")?.let { r ->
                    DiagnosticsRequest(
                        id = r.optInt("id"),
                        process = r.optString("process"),
                        requestedAt = r.optLong("requestedAt"),
                    )
                }
                ConfigData(profile = profile, overrides = overrides, diagnosticsRequest = req)
            } catch (t: Throwable) {
                null
            }
        }

        fun default(): ConfigData = ConfigData(profile = Profile.BALANCED)
    }
}
