package com.foco.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ColdStartGuardTest {
    @Test
    fun applicationDoesNotMigrateOnTheMainThread() {
        val src = File("src/main/java/com/foco/launcher/FocoApp.kt").readText()
        assertFalse(src.contains("runBlocking"))
        assertTrue(src.contains("startupReady"))
    }

    @Test
    fun manifestKeepsTaskAndWarmsOnBoot() {
        val xml = File("src/main/AndroidManifest.xml").readText()
        assertTrue(xml.contains("android:clearTaskOnLaunch=\"false\""))
        assertFalse(xml.contains("android:clearTaskOnLaunch=\"true\""))
        assertTrue(xml.contains("android.intent.action.BOOT_COMPLETED"))
        assertTrue(xml.contains("android:exported=\"true\""))
        assertTrue(xml.contains("RECEIVE_BOOT_COMPLETED"))
    }
}
