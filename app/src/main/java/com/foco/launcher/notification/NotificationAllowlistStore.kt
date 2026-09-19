package com.foco.launcher.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.notificationAllowlistDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "foco_notification_allowlist",
)

/**
 * Legacy store from the first 0.2 cut (separate package set).
 * Source of truth is now [com.foco.launcher.registry.LauncherPrefs]:
 * `notificationAllowlist = entries.filter { allowNotif }`.
 * Kept only to migrate nlsFilterEnabled + muted packages once.
 */
class NotificationAllowlistStore(context: Context) {
    private val dataStore = context.applicationContext.notificationAllowlistDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun consumeForMigration(): NotificationAllowlistPrefs? {
        var snapshot: NotificationAllowlistPrefs? = null
        dataStore.edit { store ->
            val current = store[KEY_JSON]?.let {
                runCatching { json.decodeFromString<NotificationAllowlistPrefs>(it) }.getOrNull()
            } ?: NotificationAllowlistPrefs()
            if (current.migratedToLauncherPrefs) {
                snapshot = null
            } else {
                snapshot = current
                store[KEY_JSON] = json.encodeToString(current.copy(migratedToLauncherPrefs = true))
            }
        }
        return snapshot
    }

    companion object {
        private val KEY_JSON = stringPreferencesKey("notification_allowlist_json")

        suspend fun migrateInto(prefsStore: com.foco.launcher.registry.PrefsStore, legacy: NotificationAllowlistStore) {
            val previous = legacy.consumeForMigration() ?: return
            if (!previous.nlsFilterEnabled && previous.packages.isEmpty() && !previous.seededFromHome) return
            prefsStore.update { prefs ->
                val nextEntries = if (previous.seededFromHome || previous.packages.isNotEmpty()) {
                    prefs.entries.map { entry ->
                        entry.copy(allowNotif = entry.packageName in previous.packages)
                    }
                } else {
                    prefs.entries
                }
                prefs.copy(
                    nlsFilterEnabled = prefs.nlsFilterEnabled || previous.nlsFilterEnabled,
                    entries = nextEntries,
                )
            }
        }
    }
}
