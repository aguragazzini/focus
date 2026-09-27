package com.foco.launcher.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PIN lives in encrypted prefs, not the launcher DataStore document.
 * A failed keystore read leaves the gate off so Ajustes still opens.
 */
class PinStore(context: Context) {
    private val prefs: SharedPreferences? = open(context.applicationContext)
    private val enabledState = MutableStateFlow(readEnabled())

    val enabled: StateFlow<Boolean> = enabledState.asStateFlow()

    fun isEnabled(): Boolean = enabledState.value

    fun verify(pin: String): Boolean {
        val store = prefs ?: return false
        val salt = store.getString(KEY_SALT, null).orEmpty()
        val hash = store.getString(KEY_HASH, null).orEmpty()
        return PinRules.matches(salt, pin, hash)
    }

    /** First enable: both copies must be the same 4 digits. */
    fun enable(first: String, confirm: String): Boolean {
        if (!PinRules.isFourDigits(first) || first != confirm) return false
        return writePin(first, enabled = true)
    }

    fun change(current: String, next: String, confirm: String): Boolean {
        if (!verify(current)) return false
        if (!PinRules.isFourDigits(next) || next != confirm) return false
        return writePin(next, enabled = true)
    }

    fun disable(current: String): Boolean {
        if (!verify(current)) return false
        val store = prefs ?: return false
        store.edit().remove(KEY_SALT).remove(KEY_HASH).putBoolean(KEY_ENABLED, false).apply()
        enabledState.value = false
        return true
    }

    private fun writePin(pin: String, enabled: Boolean): Boolean {
        val store = prefs ?: return false
        val salt = PinRules.newSalt()
        store.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, PinRules.hash(salt, pin))
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        enabledState.value = enabled
        return true
    }

    private fun readEnabled(): Boolean {
        val store = prefs ?: return false
        return store.getBoolean(KEY_ENABLED, false) &&
            !store.getString(KEY_HASH, null).isNullOrEmpty()
    }

    private companion object {
        const val FILE = "foco_pin"
        const val KEY_ENABLED = "enabled"
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"

        fun open(context: Context): SharedPreferences? {
            return runCatching {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }.getOrNull()
        }
    }
}
