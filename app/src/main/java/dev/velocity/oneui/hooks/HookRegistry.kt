package dev.velocity.oneui.hooks

import android.app.AndroidAppHelper
import android.content.Context
import dev.velocity.oneui.config.ConfigClient
import dev.velocity.oneui.config.ConfigData
import dev.velocity.oneui.core.HookContext
import dev.velocity.oneui.core.TargetIo
import dev.velocity.oneui.diagnostics.DiagLog
import dev.velocity.oneui.diagnostics.StatusReporter
import dev.velocity.oneui.diagnostics.BenchController
import dev.velocity.oneui.hooks.impl.NoOpExplicitGcHook
import dev.velocity.oneui.profiles.ProfileManager
import dev.velocity.oneui.systemui.SystemUILogSuppressionHook

/**
 * Registry and bootstrapper for one target process.
 *
 * Bootstrap sequence (all on the io thread):
 * 1. Wait for the target's Application object (bounded retries, 60 s).
 * 2. Read configuration through the provider (defaults to Balanced if the
 *    provider is unreachable, e.g. the module app is force-stopped).
 * 3. Apply the diffed hook set; report per-hook status.
 * 4. Register the config observer for live profile switching.
 *
 * Every step is individually guarded; total failure leaves the module inert.
 */
object HookRegistry {

    private val allHooks: List<Hook> = listOf(
        NoOpExplicitGcHook(),
        SystemUILogSuppressionHook(),
    )

    fun hooksFor(process: dev.velocity.oneui.core.Process): List<Hook> =
        allHooks.filter { process in it.processes }

    fun bootstrap(context: HookContext) {
        var attempts = 0
        val bootstrapRunnable = object : Runnable {
            override fun run() {
                val app: Context? = try {
                    AndroidAppHelper.currentApplication()
                } catch (t: Throwable) {
                    null
                }
                if (app == null) {
                    if (attempts++ < 120) {
                        TargetIo.postDelayed(this, 500)
                    } else {
                        DiagLog.e("Application never became available; module inert in ${context.process.display}")
                    }
                    return
                }
                try {
                    val client = ConfigClient(context.process, app)
                    StatusReporter.attach(client)
                    val config = client.read() ?: ConfigData.default()
                    applyConfig(context, config)
                    StatusReporter.heartbeat(context.process)
                    client.observe { newConfig -> applyConfig(context, newConfig) }
                    DiagLog.i("Bootstrap complete in ${context.process.display} (profile=${config.profile.key})")
                } catch (t: Throwable) {
                    DiagLog.e("Bootstrap failed; module inert in ${context.process.display}", t)
                }
            }
        }
        TargetIo.post(bootstrapRunnable)
    }

    private fun applyConfig(context: HookContext, config: ConfigData) {
        try {
            ProfileManager.apply(context, config, hooksFor(context.process))
            BenchController.maybeRun(context, config)
        } catch (t: Throwable) {
            DiagLog.e("Applying configuration failed", t)
        }
    }
}
