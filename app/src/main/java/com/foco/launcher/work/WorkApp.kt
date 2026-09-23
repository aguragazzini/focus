package com.foco.launcher.work

import android.content.ComponentName
import android.os.UserHandle

/**
 * One work-profile launchable. Icons are loaded separately so listing the catalog
 * does not decode every bitmap.
 */
data class WorkApp(
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    val user: UserHandle,
    val component: ComponentName,
)
