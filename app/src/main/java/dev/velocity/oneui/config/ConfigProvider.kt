package dev.velocity.oneui.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import dev.velocity.oneui.core.BuildConstants

/**
 * Exported configuration bridge.
 *
 * Access policy: same-uid calls (the module's own UI) are allowed;
 * external callers must resolve to one of the target packages. UID-to-package
 * resolution goes through PackageManager against the Binder identity, which
 * cannot be spoofed by an ordinary app. Configuration is not secret, but the
 * provider still restricts access to the module and its intended targets.
 *
 * The provider performs no long-running work, owns no services, and is only
 * instantiated on demand.
 */
class ConfigProvider : ContentProvider() {

    private lateinit var store: ConfigStore

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        store = ConfigStore(ctx)
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!isAllowed(ctx)) return null
        return try {
            when (method) {
                ProviderContract.METHOD_GET -> Bundle().apply {
                    putString(ProviderContract.KEY_CONFIG, store.load().toJson())
                }

                ProviderContract.METHOD_PING -> Bundle().apply {
                    putString("pong", BuildConstants.VERSION)
                }

                ProviderContract.METHOD_HEARTBEAT -> {
                    if (!arg.isNullOrBlank()) {
                        store.recordHeartbeat(arg, extras?.getString(ProviderContract.KEY_JSON) ?: "{}")
                    }
                    null
                }

                ProviderContract.METHOD_REPORT_STATUS -> {
                    if (!arg.isNullOrBlank()) {
                        store.recordStatus(arg, extras?.getString(ProviderContract.KEY_JSON) ?: "{}")
                    }
                    null
                }

                ProviderContract.METHOD_REPORT_BENCH -> {
                    if (!arg.isNullOrBlank()) {
                        store.recordBench(arg, extras?.getString(ProviderContract.KEY_JSON) ?: "{}")
                    }
                    null
                }

                else -> null
            }
        } catch (t: Throwable) {
            android.util.Log.w(BuildConstants.TAG, "provider call failed: $t")
            null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val ctx = context ?: throw IllegalStateException("Provider context is unavailable")
        if (!isAllowed(ctx)) {
            throw SecurityException("Caller is not allowed to query Velocity configuration")
        }
        return MatrixCursor(arrayOf("payload")).apply {
            addRow(arrayOf<Any>(store.load().toJson()))
        }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/config"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun isAllowed(ctx: Context): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid()) return true
        val packages = ctx.packageManager.getPackagesForUid(callingUid) ?: return false
        return packages.any { it in ALLOWED_EXTERNAL_PACKAGES }
    }

    private companion object {
        val ALLOWED_EXTERNAL_PACKAGES = setOf(
            "com.android.systemui",
            "com.sec.android.app.launcher",
        )
    }
}
