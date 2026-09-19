package com.foco.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foco.launcher.FocoApp
import com.foco.launcher.notification.NlsStatus
import com.foco.launcher.notification.UserProfileHelper
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.LauncherPrefs
import com.foco.launcher.registry.SuggestedApps
import com.foco.launcher.registry.WhitelistMutations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsDest { Main, Edit, Add, Avisos, AvisosAdd, NlsOnboarding }

data class AvisosRow(
    val app: LaunchableApp,
    val allowed: Boolean,
    val onHome: Boolean,
)

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
    val nlsFilterEnabled: Boolean = false,
    val nlsGranted: Boolean = false,
    val nlsActive: Boolean = false,
    val hasWorkProfile: Boolean = false,
    val avisosRows: List<AvisosRow> = emptyList(),
    val avisosCatalog: List<LaunchableApp> = emptyList(),
    val pendingAvisosAdd: Set<String> = emptySet(),
    val nlsMessage: String? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FocoApp
    private val dest = MutableStateFlow(SettingsDest.Main)
    private val pendingAdd = MutableStateFlow<Set<String>>(emptySet())
    private val pendingAvisosAdd = MutableStateFlow<Set<String>>(emptySet())
    private val isDefault = MutableStateFlow(false)
    private val removeCandidate = MutableStateFlow<LaunchableApp?>(null)
    private val nlsTick = MutableStateFlow(0)
    private val nlsMessage = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dest,
                app.prefsStore.prefs,
                app.registry.launchables,
                app.notificationAllowlistStore.prefs,
                nlsTick,
            ) { d, prefs, all, notif, _ ->
                CoreSnap(d, prefs, all, notif.nlsFilterEnabled, notif.packages)
            }.combine(pendingAdd) { core, pending -> core to pending }
                .combine(pendingAvisosAdd) { pair, avisosPending -> Triple(pair.first, pair.second, avisosPending) }
                .combine(removeCandidate) { triple, remove ->
                    val (core, pending, avisosPending) = triple
                    buildState(core, pending, avisosPending, remove, nlsMessage.value)
                }
                .combine(nlsMessage) { ui, msg -> ui.copy(nlsMessage = msg) }
                .combine(isDefault) { ui, defaultHome -> ui.copy(isDefaultHome = defaultHome) }
                .collect { _state.value = it }
        }
    }

    fun open(destName: String) {
        dest.value = when (destName) {
            SettingsActivity.DEST_EDIT -> SettingsDest.Edit
            SettingsActivity.DEST_ADD -> SettingsDest.Add
            SettingsActivity.DEST_AVISOS -> SettingsDest.Avisos
            SettingsActivity.DEST_NLS_ONBOARDING -> SettingsDest.NlsOnboarding
            else -> SettingsDest.Main
        }
        if (dest.value != SettingsDest.Add) pendingAdd.value = emptySet()
        if (dest.value != SettingsDest.AvisosAdd) pendingAvisosAdd.value = emptySet()
    }

    fun openEdit() {
        dest.value = SettingsDest.Edit
    }

    fun openAdd() {
        pendingAdd.value = emptySet()
        dest.value = SettingsDest.Add
    }

    fun openAvisos() {
        dest.value = SettingsDest.Avisos
        nlsTick.value += 1
    }

    fun openAvisosAdd() {
        pendingAvisosAdd.value = emptySet()
        dest.value = SettingsDest.AvisosAdd
    }

    fun onBack(): Boolean {
        return when (dest.value) {
            SettingsDest.Add -> {
                dest.value = SettingsDest.Edit
                pendingAdd.value = emptySet()
                true
            }
            SettingsDest.AvisosAdd -> {
                dest.value = SettingsDest.Avisos
                pendingAvisosAdd.value = emptySet()
                true
            }
            SettingsDest.NlsOnboarding -> {
                dest.value = SettingsDest.Avisos
                true
            }
            SettingsDest.Avisos, SettingsDest.Edit -> {
                dest.value = SettingsDest.Main
                true
            }
            SettingsDest.Main -> false
        }
    }

    fun refreshDefault(value: Boolean) {
        isDefault.value = value
    }

    fun refreshNls() {
        nlsTick.value += 1
        viewModelScope.launch {
            if (dest.value == SettingsDest.NlsOnboarding && NlsStatus.isGranted(getApplication())) {
                enableFilterAfterGrant()
            }
        }
    }

    fun requestEnableFilter(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                app.notificationAllowlistStore.setFilterEnabled(false)
                return@launch
            }
            if (NlsStatus.isGranted(getApplication())) {
                enableFilterAfterGrant()
            } else {
                dest.value = SettingsDest.NlsOnboarding
            }
        }
    }

    fun skipNlsOnboarding() {
        viewModelScope.launch {
            app.notificationAllowlistStore.setFilterEnabled(false)
            dest.value = SettingsDest.Avisos
        }
    }

    fun clearNlsMessage() {
        nlsMessage.value = null
    }

    fun setNotifAllowed(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            app.notificationAllowlistStore.setAllowed(packageName, allowed)
        }
    }

    fun togglePending(packageName: String) {
        pendingAdd.update { current ->
            val next = current.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            next
        }
    }

    fun togglePendingAvisos(packageName: String) {
        pendingAvisosAdd.update { current ->
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
            app.notificationAllowlistStore.addAll(toAdd)
            pendingAdd.value = emptySet()
            dest.value = SettingsDest.Edit
        }
    }

    fun confirmAvisosAdd() {
        val toAdd = pendingAvisosAdd.value
        if (toAdd.isEmpty()) return
        viewModelScope.launch {
            app.notificationAllowlistStore.addAll(toAdd)
            pendingAvisosAdd.value = emptySet()
            dest.value = SettingsDest.Avisos
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

    private suspend fun enableFilterAfterGrant() {
        val homePkgs = app.prefsStore.prefs.first().entries.map { it.packageName }
        app.notificationAllowlistStore.seedFromHomeIfNeeded(homePkgs)
        app.notificationAllowlistStore.setFilterEnabled(true)
        dest.value = SettingsDest.Avisos
        nlsMessage.value = getApplication<Application>().getString(com.foco.launcher.R.string.nls_done)
        nlsTick.value += 1
    }

    private fun buildState(
        core: CoreSnap,
        pending: Set<String>,
        avisosPending: Set<String>,
        remove: LaunchableApp?,
        message: String?,
    ): SettingsUiState {
        val byPkg = core.all.associateBy { it.packageName }
        val whitelist = core.prefs.entries.sortedBy { it.order }.mapNotNull { byPkg[it.packageName] }
        val selected = core.prefs.entries.map { it.packageName }.toSet()
        val catalog = core.all.filterNot { it.packageName in selected }
        val settingsPkg = core.all.find {
            SuggestedApps.isSystemSettings(getApplication(), it.packageName)
        }?.packageName
        val granted = NlsStatus.isGranted(getApplication())
        val homeSet = selected
        val extraAllowed = core.allowlist.filter { it !in homeSet }.mapNotNull { byPkg[it] }
        val rows = buildList {
            for (appItem in whitelist) {
                add(AvisosRow(appItem, appItem.packageName in core.allowlist, onHome = true))
            }
            for (appItem in extraAllowed) {
                add(AvisosRow(appItem, allowed = true, onHome = false))
            }
        }
        val avisosCatalog = core.all.filterNot { it.packageName in core.allowlist }
        return SettingsUiState(
            dest = core.dest,
            versionName = com.foco.launcher.BuildConfig.VERSION_NAME,
            isDefaultHome = false,
            whitelist = whitelist,
            catalog = catalog,
            pendingAdd = pending,
            settingsPackage = settingsPkg,
            removeCandidate = remove,
            removeIsLast = remove != null && whitelist.size <= 1,
            removeIsSettings = remove != null && settingsPkg != null && remove.packageName == settingsPkg,
            nlsFilterEnabled = core.nlsFilterEnabled,
            nlsGranted = granted,
            nlsActive = NlsStatus.isFilterActive(getApplication(), core.nlsFilterEnabled),
            hasWorkProfile = UserProfileHelper.hasWorkProfile(getApplication()),
            avisosRows = rows,
            avisosCatalog = avisosCatalog,
            pendingAvisosAdd = avisosPending,
            nlsMessage = message,
        )
    }

    private data class CoreSnap(
        val dest: SettingsDest,
        val prefs: LauncherPrefs,
        val all: List<LaunchableApp>,
        val nlsFilterEnabled: Boolean,
        val allowlist: Set<String>,
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
