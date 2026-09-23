package com.foco.launcher.notification

import android.content.Intent

/**
 * Order for opening notification-listener access.
 * Detail (API 30+) lands on Foco's row. The list is the documented fallback.
 * Generic settings is last, only when both listener screens fail to start.
 */
object NlsSettingsPlan {
    const val ACTION_DETAIL = "android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS"
    const val ACTION_LIST = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
    const val ACTION_GENERIC = "android.settings.SETTINGS"
    const val EXTRA_COMPONENT = "android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME"
    const val SDK_DETAIL = 30

    enum class Kind { Detail, List, Generic }

    data class Candidate(
        val kind: Kind,
        val action: String,
        val componentFlat: String? = null,
    ) {
        fun toIntent(): Intent {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (kind == Kind.Detail && !componentFlat.isNullOrBlank()) {
                intent.putExtra(EXTRA_COMPONENT, componentFlat)
            }
            return intent
        }
    }

    fun candidates(sdkInt: Int, componentFlat: String): List<Candidate> {
        val list = Candidate(Kind.List, ACTION_LIST)
        val generic = Candidate(Kind.Generic, ACTION_GENERIC)
        return if (sdkInt >= SDK_DETAIL) {
            listOf(Candidate(Kind.Detail, ACTION_DETAIL, componentFlat), list, generic)
        } else {
            listOf(list, generic)
        }
    }
}
