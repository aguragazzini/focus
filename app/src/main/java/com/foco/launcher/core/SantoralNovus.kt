package com.foco.launcher.core

import android.content.Context
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

/**
 * General Roman Calendar (1969), not the 1962 Missal.
 * Fixed days come from the bundled asset. Moveable days use the Gregorian computus.
 * Ascension and Corpus Christi stay on Sunday, as kept in Argentina. Epiphany stays on 6 January.
 */
object SantoralNovus {
    private val json = Json { ignoreUnknownKeys = true }
    private val cached = AtomicReference<SantoralCatalog?>(null)

    fun peek(): SantoralCatalog? = cached.get()

    fun store(catalog: SantoralCatalog) {
        cached.compareAndSet(null, catalog)
    }

    /** Parses the bundled asset off the caller thread. Safe to call more than once. */
    fun load(context: Context) {
        if (cached.get() != null) return
        val catalog = runCatching {
            context.assets.open("santoral_novus.json").bufferedReader().use { reader ->
                parse(reader.readText())
            }
        }.getOrNull() ?: return
        cached.compareAndSet(null, catalog)
    }

    fun parse(raw: String): SantoralCatalog = json.decodeFromString(raw)

    fun read(raw: String): SantoralCatalog? = runCatching { parse(raw) }.getOrNull()

    fun resolve(catalog: SantoralCatalog, date: LocalDate): SantoralDay {
        val candidates = ArrayList<Cand>()
        candidates += fixed(catalog, date)
        candidates += temporal(date)
        val winner = candidates.maxByOrNull { it.score }
        if (winner == null || winner.score < WEEKDAY) return plain(date)
        return SantoralDay(
            name = winner.name,
            rank = winner.rank,
            summary = winner.summary,
            note = winner.note.trim(),
        )
    }

    fun baptism(year: Int): LocalDate {
        var day = LocalDate.of(year, 1, 6).plusDays(1)
        while (day.dayOfWeek != DayOfWeek.SUNDAY) day = day.plusDays(1)
        return day
    }

    fun adventSunday(year: Int): LocalDate {
        var day = LocalDate.of(year, 11, 27)
        while (day.dayOfWeek != DayOfWeek.SUNDAY) day = day.plusDays(1)
        return day
    }

    fun christTheKing(year: Int): LocalDate = adventSunday(year).minusDays(7)

    private fun fixed(catalog: SantoralCatalog, date: LocalDate): List<Cand> {
        val year = date.year
        val out = ArrayList<Cand>()
        fun add(key: String) {
            val feast = catalog.days[key] ?: return
            out += Cand(feast.name, feast.rank, feast.summary, feast.note, scoreOf(feast.rank, key))
        }
        val joseph = josephDate(year)
        val annunciation = annunciationDate(year)
        val souls = allSoulsDate(year)
        if (date == joseph) add("03-19")
        if (date == annunciation) add("03-25")
        if (date == souls) add("11-02")
        val key = key(date)
        val movedAway = (key == "03-19" && joseph != date) ||
            (key == "03-25" && annunciation != date) ||
            (key == "11-02" && souls != date)
        if (!movedAway && key != "03-19" && key != "03-25" && key != "11-02") add(key)
        return out
    }

