package com.foco.launcher.registry

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony

enum class SuggestedKind {
    PHONE,
    SETTINGS,
    MESSAGES,
    CAMERA,
}

data class SuggestedApp(
    val kind: SuggestedKind,
    val packageName: String,
    val label: String,
)

object SuggestedApps {
    fun resolve(context: Context): List<SuggestedApp> {
        val pm = context.packageManager
        val ordered = listOfNotNull(
            phone(context, pm),
            settings(context, pm),
            messages(context, pm),
            camera(context, pm),
        )
        return ordered.distinctBy { it.packageName }
    }

    fun isPhone(context: Context, packageName: String): Boolean {
        return cachedPhone(context) == packageName
    }

    /** Package that resolves [android.provider.Settings.ACTION_SETTINGS], not a hardcoded OEM name. */
    fun settingsPackage(context: Context): String? {
        return cachedSettings(context)
    }

    /** Package install/remove can change the resolved dialer or settings app. */
    fun invalidate() {
        synchronized(cacheLock) { cachedAtMs = 0L }
    }

    fun isSystemSettings(context: Context, packageName: String): Boolean {
        return settingsPackage(context) == packageName
    }

    private fun cachedPhone(context: Context): String? = synchronized(cacheLock) {
        refreshCache(context)
        cachedPhonePkg
    }

    private fun cachedSettings(context: Context): String? = synchronized(cacheLock) {
        refreshCache(context)
        cachedSettingsPkg
    }

    private fun refreshCache(context: Context) {
        val now = SystemClock.elapsedRealtime()
        if (cachedAtMs != 0L && now - cachedAtMs < RESOLVE_TTL_MS) return
        val pm = context.applicationContext.packageManager
        val app = context.applicationContext
        cachedPhonePkg = phone(app, pm)?.packageName
        cachedSettingsPkg = settings(app, pm)?.packageName
        cachedAtMs = now
    }

    private val cacheLock = Any()

    private const val RESOLVE_TTL_MS = 60_000L
    private var cachedAtMs = 0L
    private var cachedPhonePkg: String? = null
    private var cachedSettingsPkg: String? = null

    private fun phone(context: Context, pm: PackageManager): SuggestedApp? {
        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:") }
        return fromIntent(pm, intent, SuggestedKind.PHONE, context.getString(com.foco.launcher.R.string.setup_suggested_phone))
            ?: fromKnown(pm, PHONE_PACKAGES, SuggestedKind.PHONE, context.getString(com.foco.launcher.R.string.setup_suggested_phone))
    }

    private fun settings(context: Context, pm: PackageManager): SuggestedApp? {
        val intent = Intent(Settings.ACTION_SETTINGS)
        return fromIntent(pm, intent, SuggestedKind.SETTINGS, context.getString(com.foco.launcher.R.string.setup_suggested_settings))
            ?: fromKnown(pm, listOf("com.android.settings"), SuggestedKind.SETTINGS, context.getString(com.foco.launcher.R.string.setup_suggested_settings))
    }

    private fun messages(context: Context, pm: PackageManager): SuggestedApp? {
        val sms = Telephony.Sms.getDefaultSmsPackage(context)
        if (!sms.isNullOrBlank()) {
            fromPackage(pm, sms, SuggestedKind.MESSAGES, context.getString(com.foco.launcher.R.string.setup_suggested_messages))
                ?.let { return it }
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:") }
        return fromIntent(pm, intent, SuggestedKind.MESSAGES, context.getString(com.foco.launcher.R.string.setup_suggested_messages))
            ?: fromKnown(pm, MESSAGE_PACKAGES, SuggestedKind.MESSAGES, context.getString(com.foco.launcher.R.string.setup_suggested_messages))
    }

    private fun camera(context: Context, pm: PackageManager): SuggestedApp? {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return fromIntent(pm, intent, SuggestedKind.CAMERA, context.getString(com.foco.launcher.R.string.setup_suggested_camera))
            ?: fromKnown(pm, CAMERA_PACKAGES, SuggestedKind.CAMERA, context.getString(com.foco.launcher.R.string.setup_suggested_camera))
    }

    private fun fromIntent(
        pm: PackageManager,
        intent: Intent,
        kind: SuggestedKind,
        displayLabel: String,
    ): SuggestedApp? {
        val resolve = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        val pkg = resolve.activityInfo?.packageName ?: return null
        if (pkg == "android") return null
        return fromPackage(pm, pkg, kind, displayLabel)
    }

    private fun fromKnown(
        pm: PackageManager,
        packages: List<String>,
        kind: SuggestedKind,
        displayLabel: String,
    ): SuggestedApp? {
        for (pkg in packages) {
            fromPackage(pm, pkg, kind, displayLabel)?.let { return it }
        }
        return null
    }

    private fun fromPackage(
        pm: PackageManager,
        packageName: String,
        kind: SuggestedKind,
        displayLabel: String,
    ): SuggestedApp? {
        val launch = pm.getLaunchIntentForPackage(packageName) ?: return null
        val pkg = launch.component?.packageName ?: packageName
        val label = runCatching {
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(displayLabel)
        return SuggestedApp(kind, pkg, label)
    }

    private val PHONE_PACKAGES = listOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.motorola.dialer",
    )

    private val MESSAGE_PACKAGES = listOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.motorola.messaging",
    )

    private val CAMERA_PACKAGES = listOf(
        "com.google.android.GoogleCamera",
        "com.android.camera2",
        "com.android.camera",
        "com.motorola.camera3",
        "com.motorola.camera2",
        "com.sec.android.app.camera",
    )
}
