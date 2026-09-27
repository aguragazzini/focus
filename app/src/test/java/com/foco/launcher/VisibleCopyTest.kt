package com.foco.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VisibleCopyTest {
    @Test
    fun userFacingStringsHaveNoInternOrEnglishOffCopy() {
        val xml = File("src/main/res/values/strings.xml").readText()
        assertTrue(xml.contains("<string name=\"santoral_vetus\">Vetus</string>"))
        assertTrue(xml.contains("<string name=\"santoral_novus\">Novus</string>"))
        assertFalse(xml.contains("Betus"))
        assertFalse(xml.contains("Nobus"))
        assertFalse(xml.contains(">1962<"))
        assertFalse(xml.contains("Semana"))
        assertFalse(xml.contains("2a"))
        assertFalse(xml.contains("laburo"))
        assertFalse(xml.contains("huella"))
        // Camila v0.5 status token. The word is product copy, not an intern flag.
        val scrubbed = xml
            .replace("<string name=\"strip_filter_off\">Off</string>", "")
            .replace("<string name=\"strip_bio_off\">Off</string>", "")
        assertTrue(xml.contains("<string name=\"strip_filter_off\">Off</string>"))
        assertFalse(Regex("\\boff\\b", RegexOption.IGNORE_CASE).containsMatchIn(scrubbed))
        assertTrue(xml.contains("Foco puede ocultar Trabajo y silenciar avisos. No cambia el perfil del sistema."))
        assertTrue(xml.contains("Trabajo · En pausa"))
        assertTrue(xml.contains("Avisos · En pausa"))
        assertTrue(xml.contains("Mostrar solo nombres"))
        assertTrue(xml.contains("No es el No molestar del teléfono."))
        assertFalse(xml.contains("pausar perfil"))
        assertFalse(xml.contains("Sin filtro"))
        assertFalse(xml.contains("setup_other_apps"))
        assertFalse(xml.contains("settings_week1_note"))
    }
}