    private fun temporal(date: LocalDate): List<Cand> {
        val year = date.year
        val easter = Santoral1962.easter(year)
        val out = ArrayList<Cand>()
        fun add(day: LocalDate, name: String, rank: String?, summary: String, score: Int) {
            if (date == day) out += Cand(name, rank, summary, "", score)
        }
        add(easter, "Domingo de Pascua", "S", "Resurrección del Señor.", 110)
        add(easter.minusDays(2), "Viernes Santo", "S", "Pasión del Señor.", 110)
        add(easter.minusDays(1), "Sábado Santo", "S", "Vigilia pascual.", 110)
        add(easter.minusDays(3), "Jueves Santo", "S", "Cena del Señor.", 100)
        add(easter.minusDays(6), "Lunes Santo", null, "Feria de Semana Santa.", 70)
        add(easter.minusDays(5), "Martes Santo", null, "Feria de Semana Santa.", 70)
        add(easter.minusDays(4), "Miércoles Santo", null, "Feria de Semana Santa.", 70)
        add(easter.minusDays(46), "Miércoles de Ceniza", null, "Comienzo de la Cuaresma.", 80)
        for (offset in 1..6) {
            val day = easter.plusDays(offset.toLong())
            add(day, "${weekday(day)} de la octava de Pascua", "S", "Octava de Pascua.", 100)
        }
        add(easter.plusDays(42), "Ascensión del Señor", "S", "Domingo de la Ascensión.", 100)
        add(easter.plusDays(49), "Domingo de Pentecostés", "S", "Venida del Espíritu Santo.", 110)
        add(easter.plusDays(56), "Santísima Trinidad", "S", "Domingo después de Pentecostés.", 100)
        add(easter.plusDays(63), "Santísimo Cuerpo y Sangre de Cristo", "S", "Domingo del Cuerpo del Señor.", 100)
        add(easter.plusDays(68), "Sagrado Corazón de Jesús", "S", "Viernes posterior al Corpus.", 100)
        add(christTheKing(year), "Jesucristo, Rey del universo", "S", "Último domingo del tiempo ordinario.", 100)
        add(baptism(year), "Bautismo del Señor", "F", "Cierra el tiempo de Navidad.", 80)
        add(holyFamily(year), "Sagrada Familia", "F", "Domingo después de Navidad.", 80)

        lentSunday(date, easter)?.let { out += it }
        easterSunday(date, easter)?.let { out += it }
        adventSunday(date)?.let { out += it }
        seasonFeria(date, easter)?.let { out += it }
        ordinaryName(date)?.let { name ->
            out += Cand(name, null, "Tiempo ordinario.", "", WEEKDAY)
        }
        return out
    }

    private fun plain(date: LocalDate): SantoralDay {
        val name = ordinaryName(date) ?: "Feria"
        val summary = if (name == "Feria") {
            "Día sin celebración propia en el calendario romano."
        } else {
            "Tiempo ordinario."
        }
        return SantoralDay(name, null, summary, "")
    }

