package com.foco.launcher.registry

import kotlinx.serialization.Serializable

/**
 * One info block on a home page. [note], [count], and [pins] are the local
 * payload for Nota, Contador, and Favoritos. Other types ignore them.
 */
@Serializable
data class PageBlock(
    val id: String,
    val type: String,
    val note: String = "",
    val count: Int = 0,
    val pins: List<String> = emptyList(),
)

/**
 * Short-label modules a page can show. Stable ids. Duplicates are refused
 * except for spacer, note, tally, and favorites.
 */
object PageBlocks {
    const val RELOJ = "reloj"
    const val FECHA = "fecha"
    const val BATERIA = "bateria"
    const val ALARMA = "alarma"
    const val VETUS = "vetus"
    const val NOVUS = "novus"
    const val AGENDA = "agenda"
    const val PROXIMO = "proximo"
    const val COMIDA = "comida"
    const val CLIMA = "clima"
    const val SEMANA = "semana"
    const val LUNA = "luna"
    const val SILENCIO = "silencio"
    const val PAUSAR = "pausar"
    const val ATAJOS = "atajos"
    const val TRABAJO = "trabajo"
    const val FAVORITOS = "favoritos"
    const val RECORDATORIO = "recordatorio"
    const val NOTA = "nota"
    const val CONTADOR = "contador"
    const val DATOS = "datos"
    const val WIFI = "wifi"
    const val ESPACIO = "espacio"
    const val ONOMASTICO = "onomastico"
    const val FRASE = "frase"
    const val PASOS = "pasos"
    const val CALENDARIO = "calendario"
    const val VACIO = "vacio"

    const val NOTE_MAX = 240
    const val COUNT_MAX = 9999

    val ALL: List<String> = listOf(
        RELOJ, FECHA, BATERIA, ALARMA, VETUS, NOVUS, AGENDA, PROXIMO, COMIDA, CLIMA,
        SEMANA, LUNA, SILENCIO, PAUSAR, ATAJOS, TRABAJO, FAVORITOS, RECORDATORIO,
        NOTA, CONTADOR, DATOS, WIFI, ESPACIO, ONOMASTICO, FRASE, PASOS, CALENDARIO, VACIO,
    )

    val REPEATABLE = setOf(VACIO, NOTA, CONTADOR, FAVORITOS)

    private val known = ALL.toSet()

    fun allowsDuplicate(type: String): Boolean = type in REPEATABLE

    /** What a page shows before the user edits its blocks. Personal and Trabajo keep their grids. */
    fun defaultsFor(pageType: String): List<String> {
        return when (pageType) {
            HomePages.TYPE_CLOCK -> listOf(RELOJ, FECHA, BATERIA, ALARMA, VETUS, NOVUS)
            HomePages.TYPE_AGENDA -> listOf(AGENDA)
            HomePages.TYPE_DIET -> listOf(COMIDA)
            else -> emptyList()
        }
    }

    fun effective(page: HomePageSpec): List<PageBlock> {
        if (page.blocksSet) return page.blocks
        return defaultsFor(page.type).map { type ->
            PageBlock(id = "${page.id}:$type", type = type)
        }
    }

    fun clean(blocks: List<PageBlock>): List<PageBlock> {
        val seen = HashSet<String>()
        val out = ArrayList<PageBlock>(blocks.size)
        for (block in blocks) {
            val id = block.id.trim()
            val type = block.type.trim().lowercase()
            if (id.isEmpty() || type !in known || !seen.add(id)) continue
            if (!allowsDuplicate(type) && out.any { it.type == type }) continue
            out += block.copy(
                id = id,
                type = type,
                note = block.note.take(NOTE_MAX),
                count = block.count.coerceIn(0, COUNT_MAX),
                pins = block.pins.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(8),
            )
        }
        return out
    }

    fun add(blocks: List<PageBlock>, type: String, id: String): List<PageBlock> {
        val kind = type.trim().lowercase()
        val blockId = id.trim()
        if (kind !in known || blockId.isEmpty() || blocks.any { it.id == blockId }) return blocks
        if (!allowsDuplicate(kind) && blocks.any { it.type == kind }) return blocks
        return blocks + PageBlock(id = blockId, type = kind)
    }

    fun remove(blocks: List<PageBlock>, id: String): List<PageBlock> {
        return blocks.filter { it.id != id }
    }

    fun move(blocks: List<PageBlock>, id: String, delta: Int): List<PageBlock> {
        if (delta == 0) return blocks
        val index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return blocks
        val target = index + delta
        if (target !in blocks.indices) return blocks
        val next = blocks.toMutableList()
        val item = next.removeAt(index)
        next.add(target, item)
        return next
    }

    fun update(blocks: List<PageBlock>, id: String, transform: (PageBlock) -> PageBlock): List<PageBlock> {
        return clean(blocks.map { if (it.id == id) transform(it) else it })
    }
}
