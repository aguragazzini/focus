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
    fun workPauseCancelsWorkButSparesCallAlarmNavMediaAndSystem() {
        val work = personal.copy(packageName = "com.slack", isWorkOrOtherProfile = true)
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = work,
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                workSectionPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = work.copy(category = NotificationPolicy.CATEGORY_CALL),
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                workSectionPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = work.copy(category = NotificationPolicy.CATEGORY_ALARM),
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                workSectionPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = work.copy(category = NotificationPolicy.CATEGORY_NAVIGATION),
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                workSectionPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = work.copy(isMediaStyle = true),
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                workSectionPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = work.copy(packageName = "com.android.systemui"),
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                workSectionPaused = true,
                notificationsPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                workSectionPaused = true,
            ),
        )
    }

    @Test
    fun notificationsPauseCancelsPersonalAndWorkExceptSpares() {
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = personal.copy(packageName = "com.instagram.android"),
                nlsFilterEnabled = false,
                allowlist = setOf("com.instagram.android"),
                notificationsPaused = true,
            ),
        )
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = personal.copy(isWorkOrOtherProfile = true, packageName = "com.slack"),
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                notificationsPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal.copy(category = NotificationPolicy.CATEGORY_ALARM),
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                notificationsPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal.copy(hasMediaSession = true),
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                notificationsPaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                listenerGranted = false,
                notificationsPaused = true,
                workSectionPaused = true,
            ),
        )
    }

    @Test
    fun pauseFlagsOffKeepsWorkPassAndAllowlistPolicy() {
        val work = personal.copy(isWorkOrOtherProfile = true)
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = work,
                nlsFilterEnabled = true,
                allowlist = emptySet(),
                workSectionPaused = false,
                notificationsPaused = false,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = true,
                allowlist = setOf(personal.packageName),
                notificationsPaused = false,
            ),
        )
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

    @Test
    fun phonePauseCancelsEverythingTheListenerCanSee() {
        val call = personal.copy(category = NotificationPolicy.CATEGORY_CALL)
        val system = personal.copy(packageName = "com.android.systemui")
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = call,
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                phonePaused = true,
            ),
        )
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = system,
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                phonePaused = true,
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = call,
                nlsFilterEnabled = false,
                allowlist = emptySet(),
                phonePaused = true,
                listenerGranted = false,
            ),
        )
    }

    @Test
    fun pausedPackageCancelsThatPackageIncludingMedia() {
        val media = personal.copy(isMediaStyle = true)
        assertTrue(
            NotificationPolicy.shouldSuppress(
                facts = media,
                nlsFilterEnabled = false,
                allowlist = setOf(media.packageName),
                pausedPackages = setOf(media.packageName),
            ),
        )
        assertFalse(
            NotificationPolicy.shouldSuppress(
                facts = personal,
                nlsFilterEnabled = false,
                allowlist = setOf(personal.packageName),
                pausedPackages = setOf("com.other"),
            ),
        )
    }
}
