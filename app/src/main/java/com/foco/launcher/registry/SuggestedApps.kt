package com.foco.launcher.registry

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
        return phone(context, context.packageManager)?.packageName == packageName
    }

    fun isSystemSettings(context: Context, packageName: String): Boolean {
        return settings(context, context.packageManager)?.packageName == packageName
    }

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
