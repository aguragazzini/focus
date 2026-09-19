package com.foco.launcher.notification

import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.service.notification.StatusBarNotification

/**
 * Work / managed profile: NLS must PASS always.
 * Package name alone is insufficient (dual apps share it).
 */
object UserProfileHelper {
    fun isWorkOrOtherProfile(sbn: StatusBarNotification): Boolean {
        return isWorkOrOtherProfile(sbn.user)
    }

    fun isWorkOrOtherProfile(notifUser: UserHandle?): Boolean {
        if (notifUser == null) return false
        return notifUser != Process.myUserHandle()
    }

    /**
     * UI only: show the work-profile honesty card if this device has another profile.
     * Extra UserHandles are treated as work-like; we never expose toggles for them.
     */
    fun hasWorkProfile(context: Context): Boolean {
        val um = context.getSystemService(UserManager::class.java) ?: return false
        val my = Process.myUserHandle()
        val profiles = um.userProfiles
        if (profiles.size <= 1) return false
        return profiles.any { it != my }
    }
}
