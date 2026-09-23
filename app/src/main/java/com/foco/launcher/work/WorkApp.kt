package com.foco.launcher.work

import android.content.ComponentName
import android.graphics.Bitmap
import android.os.UserHandle

data class WorkApp(
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Bitmap,
    val user: UserHandle,
    val component: ComponentName,
)
