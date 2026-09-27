package com.foco.launcher.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinRulesTest {
    @Test
    fun onlyFourDigitsCount() {
        assertTrue(PinRules.isFourDigits("1234"))
        assertFalse(PinRules.isFourDigits("123"))
        assertFalse(PinRules.isFourDigits("12345"))
        assertFalse(PinRules.isFourDigits("12a4"))
        assertFalse(PinRules.isFourDigits(""))
    }

    @Test
    fun hashDependsOnSaltAndDoesNotEchoThePin() {
        val salt = PinRules.newSalt()
        val hash = PinRules.hash(salt, "1234")
        assertNotEquals("1234", hash)
        assertTrue(PinRules.matches(salt, "1234", hash))
        assertFalse(PinRules.matches(salt, "9999", hash))
        assertFalse(PinRules.matches(PinRules.newSalt(), "1234", hash))
        assertEquals(hash, PinRules.hash(salt, "1234"))
    }
}
