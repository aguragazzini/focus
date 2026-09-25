package com.foco.launcher.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

/**
 * Universal Roman calendar of the 1962 Missal (rubrics of 1960).
 * Fixed days come from the bundled asset. Moveable days use the Gregorian computus.
 * This is the traditional sanctorale, not the 1969 General Roman Calendar.
 */
object Santoral1962 {
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
            context.assets.open("santoral_1962.json").bufferedReader().use { reader ->
                parse(reader.readText())
            }
        }.getOrNull() ?: return
        cached.compareAndSet(null, catalog)
    }

    fun parse(raw: String): SantoralCatalog = json.decodeFromString(raw)

    fun resolve(catalog: SantoralCatalog, date: LocalDate): SantoralDay {
        val candidates = ArrayList<Cand>()
        for (feast in feastsOn(catalog, date)) {
            candidates += Cand(
                name = feast.name,
                rank = feast.rank,
                summary = feast.summary,
                note = feast.note,
                score = scoreOf(feast.rank),
                commemorate = true,
            )
        }
        candidates += temporal(date)
        if (candidates.isEmpty()) return plainFeria(date)
        val winner = candidates.maxWith(compareBy(Cand::score))
        if (winner.score <= FERIA) return plainFeria(date)
        val notes = LinkedHashSet<String>()
        if (winner.note.isNotBlank()) notes += winner.note.trim()
        for (other in candidates) {
            if (other === winner || !other.commemorate) continue
            if (winner.score >= CLASS_I && other.score <= CLASS_IV) continue
            if (other.score < CLASS_IV) continue
            notes += "Conmemoración: ${other.name}."
        }
        if (inChristmasOctave(date) && !winner.name.contains("octava", ignoreCase = true) &&
            !winner.name.contains("Navidad")
        ) {
            notes += "Dentro de la octava de Navidad."
        }
        if (isEmber(date) && winner.name != "Témporas") notes += "Témporas."
        return SantoralDay(
            name = winner.name,
            rank = winner.rank,
            summary = winner.summary,
            note = notes.joinToString(" ").trim(),
        )
    }

    /** Gregorian computus. Easter 2026 is 5 April. */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    private fun feastsOn(catalog: SantoralCatalog, date: LocalDate): List<SantoralFixed> {
        val year = date.year
        val found = ArrayList<SantoralFixed>()
        val annunciation = annunciation(year)
        val joseph = josephTransfer(year)
        val souls = allSouls(year)
        if (date == annunciation && date != LocalDate.of(year, 3, 25)) {
            catalog.days["03-25"]?.let(found::add)
        }
        if (joseph != null && date == joseph) catalog.days["03-19"]?.let(found::add)
        if (date == souls && date.dayOfMonth != 2) catalog.days["11-02"]?.let(found::add)
        val suppressed = (date.monthValue == 3 && date.dayOfMonth == 25 && annunciation != date) ||
            (date.monthValue == 3 && date.dayOfMonth == 19 && joseph != null) ||
            (date.monthValue == 11 && date.dayOfMonth == 2 && souls != date)
        if (!suppressed) {
            catalog.days[key(date)]?.let(found::add)
        }
        return found
    }

    private fun temporal(date: LocalDate): List<Cand> {
        val year = date.year
        val easter = easter(year)
        val out = ArrayList<Cand>()
        fun add(day: LocalDate, name: String, rank: String, summary: String, score: Int) {
            if (date == day) {
                out += Cand(name, rank, summary, "", score, false)
            }
        }
        add(easter, "Domingo de Resurrección", "I", "Pascua. El Señor resucitó, como lo había anunciado.", 110)
        add(easter.minusDays(2), "Viernes Santo", "I", "Pasión del Señor. La Iglesia no celebra santo ese día.", 110)
        add(easter.minusDays(1), "Sábado Santo", "I", "Vigilia de la Resurrección, junto al sepulcro.", 110)
        add(easter.minusDays(3), "Jueves Santo", "I", "Cena del Señor, institución de la Eucaristía.", 110)
        for (offset in 1..6) {
            val name = "${weekday(easter.plusDays(offset.toLong()))} de la octava de Pascua"
            add(easter.plusDays(offset.toLong()), name, "I", "Día de la octava privilegiada de Pascua.", 100)
        }
        add(easter.plusDays(7), "Domingo in albis", "I", "Octava de Pascua. Domingo de la Misericordia en el rito antiguo.", 90)
        add(easter.minusDays(46), "Miércoles de Ceniza", "I", "Comienzo de la Cuaresma. Imposición de la ceniza.", 88)
        add(easter.minusDays(6), "Lunes Santo", "I", "Feria mayor de Semana Santa.", 88)
        add(easter.minusDays(5), "Martes Santo", "I", "Feria mayor de Semana Santa.", 88)
        add(easter.minusDays(4), "Miércoles Santo", "I", "Feria mayor de Semana Santa.", 88)
        add(easter.plusDays(39), "Ascensión del Señor", "I", "Cristo sube al cielo a los cuarenta días de su resurrección.", 90)
        add(easter.plusDays(48), "Vigilia de Pentecostés", "I", "Víspera de la venida del Espíritu Santo.", 90)
        add(easter.plusDays(49), "Domingo de Pentecostés", "I", "Venida del Espíritu Santo sobre los apóstoles.", 100)
        for (offset in 1..6) {
            val day = easter.plusDays(49 + offset.toLong())
            val name = "${weekday(day)} de la octava de Pentecostés"
            add(day, name, "I", "Día de la octava privilegiada de Pentecostés.", 100)
        }
        add(easter.plusDays(56), "Santísima Trinidad", "I", "Fiesta de la Santísima Trinidad, el domingo siguiente a Pentecostés.", 90)
        add(easter.plusDays(60), "Santísimo Cuerpo de Cristo", "I", "Corpus Christi. Solemnidad del Cuerpo y la Sangre del Señor.", 90)
        add(easter.plusDays(68), "Sagrado Corazón de Jesús", "I", "Viernes posterior a la octava de Corpus Christi.", 90)
        add(christTheKing(year), "Cristo Rey", "I", "Último domingo de octubre en el calendario de 1962.", 90)
        add(holyName(year), "Santísimo Nombre de Jesús", "II", "El domingo entre el 2 y el 5 de enero, o el 2 si no lo hay.", 60)

        lentSunday(date, easter)?.let { out += it }
        advent(date)?.let { out += it }
        epiphanySunday(date)?.let { out += it }
        pentecostSunday(date, easter)?.let { out += it }
        if (date == easter.minusDays(63)) {
            out += Cand("Domingo de Septuagésima", "II", "Comienza el tiempo de Septuagésima.", "", 55, false)
        }
        if (date == easter.minusDays(56)) {
            out += Cand("Domingo de Sexagésima", "II", "Segundo domingo de preparación a la Cuaresma.", "", 55, false)
        }
        if (date == easter.minusDays(49)) {
            out += Cand("Domingo de Quincuagésima", "II", "Último domingo antes de la Ceniza.", "", 55, false)
        }
        if (date == easter.plusDays(42)) {
            out += Cand("Domingo después de la Ascensión", "II", "Domingo entre la Ascensión y Pentecostés.", "", 55, false)
        }
        if (isEmber(date) && date != easter.plusDays(49) && date.dayOfWeek != DayOfWeek.SUNDAY) {
            val insidePentecostOctave = !date.isBefore(easter.plusDays(49)) && !date.isAfter(easter.plusDays(55))
            if (!insidePentecostOctave) {
                out += Cand("Témporas", "II", "Día de témporas: ayuno y oración por los frutos y las órdenes.", "", 50, false)
            }
        }
        if (isAdventFeria(date)) {
            out += Cand("Feria de Adviento", "II", "Feria mayor de Adviento, del 17 al 23 de diciembre.", "", 50, false)
        }
        if (isLentFeria(date, easter)) {
            out += Cand("Feria de Cuaresma", null, "Día de cuaresma sin fiesta que lo desplace.", "", 25, false)
        }
        return out
    }

    private fun plainFeria(date: LocalDate): SantoralDay {
        val notes = ArrayList<String>()
        if (isEmber(date)) notes += "Témporas."
        if (isAdventFeria(date)) notes += "Feria mayor de Adviento."
        if (inChristmasOctave(date)) notes += "Dentro de la octava de Navidad."
        return SantoralDay(
            name = "Feria",
            rank = null,
            summary = "Día sin fiesta propia en el calendario tradicional.",
            note = notes.joinToString(" "),
        )
    }

    private fun lentSunday(date: LocalDate, easter: LocalDate): Cand? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        val names = listOf(
            easter.minusDays(42) to "Domingo I de Cuaresma",
            easter.minusDays(35) to "Domingo II de Cuaresma",
            easter.minusDays(28) to "Domingo III de Cuaresma",
            easter.minusDays(21) to "Domingo IV de Cuaresma",
            easter.minusDays(14) to "Domingo de Pasión",
            easter.minusDays(7) to "Domingo de Ramos",
        )
        val match = names.firstOrNull { it.first == date } ?: return null
        return Cand(match.second, "I", "Domingo mayor del temporal de Cuaresma.", "", 85, false)
    }

    private fun advent(date: LocalDate): Cand? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        val first = adventFirst(date.year)
        if (date.isBefore(first)) return null
        val weeks = ((date.toEpochDay() - first.toEpochDay()) / 7).toInt()
        if (weeks !in 0..3) return null
        val title = "Domingo ${ROMAN[weeks]} de Adviento"
        val score = if (weeks == 0) 85 else 70
        val rank = if (weeks == 0) "I" else "II"
        return Cand(title, rank, "Domingo de Adviento en el temporal tradicional.", "", score, false)
    }

    private fun epiphanySunday(date: LocalDate): Cand? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        val epiphany = LocalDate.of(date.year, 1, 6)
        val septuagesima = easter(date.year).minusDays(63)
        if (!date.isAfter(epiphany) || !date.isBefore(septuagesima)) return null
        var cursor = nextSundayAfter(epiphany)
        var number = 1
        while (cursor.isBefore(septuagesima) && cursor != date) {
            number += 1
            cursor = cursor.plusDays(7)
        }
        if (cursor != date || number !in 1..6) return null
        return Cand(
            "Domingo $number después de la Epifanía",
            "II",
            "Domingo del tiempo después de la Epifanía.",
            "",
            55,
            false,
        )
    }

    private fun pentecostSunday(date: LocalDate, easter: LocalDate): Cand? {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return null
        val pentecost = easter.plusDays(49)
        val advent = adventFirst(date.year)
        if (!date.isAfter(pentecost) || !date.isBefore(advent)) return null
        val weeks = ((date.toEpochDay() - pentecost.toEpochDay()) / 7).toInt()
        if (weeks == 1) return null
        if (weeks !in 2..24) return null
        if (date == christTheKing(date.year)) return null
        return Cand(
            "Domingo $weeks después de Pentecostés",
            "II",
            "Domingo del tiempo después de Pentecostés.",
            "",
            55,
            false,
        )
    }

    private fun isLentFeria(date: LocalDate, easter: LocalDate): Boolean {
        if (date.dayOfWeek == DayOfWeek.SUNDAY) return false
        val start = easter.minusDays(45)
        val holyMonday = easter.minusDays(6)
        return !date.isBefore(start) && date.isBefore(holyMonday)
    }

    private fun isAdventFeria(date: LocalDate): Boolean {
        if (date.monthValue != 12 || date.dayOfWeek == DayOfWeek.SUNDAY) return false
        return date.dayOfMonth in 17..23
    }

    private fun inChristmasOctave(date: LocalDate): Boolean {
        return date.monthValue == 12 && date.dayOfMonth in 26..31
    }

    fun isEmber(date: LocalDate): Boolean {
        val year = date.year
        val easter = easter(year)
        val lent = emberWeek(easter.minusDays(42))
        val pentecost = emberWeek(easter.plusDays(49))
        val september = emberAfter(LocalDate.of(year, 9, 14))
        val december = emberAfter(LocalDate.of(year, 12, 13))
        return date in lent || date in pentecost || date in september || date in december
    }

    private fun emberWeek(sunday: LocalDate): Set<LocalDate> {
        return setOf(sunday.plusDays(3), sunday.plusDays(5), sunday.plusDays(6))
    }

    private fun emberAfter(feast: LocalDate): Set<LocalDate> {
        val wednesday = nextWeekday(feast, DayOfWeek.WEDNESDAY)
        return setOf(wednesday, wednesday.plusDays(2), wednesday.plusDays(3))
    }

    private fun nextWeekday(after: LocalDate, target: DayOfWeek): LocalDate {
        var cursor = after.plusDays(1)
        while (cursor.dayOfWeek != target) cursor = cursor.plusDays(1)
        return cursor
    }

    private fun nextSundayAfter(day: LocalDate): LocalDate = nextWeekday(day, DayOfWeek.SUNDAY)

    private fun adventFirst(year: Int): LocalDate {
        val christmas = LocalDate.of(year, 12, 25)
        var fourth = christmas
        while (fourth.dayOfWeek != DayOfWeek.SUNDAY) fourth = fourth.minusDays(1)
        if (fourth == christmas) fourth = christmas.minusDays(7)
        return fourth.minusDays(21)
    }

    private fun christTheKing(year: Int): LocalDate {
        var day = LocalDate.of(year, 10, 31)
        while (day.dayOfWeek != DayOfWeek.SUNDAY) day = day.minusDays(1)
        return day
    }

    private fun holyName(year: Int): LocalDate {
        for (day in 2..5) {
            val date = LocalDate.of(year, 1, day)
            if (date.dayOfWeek == DayOfWeek.SUNDAY) return date
        }
        return LocalDate.of(year, 1, 2)
    }

    private fun annunciation(year: Int): LocalDate {
        val native = LocalDate.of(year, 3, 25)
        val easter = easter(year)
        val palm = easter.minusDays(7)
        if (!native.isBefore(palm) && !native.isAfter(easter)) return easter.plusDays(8)
        return native
    }

    private fun josephTransfer(year: Int): LocalDate? {
        val native = LocalDate.of(year, 3, 19)
        val easter = easter(year)
        val palm = easter.minusDays(7)
        if (native.isBefore(palm) || native.isAfter(easter)) return null
        var day = easter.plusDays(8)
        if (annunciation(year) == day) day = day.plusDays(1)
        return day
    }

    private fun allSouls(year: Int): LocalDate {
        val native = LocalDate.of(year, 11, 2)
        return if (native.dayOfWeek == DayOfWeek.SUNDAY) native.plusDays(1) else native
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

    private fun scoreOf(rank: String): Int = when (rank) {
        "I" -> CLASS_I
        "II" -> CLASS_II
        "III" -> CLASS_III
        "IV" -> CLASS_IV
        else -> FERIA
    }

    private fun key(date: LocalDate): String = "%02d-%02d".format(date.monthValue, date.dayOfMonth)

    private data class Cand(
        val name: String,
        val rank: String?,
        val summary: String,
        val note: String,
        val score: Int,
        val commemorate: Boolean,
    )

    private const val CLASS_I = 80
    private const val CLASS_II = 60
    private const val CLASS_III = 40
    private const val CLASS_IV = 20
    private const val FERIA = 5

    private val ROMAN = arrayOf("I", "II", "III", "IV")
}

@Serializable
data class SantoralCatalog(
    val calendar: String,
    val rubrics: String,
    val rite: String,
    val source: String,
    val days: Map<String, SantoralFixed>,
)

@Serializable
data class SantoralFixed(
    val name: String,
    val rank: String,
    val summary: String,
    val note: String = "",
)

data class SantoralDay(
    val name: String,
    val rank: String?,
    val summary: String,
    val note: String,
)
