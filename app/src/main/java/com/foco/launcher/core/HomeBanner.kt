package com.foco.launcher.core

/**
 * At most one home banner. Not-default outranks a missing notification-listener grant.
 */
enum class HomeBanner {
    None,
    NotDefault,
    Nls,
}

fun selectHomeBanner(
    setupDone: Boolean,
    isDefaultHome: Boolean,
    nlsNeedsGrant: Boolean,
): HomeBanner {
    return when {
        setupDone && !isDefaultHome -> HomeBanner.NotDefault
        nlsNeedsGrant -> HomeBanner.Nls
        else -> HomeBanner.None
    }
}
