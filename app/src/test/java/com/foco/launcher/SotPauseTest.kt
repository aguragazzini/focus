package com.foco.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Camila §3.1 drafts pause as "filter off". Product override: Pausar avisos is
 * silence (NLS cancel in-scope, Andrés §2). The attached notes stay verbatim;
 * shipped strings must not pick up the rejected copy.
 */
class SotPauseTest {
    @Test
    fun attachedNotesStayVerbatimAndShippedCopyIsSilence() {
        val camila = File("../docs/foco-launcher-ux-v0.7-drag-grupos.md").readText()
        val andres = File("../docs/foco-launcher-arch-pause-v0.6.md").readText()
        val index = File("../docs/foco-launcher-v070-sot.md").readText()
        val xml = File("src/main/res/values/strings.xml").readText()

        assertTrue(camila.contains("Avisos · Sin filtro"))
        assertTrue(camila.contains("Pausar filtro de avisos"))
        assertTrue(camila.contains("Agrupar con…"))
        assertTrue(camila.contains("Sacar del grupo"))

        assertTrue(andres.contains("cancel in-scope"))
        assertTrue(andres.contains("notificationsPaused"))
        assertTrue(andres.contains("setInterruptionFilter"))

        assertTrue(index.contains("Pausar avisos is silence"))
        assertTrue(index.contains("Avisos · En pausa"))

        assertTrue(xml.contains("<string name=\"nls_pause\">Pausar avisos</string>"))
        assertTrue(xml.contains("<string name=\"nls_paused_chip\">Avisos · En pausa</string>"))
        assertTrue(xml.contains("<string name=\"group_add\">Agrupar con…</string>"))
        assertTrue(xml.contains("<string name=\"group_remove\">Sacar del grupo</string>"))
        assertFalse(xml.contains("Pausar filtro de avisos"))
        assertFalse(xml.contains("Sin filtro"))
        assertFalse(xml.contains("deja de filtrar"))
    }
}
