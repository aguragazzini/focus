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
                listenerGranted = true,
            ),
        )
    }

    @Test
    fun personalNotInAllowlistIsSuppressed() {
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                listenerGranted = true,
            ),
        )
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = true,
                allowlist = setOf("com.whatsapp"),
                listenerGranted = true,
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
                listenerGranted = true,
            ),
        )
    }

    @Test
    fun emptyAllowlistAndFilterOnCancelsPersonalExceptPreserve() {
        val allowlist = emptySet<String>()
        assertTrue(NotificationPolicy.shouldSuppress(personal, true, allowlist, true))
        assertFalse(
            NotificationPolicy.shouldSuppress(
                personal.copy(category = NotificationPolicy.CATEGORY_NAVIGATION),
                true,
                allowlist,
                true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                personal.copy(isWorkOrOtherProfile = true),
                true,
                allowlist,
                true,
            ),
        )
    }

    @Test
    fun filterOffOrListenerDeniedIsNoOp() {
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                listenerGranted = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                listenerGranted = false,
            ),
        )
    }

    @Test
    fun callAndAlarmAndNavigationAndMediaNeverCancel() {
        val call = personal.copy(category = NotificationPolicy.CATEGORY_CALL)
        val alarm = personal.copy(category = NotificationPolicy.CATEGORY_ALARM)
        val nav = personal.copy(category = NotificationPolicy.CATEGORY_NAVIGATION)
        val media = personal.copy(isMediaStyle = true)
        val mediaSession = personal.copy(hasMediaSession = true)
        val transport = personal.copy(category = NotificationPolicy.CATEGORY_TRANSPORT)
        val dialer = personal.copy(packageName = "com.google.android.dialer", isDialerPackage = true)
        val allowlist = emptySet<String>()
        assertFalse(NotificationPolicy.shouldSuppress(call, true, allowlist, true))
        assertFalse(NotificationPolicy.shouldSuppress(alarm, true, allowlist, true))
        assertFalse(NotificationPolicy.shouldSuppress(nav, true, allowlist, true))
        assertFalse(NotificationPolicy.shouldSuppress(media, true, allowlist, true))
        assertFalse(NotificationPolicy.shouldSuppress(mediaSession, true, allowlist, true))
        assertFalse(NotificationPolicy.shouldSuppress(transport, true, allowlist, true))
        assertFalse(NotificationPolicy.shouldSuppress(dialer, true, allowlist, true))
    }

    @Test
    fun systemOemNeverCancel() {
        val sys = personal.copy(packageName = "com.android.systemui")
        val moto = personal.copy(packageName = "com.motorola.android.settings")
        assertFalse(NotificationPolicy.shouldSuppress(sys, true, emptySet(), true))
        assertFalse(NotificationPolicy.shouldSuppress(moto, true, emptySet(), true))
        assertTrue(NotificationPolicy.isProtectedSystemOrOem("android"))
        assertTrue(NotificationPolicy.isProtectedSystemOrOem("com.android.settings"))
        assertFalse(NotificationPolicy.isProtectedSystemOrOem("com.motorola.camera3"))
    }
}
