package com.foco.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foco.launcher.FocoApp
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.LauncherPrefs
import com.foco.launcher.registry.SuggestedApps
import com.foco.launcher.registry.WhitelistMutations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsDest { Main, Edit, Add }

data class SettingsUiState(
    val dest: SettingsDest = SettingsDest.Main,
    val versionName: String = "",
    val isDefaultHome: Boolean = false,
    val whitelist: List<LaunchableApp> = emptyList(),
    val catalog: List<LaunchableApp> = emptyList(),
    val pendingAdd: Set<String> = emptySet(),
    val settingsPackage: String? = null,
    val removeCandidate: LaunchableApp? = null,
    val removeIsLast: Boolean = false,
    val removeIsSettings: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FocoApp
    private val dest = MutableStateFlow(SettingsDest.Main)
    private val pendingAdd = MutableStateFlow<Set<String>>(emptySet())
    private val isDefault = MutableStateFlow(false)
    private val removeCandidate = MutableStateFlow<LaunchableApp?>(null)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dest,
                app.prefsStore.prefs,
                app.registry.launchables,
                pendingAdd,
                isDefault,
            ) { d, prefs, all, pending, defaultHome ->
                SettingsSnapshot(d, prefs, all, pending, defaultHome)
            }.combine(removeCandidate) { snap, remove ->
                val byPkg = snap.all.associateBy { it.packageName }
                val whitelist = snap.prefs.entries.sortedBy { it.order }.mapNotNull { byPkg[it.packageName] }
                val selected = snap.prefs.entries.map { it.packageName }.toSet()
                val catalog = snap.all.filterNot { it.packageName in selected }
                val settingsPkg = snap.all.find {
                    SuggestedApps.isSystemSettings(getApplication(), it.packageName)
                }?.packageName
                SettingsUiState(
                    dest = snap.dest,
                    versionName = com.foco.launcher.BuildConfig.VERSION_NAME,
                    isDefaultHome = snap.defaultHome,
                    whitelist = whitelist,
                    catalog = catalog,
                    pendingAdd = snap.pending,
                    settingsPackage = settingsPkg,
                    removeCandidate = remove,
                    removeIsLast = remove != null && whitelist.size <= 1,
                    removeIsSettings = remove != null && settingsPkg != null && remove.packageName == settingsPkg,
                )
            }.collect { _state.value = it }
        }
    }

    fun open(destName: String) {
        dest.value = when (destName) {
            SettingsActivity.DEST_EDIT -> SettingsDest.Edit
            SettingsActivity.DEST_ADD -> SettingsDest.Add
            else -> SettingsDest.Main
        }
        if (dest.value != SettingsDest.Add) pendingAdd.value = emptySet()
    }

    fun openEdit() {
        dest.value = SettingsDest.Edit
    }

    fun openAdd() {
        pendingAdd.value = emptySet()
        dest.value = SettingsDest.Add
    }

    fun onBack(): Boolean {
        return when (dest.value) {
            SettingsDest.Add -> {
                dest.value = SettingsDest.Edit
                pendingAdd.value = emptySet()
                true
            }
            SettingsDest.Edit -> {
                dest.value = SettingsDest.Main
                true
            }
            SettingsDest.Main -> false
        }
    }

    fun refreshDefault(value: Boolean) {
        isDefault.value = value
    }

    fun togglePending(packageName: String) {
        pendingAdd.update { current ->
            val next = current.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            next
        }
    }

    fun confirmAdd() {
        val toAdd = pendingAdd.value
        if (toAdd.isEmpty()) return
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val packages = toAdd.map { pkg ->
                    pkg to !SuggestedApps.isPhone(getApplication(), pkg)
                }
                prefs.copy(entries = WhitelistMutations.addAll(prefs.entries, packages))
            }
            pendingAdd.value = emptySet()
            dest.value = SettingsDest.Edit
        }
    }

    fun requestRemove(appItem: LaunchableApp) {
        removeCandidate.value = appItem
    }

    fun dismissRemove() {
        removeCandidate.value = null
    }

    fun confirmRemove() {
        val target = removeCandidate.value ?: return
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                prefs.copy(entries = WhitelistMutations.remove(prefs.entries, target.packageName))
            }
            removeCandidate.value = null
        }
    }

    fun move(packageName: String, delta: Int) {
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val ordered = prefs.entries.sortedBy { it.order }
                val index = ordered.indexOfFirst { it.packageName == packageName }
                if (index < 0) return@update prefs
                val to = (index + delta).coerceIn(0, ordered.lastIndex)
                prefs.copy(entries = WhitelistMutations.move(ordered, index, to))
            }
        }
    }

    private data class SettingsSnapshot(
        val dest: SettingsDest,
        val prefs: LauncherPrefs,
        val all: List<LaunchableApp>,
        val pending: Set<String>,
        val defaultHome: Boolean,
    )

    companion object {
        fun factory(app: FocoApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(app) as T
            }
        }
    }
}
