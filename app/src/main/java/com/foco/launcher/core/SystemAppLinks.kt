package com.foco.launcher.core

/**
 * System clock and calendar entry points. Standard actions first.
 * Package names are launcher fallbacks only — never a weather or network call.
 */
object SystemAppLinks {
    const val ACTION_SHOW_ALARMS = "android.intent.action.SHOW_ALARMS"
    const val ACTION_MAIN = "android.intent.action.MAIN"
    const val CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR"

    data class Candidate(
        val action: String,
        val categories: List<String> = emptyList(),
        val launcherPackage: String? = null,
    )

    fun clockCandidates(): List<Candidate> = listOf(
        Candidate(action = ACTION_SHOW_ALARMS),
        Candidate(action = ACTION_MAIN, launcherPackage = "com.google.android.deskclock"),
        Candidate(action = ACTION_MAIN, launcherPackage = "com.android.deskclock"),
    )

    fun calendarCandidates(): List<Candidate> = listOf(
        Candidate(action = ACTION_MAIN, categories = listOf(CATEGORY_APP_CALENDAR)),
        Candidate(action = ACTION_MAIN, launcherPackage = "com.google.android.calendar"),
        Candidate(action = ACTION_MAIN, launcherPackage = "com.android.calendar"),
    )
}
