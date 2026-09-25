package com.foco.launcher.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/**
 * Fixed weekly meals for Agus. Weekday is [ZONE], not the device zone.
 * Saturday and Sunday share one soft line. No network.
 */
object DietPlan {
    val ZONE: ZoneId = ZoneId.of("America/Argentina/Cordoba")

    private val json = Json { ignoreUnknownKeys = true }
    private val cached = AtomicReference<DietCatalog?>(null)

    fun peek(): DietCatalog? = cached.get()

    fun store(catalog: DietCatalog) {
        cached.compareAndSet(null, catalog)
    }

    /** Parses the bundled asset off the caller thread. Safe to call more than once. */
    fun load(context: Context) {
        if (cached.get() != null) return
        val catalog = runCatching {
            context.assets.open("plan_ragazzini.json").bufferedReader().use { reader ->
                parse(reader.readText())
            }
        }.getOrNull() ?: return
        cached.compareAndSet(null, catalog)
    }

    fun parse(raw: String): DietCatalog = json.decodeFromString(raw)

    fun resolve(catalog: DietCatalog, date: LocalDate): DietDay? {
        val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        if (weekend) {
            val title = catalog.weekend.title.trim()
            if (title.isEmpty() && catalog.weekend.rules.isEmpty()) return null
            return DietDay(
                weekend = true,
                theme = title,
                breakfast = "",
                breakfastTip = "",
                lunch = "",
                lunchTip = "",
                dinner = "",
                dinnerTip = "",
                alternateTitles = emptyList(),
                rules = catalog.weekend.rules.map { it.trim() }.filter { it.isNotEmpty() },
            )
        }
        val entry = catalog.days.firstOrNull { it.weekday.equals(date.dayOfWeek.name, ignoreCase = true) }
            ?: return null
        val theme = entry.theme.trim()
        if (theme.isEmpty()) return null
        return DietDay(
            weekend = false,
            theme = theme,
            breakfast = entry.breakfast.trim(),
            breakfastTip = entry.breakfastTip.trim(),
            lunch = entry.lunch.trim(),
            lunchTip = entry.lunchTip.trim(),
            dinner = entry.dinner.trim(),
            dinnerTip = entry.dinnerTip.trim(),
            alternateTitles = entry.alternateTitles.map { it.trim() }.filter { it.isNotEmpty() }.take(3),
            rules = emptyList(),
        )
    }

    fun homeTitle(day: DietDay): String {
        return if (day.weekend) day.theme else "Hoy · ${day.theme}"
    }
}

@Serializable
data class DietCatalog(
    val weekend: DietWeekend = DietWeekend(),
    val days: List<DietEntry> = emptyList(),
)

@Serializable
data class DietWeekend(
    val title: String = "",
    val rules: List<String> = emptyList(),
)

@Serializable
data class DietEntry(
    val weekday: String = "",
    val theme: String = "",
    val breakfast: String = "",
    val breakfastTip: String = "",
    val lunch: String = "",
    val lunchTip: String = "",
    val dinner: String = "",
    val dinnerTip: String = "",
    val alternateTitles: List<String> = emptyList(),
)

data class DietDay(
    val weekend: Boolean,
    val theme: String,
    val breakfast: String,
    val breakfastTip: String,
    val lunch: String,
    val lunchTip: String,
    val dinner: String,
    val dinnerTip: String,
    val alternateTitles: List<String>,
    val rules: List<String>,
)