    private fun lentSunday(date: LocalDate, easter: LocalDate): Cand? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        val names = listOf(
            easter.minusDays(42) to "Domingo I de Cuaresma",
            easter.minusDays(35) to "Domingo II de Cuaresma",
            easter.minusDays(28) to "Domingo III de Cuaresma",
            easter.minusDays(21) to "Domingo IV de Cuaresma",
            easter.minusDays(14) to "Domingo V de Cuaresma",
            easter.minusDays(7) to "Domingo de Ramos",
        )
        val match = names.firstOrNull { it.first == date } ?: return null
        return Cand(match.second, "S", "Domingo de Cuaresma.", "", 90)
    }

    private fun easterSunday(date: LocalDate, easter: LocalDate): Cand? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        if (!date.isAfter(easter) || !date.isBefore(easter.plusDays(42))) return null
        val n = ((date.toEpochDay() - easter.toEpochDay()) / 7).toInt() + 1
        if (n !in 2..6) return null
        return Cand("Domingo ${ROMAN[n - 1]} de Pascua", "S", "Domingo del tiempo de Pascua.", "", 90)
    }

    private fun adventSunday(date: LocalDate): Cand? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        val first = adventSunday(date.year)
        if (date.isBefore(first)) return null
        val weeks = ((date.toEpochDay() - first.toEpochDay()) / 7).toInt()
        if (weeks !in 0..3) return null
        return Cand("Domingo ${ROMAN[weeks]} de Adviento", "S", "Domingo de Adviento.", "", 90)
    }

    private fun seasonFeria(date: LocalDate, easter: LocalDate): Cand? {
        if (date.dayOfWeek == DayOfWeek.SUNDAY) return null
        val ash = easter.minusDays(46)
        val lentEnd = easter.minusDays(7)
        if (!date.isBefore(ash) && date.isBefore(lentEnd) && date != ash) {
            return Cand("Feria de Cuaresma", null, "Día de cuaresma.", "", 40)
        }
        val octaveEnd = easter.plusDays(6)
        val ascension = easter.plusDays(42)
        val pentecost = easter.plusDays(49)
        if (date.isAfter(octaveEnd) && date.isBefore(ascension)) {
            return Cand("Feria del tiempo de Pascua", null, "Tiempo de Pascua.", "", 22)
        }
        if (date.isAfter(ascension) && date.isBefore(pentecost)) {
            return Cand("Feria después de la Ascensión", null, "Tiempo de Pascua.", "", 22)
        }
        if (isAdventDay(date) && date.dayOfMonth in 17..24) {
            val name = if (date.monthValue == 12 && date.dayOfMonth == 24) "Vigilia de Navidad" else "Feria mayor de Adviento"
            return Cand(name, null, "Adviento.", "", 45)
        }
        if (isAdventDay(date)) {
            return Cand("Feria de Adviento", null, "Adviento.", "", 22)
        }
        if (date.monthValue == 12 && date.dayOfMonth in 29..31) {
            return Cand("Día de la octava de Navidad", null, "Octava de Navidad.", "", 22)
        }
        return null
    }

    private fun ordinaryName(date: LocalDate): String? {
        val week = ordinaryWeek(date) ?: return null
        val title = if (date.dayOfWeek == DayOfWeek.SUNDAY) "Domingo" else weekday(date)
        return "$title de la semana $week del tiempo ordinario"
    }

    private fun ordinaryWeek(date: LocalDate): Int? {
        val baptism = baptism(date.year)
        if (date.isAfter(baptism) && date.isBefore(baptism.plusDays(7))) return 1
        val sunday = weekSunday(date)
        if (sunday == baptism) return null
        return ordinarySundayNumber(sunday)
    }

    private fun ordinarySundayNumber(date: LocalDate): Int? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        val year = date.year
        val easter = Santoral1962.easter(year)
        val baptism = baptism(year)
        val ash = easter.minusDays(46)
        if (date.isAfter(baptism) && date.isBefore(ash)) {
            val weeks = ((date.toEpochDay() - baptism.toEpochDay()) / 7).toInt()
            return weeks + 1
        }
        val pentecost = easter.plusDays(49)
        val king = christTheKing(year)
        if (date.isAfter(pentecost) && !date.isAfter(king)) {
            val before = ((king.toEpochDay() - date.toEpochDay()) / 7).toInt()
            return 34 - before
        }
        return null
    }

    private fun holyFamily(year: Int): LocalDate {
        val christmas = LocalDate.of(year, 12, 25)
        if (christmas.dayOfWeek == DayOfWeek.SUNDAY) return LocalDate.of(year, 12, 30)
        var day = christmas.plusDays(1)
        while (day.dayOfWeek != DayOfWeek.SUNDAY) day = day.plusDays(1)
        return day
    }

    private fun josephDate(year: Int): LocalDate {
        val native = LocalDate.of(year, 3, 19)
        val easter = Santoral1962.easter(year)
        val palm = easter.minusDays(7)
        if (!native.isBefore(palm) && !native.isAfter(easter)) {
            var day = easter.plusDays(8)
            if (annunciationDate(year) == day) day = day.plusDays(1)
            return day
        }
        if (native.dayOfWeek == DayOfWeek.SUNDAY) return native.plusDays(1)
        return native
    }

    private fun annunciationDate(year: Int): LocalDate {
        val native = LocalDate.of(year, 3, 25)
        val easter = Santoral1962.easter(year)
        val palm = easter.minusDays(7)
        val octaveSunday = easter.plusDays(7)
        if (!native.isBefore(palm) && !native.isAfter(octaveSunday)) return easter.plusDays(8)
        return native
    }

    private fun allSoulsDate(year: Int): LocalDate {
        val native = LocalDate.of(year, 11, 2)
        return if (native.dayOfWeek == DayOfWeek.SUNDAY) native.plusDays(1) else native
    }

    private fun isAdventDay(date: LocalDate): Boolean {
        if (date.monthValue == 11 && !date.isBefore(adventSunday(date.year))) return true
        return date.monthValue == 12 && date.dayOfMonth <= 24
    }

    private fun weekSunday(date: LocalDate): LocalDate {
        var day = date
        while (day.dayOfWeek != DayOfWeek.SUNDAY) day = day.minusDays(1)
        return day
    }

    private fun weekday(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Lunes"
        DayOfWeek.TUESDAY -> "Martes"
        DayOfWeek.WEDNESDAY -> "Miércoles"
        DayOfWeek.THURSDAY -> "Jueves"
        DayOfWeek.FRIDAY -> "Viernes"
        DayOfWeek.SATURDAY -> "Sábado"
        DayOfWeek.SUNDAY -> "Domingo"
    }

    private fun scoreOf(rank: String, key: String): Int = when {
        rank == "S" -> 100
        rank == "A" || key in LORD_FEASTS -> 75
        rank == "F" -> 50
        rank == "M" -> 30
        rank == "O" -> 15
        else -> 5
    }

    private fun key(date: LocalDate): String = "%02d-%02d".format(date.monthValue, date.dayOfMonth)

    private data class Cand(
        val name: String,
        val rank: String?,
        val summary: String,
        val note: String,
        val score: Int,
    )

    private const val WEEKDAY = 10
    private val ROMAN = arrayOf("I", "II", "III", "IV", "V", "VI", "VII")
    private val LORD_FEASTS = setOf("02-02", "08-06", "09-14")
}
