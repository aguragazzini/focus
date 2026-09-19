package com.foco.launcher.registry

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

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "foco_launcher_prefs",
)

class PrefsStore(context: Context) {
    private val dataStore = context.applicationContext.launcherDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val prefs: Flow<LauncherPrefs> = dataStore.data.map { snapshot ->
        val raw = snapshot[KEY_JSON]
        if (raw.isNullOrBlank()) LauncherPrefs()
        else runCatching { json.decodeFromString<LauncherPrefs>(raw) }.getOrDefault(LauncherPrefs())
    }

    suspend fun update(transform: (LauncherPrefs) -> LauncherPrefs) {
        dataStore.edit { store ->
            val current = store[KEY_JSON]?.let {
                runCatching { json.decodeFromString<LauncherPrefs>(it) }.getOrNull()
            } ?: LauncherPrefs()
            store[KEY_JSON] = json.encodeToString(transform(current))
        }
    }

    suspend fun setEntries(entries: List<WhitelistEntry>) {
        update { it.copy(entries = entries) }
    }

    suspend fun setSetupDone(done: Boolean = true) {
        update { it.copy(setupDone = done) }
    }

    suspend fun setNlsFilterEnabled(enabled: Boolean) {
        update { it.copy(nlsFilterEnabled = enabled) }
    }

    suspend fun setAllowNotif(packageName: String, allowed: Boolean) {
        update { prefs ->
            prefs.copy(
                entries = prefs.entries.map { entry ->
                    if (entry.packageName == packageName) entry.copy(allowNotif = allowed) else entry
                },
            )
        }
    }

    companion object {
        private val KEY_JSON = stringPreferencesKey("launcher_prefs_json")
    }
}
