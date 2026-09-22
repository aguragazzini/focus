package com.foco.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VisibleCopyTest {
    @Test
    fun userFacingStringsHaveNoInternOrEnglishOffCopy() {
        val xml = File("src/main/res/values/strings.xml").readText()
        assertFalse(xml.contains("Semana"))
        assertFalse(xml.contains("2a"))
        assertFalse(xml.contains("laburo"))
        assertFalse(xml.contains("huella"))
        assertFalse(Regex("\\boff\\b", RegexOption.IGNORE_CASE).containsMatchIn(xml))
        assertTrue(xml.contains("El filtro de avisos es del perfil personal. El perfil de trabajo no se toca."))
        assertFalse(xml.contains("setup_other_apps"))
        assertFalse(xml.contains("settings_week1_note"))
    }
}