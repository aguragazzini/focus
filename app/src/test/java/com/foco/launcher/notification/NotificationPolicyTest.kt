package com.foco.launcher.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {
    private val personal = NotificationPolicy.Facts(
        packageName = "com.instagram.android",
        isWorkOrOtherProfile = false,
        category = null,
        isMediaStyle = false,
        hasMediaSession = false,
        isDialerPackage = false,
    )

    @Test
    fun workProfileNeverSuppresses() {
        val work = personal.copy(isWorkOrOtherProfile = true)
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = work,
                nlsFilterEnabled = true,
                allowlist = emptySet(),
            ),
        )
    }

    @Test
    fun personalHiddenPackageIsSuppressed() {
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = true,
                allowlist = emptySet(),
            ),
        )
    }

    @Test
    fun personalAllowlistedPasses() {
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = true,
                allowlist = setOf("com.instagram.android"),
            ),
        )
    }

    @Test
    fun filterOffIsNoOp() {
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = false,
                allowlist = emptySet(),
            ),
        )
    }

    @Test
    fun callAndAlarmAndMediaNeverCancel() {
        val call = personal.copy(category = NotificationPolicy.CATEGORY_CALL)
        val alarm = personal.copy(category = NotificationPolicy.CATEGORY_ALARM)
        val nav = personal.copy(category = NotificationPolicy.CATEGORY_NAVIGATION)
        val media = personal.copy(isMediaStyle = true)
        val transport = personal.copy(category = NotificationPolicy.CATEGORY_TRANSPORT)
        val dialer = personal.copy(packageName = "com.google.android.dialer", isDialerPackage = true)
        val allowlist = emptySet<String>()
        assertFalse(NotificationPolicy.shouldSuppress(call, true, allowlist))
        assertFalse(NotificationPolicy.shouldSuppress(alarm, true, allowlist))
        assertFalse(NotificationPolicy.shouldSuppress(nav, true, allowlist))
        assertFalse(NotificationPolicy.shouldSuppress(media, true, allowlist))
        assertFalse(NotificationPolicy.shouldSuppress(transport, true, allowlist))
        assertFalse(NotificationPolicy.shouldSuppress(dialer, true, allowlist))
    }

    @Test
    fun systemOemNeverCancel() {
        val sys = personal.copy(packageName = "com.android.systemui")
        val moto = personal.copy(packageName = "com.motorola.android.settings")
        assertFalse(NotificationPolicy.shouldSuppress(sys, true, emptySet()))
        assertFalse(NotificationPolicy.shouldSuppress(moto, true, emptySet()))
        assertTrue(NotificationPolicy.isProtectedSystemOrOem("android"))
        assertTrue(NotificationPolicy.isProtectedSystemOrOem("com.android.settings"))
        assertFalse(NotificationPolicy.isProtectedSystemOrOem("com.motorola.camera3"))
    }
}
