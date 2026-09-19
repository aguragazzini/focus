package com.foco.launcher.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.notificationAllowlistDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "foco_notification_allowlist",
)

/**
 * Store separado de la whitelist del home.
 * Personal: solo estos packages avisan cuando [NotificationAllowlistPrefs.nlsFilterEnabled].
 */
class NotificationAllowlistStore(context: Context) {
    private val dataStore = context.applicationContext.notificationAllowlistDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val prefs: Flow<NotificationAllowlistPrefs> = dataStore.data.map { snapshot ->
        val raw = snapshot[KEY_JSON]
        if (raw.isNullOrBlank()) NotificationAllowlistPrefs()
        else runCatching { json.decodeFromString<NotificationAllowlistPrefs>(raw) }
            .getOrDefault(NotificationAllowlistPrefs())
    }

    suspend fun update(transform: (NotificationAllowlistPrefs) -> NotificationAllowlistPrefs) {
        dataStore.edit { store ->
            val current = store[KEY_JSON]?.let {
                runCatching { json.decodeFromString<NotificationAllowlistPrefs>(it) }.getOrNull()
            } ?: NotificationAllowlistPrefs()
            store[KEY_JSON] = json.encodeToString(transform(current))
        }
    }

    suspend fun setFilterEnabled(enabled: Boolean) {
        update { it.copy(nlsFilterEnabled = enabled) }
    }

    suspend fun add(packageName: String) {
        update { it.copy(packages = it.packages + packageName) }
    }

    suspend fun addAll(packageNames: Collection<String>) {
        if (packageNames.isEmpty()) return
        update { it.copy(packages = it.packages + packageNames) }
    }

    suspend fun remove(packageName: String) {
        update { it.copy(packages = it.packages - packageName) }
    }

    suspend fun setAllowed(packageName: String, allowed: Boolean) {
        update {
            val next = if (allowed) it.packages + packageName else it.packages - packageName
            it.copy(packages = next)
        }
    }

    /** Seed 1× desde la whitelist del home. No activa el filtro. */
    suspend fun seedFromHomeIfNeeded(homePackages: Collection<String>) {
        update { prefs ->
            if (prefs.seededFromHome) prefs
            else prefs.copy(
                packages = prefs.packages + homePackages,
                seededFromHome = true,
            )
        }
    }

    companion object {
        private val KEY_JSON = stringPreferencesKey("notification_allowlist_json")
    }
}
