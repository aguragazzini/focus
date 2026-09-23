package com.foco.launcher.work

/**
 * Home Trabajo rules. Every launchable is shown. Nothing is dropped, hidden, or ranked.
 * Search filters the grid only; the catalog itself stays complete (A1, empty query).
 */
object WorkCatalogRules {
    const val SEARCH_MIN = 24

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

    /**
     * S0 Hidden, S1 Active, S2 Empty, S3 Quiet, S4 Error.
     * Quiet is only passed as true when [android.os.UserManager.isQuietModeEnabled] said so.
     * A null quiet reading must not become S3.
     */
    fun sectionKind(
        hasWorkProfile: Boolean,
        loadFailed: Boolean,
        quietEnabled: Boolean?,
        activityCount: Int,
    ): WorkSectionKind {
        return when {
            loadFailed && activityCount == 0 -> WorkSectionKind.Error
            !hasWorkProfile -> WorkSectionKind.Hidden
            quietEnabled == true -> WorkSectionKind.Quiet
            activityCount == 0 -> WorkSectionKind.Empty
            else -> WorkSectionKind.Active
        }
    }

    fun showWorkGrid(kind: WorkSectionKind, visibleCount: Int): Boolean {
        return visibleCount > 0 && (kind == WorkSectionKind.Active || kind == WorkSectionKind.Quiet)
    }

    fun showWorkEmptyCopy(kind: WorkSectionKind): Boolean = kind == WorkSectionKind.Empty

    fun showQuietCopy(kind: WorkSectionKind): Boolean = kind == WorkSectionKind.Quiet

    fun showErrorCopy(kind: WorkSectionKind): Boolean = kind == WorkSectionKind.Error

    fun showWorkCount(kind: WorkSectionKind): Boolean {
        return kind == WorkSectionKind.Active || kind == WorkSectionKind.Empty || kind == WorkSectionKind.Quiet
    }

    fun showSearch(kind: WorkSectionKind, unfilteredCount: Int): Boolean {
        return unfilteredCount >= SEARCH_MIN &&
            (kind == WorkSectionKind.Active || kind == WorkSectionKind.Quiet)
    }

    /**
     * Visual filter. Blank query returns the same list instance so A1 can compare sizes
     * against [presentAll] / getActivityList without a second copy.
     */
    fun <T> filterVisible(
        items: List<T>,
        query: String,
        label: (T) -> String,
        packageName: (T) -> String,
    ): List<T> {
        val q = query.trim()
        if (q.isEmpty()) return items
        return items.filter { item ->
            label(item).contains(q, ignoreCase = true) ||
                packageName(item).contains(q, ignoreCase = true)
        }
    }

    /** Header · n is the catalog size, which matches the grid when the query is empty. */
    fun headerCount(apiSize: Int): Int = apiSize

    /**
     * A1 counts icons with an empty query. A non-blank query may show fewer cells;
     * that does not change the catalog size.
     */
    fun acceptanceCount(apiSize: Int, visibleSize: Int, query: String): Int {
        return if (query.isBlank()) apiSize else visibleSize
    }

    /**
     * True only when a real quiet-mode read returned true for at least one profile.
     * Any failure/unknown reading is not treated as paused. All-false means not paused.
     */
    fun combineQuiet(readings: List<Boolean?>): Boolean? {
        if (readings.any { it == true }) return true
        if (readings.isNotEmpty() && readings.all { it == false }) return false
        return null
    }

    fun refreshChanged(before: WorkSnapshot, after: WorkSnapshot): Boolean = before != after

    fun settingsStatus(
        loaded: Boolean,
        hasProfile: Boolean,
        quietEnabled: Boolean?,
        loadFailed: Boolean,
        activityCount: Int,
    ): WorkSettingsStatus {
        return when {
            !loaded -> WorkSettingsStatus.Unknown
            loadFailed && activityCount == 0 -> WorkSettingsStatus.Failed
            quietEnabled == true -> WorkSettingsStatus.Quiet
            !hasProfile -> WorkSettingsStatus.Absent
            else -> WorkSettingsStatus.Visible
        }
    }
}

enum class WorkSectionKind {
    Hidden,
    Active,
    Empty,
    Quiet,
    Error,
}

enum class WorkSettingsStatus {
    Unknown,
    Absent,
    Visible,
    Quiet,
    Failed,
}

data class WorkSnapshot(
    val profile: Boolean,
    val failed: Boolean,
    val quiet: Boolean?,
    val keys: List<String>,
)

data class WorkActivityId(
    val packageName: String,
    val className: String,
    val label: String,
)
