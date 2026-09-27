package com.foco.launcher.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Four-digit PIN checks. The stored value is a salted hash, never the digits.
 */
object PinRules {
    private val random = SecureRandom()

    fun isFourDigits(raw: String): Boolean = raw.length == 4 && raw.all { it in '0'..'9' }

    fun newSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(salt: String, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        digest.update(0)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun matches(salt: String, pin: String, expectedHash: String): Boolean {
        if (!isFourDigits(pin) || salt.isEmpty() || expectedHash.isEmpty()) return false
        return hash(salt, pin) == expectedHash
    }
}
