package com.foco.launcher.work

/**
 * Home Trabajo rules. Every launchable is shown. Nothing is dropped, hidden, or ranked.
 */
object WorkCatalogRules {
    fun showSection(hasWorkProfile: Boolean): Boolean = hasWorkProfile

    fun <T> presentAll(
        activities: List<T>,
        label: (T) -> String,
        packageName: (T) -> String,
        className: (T) -> String,
    ): List<T> {
        return activities.sortedWith(
            compareBy<T> { label(it).lowercase() }
                .thenBy { packageName(it) }
                .thenBy { className(it) },
        )
    }
}

data class WorkActivityId(
    val packageName: String,
    val className: String,
    val label: String,
)
