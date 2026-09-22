package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBannerTest {
    @Test
    fun notDefaultOutranksMissingNlsGrant() {
        assertEquals(
            HomeBanner.NotDefault,
            selectHomeBanner(setupDone = true, isDefaultHome = false, nlsNeedsGrant = true),
        )
    }

    @Test
    fun nlsBannerOnlyWhenAlreadyDefault() {
        assertEquals(
            HomeBanner.Nls,
            selectHomeBanner(setupDone = true, isDefaultHome = true, nlsNeedsGrant = true),
        )
    }

    @Test
    fun noBannerWhenDefaultAndFilterDoesNotNeedGrant() {
        assertEquals(
            HomeBanner.None,
            selectHomeBanner(setupDone = true, isDefaultHome = true, nlsNeedsGrant = false),
        )
    }

    @Test
    fun setupNotFinishedDoesNotShowNotDefaultBanner() {
        assertEquals(
            HomeBanner.None,
            selectHomeBanner(setupDone = false, isDefaultHome = false, nlsNeedsGrant = false),
        )
    }
}
