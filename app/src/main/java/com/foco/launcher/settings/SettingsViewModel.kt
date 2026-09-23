package com.foco.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foco.launcher.FocoApp
import com.foco.launcher.notification.FocoNotificationListener
import com.foco.launcher.notification.NlsRecovery
import com.foco.launcher.notification.NlsStatus
import com.foco.launcher.notification.UserProfileHelper
import com.foco.launcher.core.LaunchpadRules
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.LauncherPrefs
import com.foco.launcher.registry.SuggestedApps
import com.foco.launcher.registry.WhitelistMutations
import com.foco.launcher.security.BiometricGate
import com.foco.launcher.work.WorkCatalogRules
import com.foco.launcher.work.snapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsDest { Main, Edit, Add, Avisos, NlsOnboarding }

data class AvisosRow(
    val app: LaunchableApp,
    val allowed: Boolean,
    val onHome: Boolean = true,
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
    val nlsConnected: Boolean = false,
    val nlsActive: Boolean = false,
    val nlsShowRestrictedReturn: Boolean = false,
    val hasWorkProfile: Boolean = false,
    val avisosRows: List<AvisosRow> = emptyList(),
    val nlsMessage: String? = null,
    val workLoaded: Boolean = false,
    val workProfile: Boolean = false,
    val workQuiet: Boolean? = null,
    val workFailed: Boolean = false,
    val workCount: Int = 0,
    val workRefreshing: Boolean = false,
    val workLink: Boolean = false,
    val workNote: String? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FocoApp
    private val dest = MutableStateFlow(SettingsDest.Main)
    private val pendingAdd = MutableStateFlow<Set<String>>(emptySet())
    private val isDefault = MutableStateFlow(false)
    private val removeCandidate = MutableStateFlow<LaunchableApp?>(null)
    private val nlsTick = MutableStateFlow(0)
    private val nlsMessage = MutableStateFlow<String?>(null)
    private val awaitingGrantReturn = MutableStateFlow(false)
    private val showRestrictedReturn = MutableStateFlow(false)
    private val workLink = MutableStateFlow(false)
    private val workNote = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dest,
                app.prefsStore.prefs,
                app.registry.launchables,
                nlsTick,
            ) { d, prefs, all, _ ->
                CoreSnap(d, prefs, all)
            }.combine(pendingAdd) { core, pending -> core to pending }
                .combine(removeCandidate) { pair, remove ->
                    val (core, pending) = pair
                    buildState(core, pending, remove, nlsMessage.value)
                }
                .combine(nlsMessage) { ui, msg -> ui.copy(nlsMessage = msg) }
                .combine(isDefault) { ui, defaultHome -> ui.copy(isDefaultHome = defaultHome) }
                .combine(app.workCatalog.state) { ui, work ->
                    ui.copy(
                        workLoaded = work.loaded,
                        workProfile = work.hasWorkProfile,
                        workQuiet = work.quietEnabled,
                        workFailed = work.loadFailed,
                        workCount = work.apps.size,
                        workRefreshing = work.refreshing,
                    )
                }
                .combine(workLink) { ui, link -> ui.copy(workLink = link) }
                .combine(workNote) { ui, note -> ui.copy(workNote = note) }
                .combine(FocoNotificationListener.connected) { ui, connected ->
                    ui.copy(
                        nlsConnected = connected,
                        nlsActive = NlsRecovery.filterIsActive(ui.nlsGranted, ui.nlsFilterEnabled, connected),
                    )
                }
                .combine(showRestrictedReturn) { ui, show -> ui.copy(nlsShowRestrictedReturn = show) }
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

    fun onBack(): Boolean {
        return when (dest.value) {
            SettingsDest.Add -> {
                dest.value = SettingsDest.Edit
                pendingAdd.value = emptySet()
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

    fun setWorkLinkResolved(resolved: Boolean) {
        workLink.value = resolved
    }

    fun refreshWorkList() {
        val ctx = getApplication<Application>()
        workNote.value = null
        app.workCatalog.refresh(manual = true) { before, after ->
            val changed = WorkCatalogRules.refreshChanged(before.snapshot(), after.snapshot())
            workNote.value = ctx.getString(
                if (changed) com.foco.launcher.R.string.work_refreshed else com.foco.launcher.R.string.work_refresh_noop,
            )
        }
    }

    fun refreshNls() {
        nlsTick.value += 1
        viewModelScope.launch {
            val granted = NlsStatus.isGranted(getApplication())
            if (dest.value == SettingsDest.NlsOnboarding && granted) {
                awaitingGrantReturn.value = false
                showRestrictedReturn.value = false
                enableFilterAfterGrant()
                return@launch
            }
            if (awaitingGrantReturn.value && !granted) {
                showRestrictedReturn.value = true
            }
        }
    }

    /** User left for the system listener screen. If they come back still ungranted, explain restricted settings. */
    fun markNlsSettingsOpened() {
        awaitingGrantReturn.value = true
    }

    fun requestEnableFilter(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                app.prefsStore.setNlsFilterEnabled(false)
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
            app.prefsStore.setNlsFilterEnabled(false)
            nlsMessage.value = null
            awaitingGrantReturn.value = false
            dest.value = SettingsDest.Avisos
        }
    }

    fun clearNlsMessage() {
        nlsMessage.value = null
    }

    fun showNlsOpenFailed() {
        nlsMessage.value = getApplication<Application>().getString(com.foco.launcher.R.string.nls_open_fail)
    }

    fun setNotifAllowed(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            app.prefsStore.setAllowNotif(packageName, allowed)
        }
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
                    val isPhone = SuggestedApps.isPhone(getApplication(), pkg)
                    pkg to BiometricGate.defaultBioEnabled(isPhone)
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

    private suspend fun enableFilterAfterGrant() {
        app.prefsStore.setNlsFilterEnabled(true)
        dest.value = SettingsDest.Avisos
        nlsMessage.value = getApplication<Application>().getString(com.foco.launcher.R.string.nls_done)
        nlsTick.value += 1
    }

    private fun buildState(
        core: CoreSnap,
        pending: Set<String>,
        remove: LaunchableApp?,
        message: String?,
    ): SettingsUiState {
        val byPkg = core.all.associateBy { it.packageName }
        val whitelist = core.prefs.entries.sortedBy { it.order }.mapNotNull { byPkg[it.packageName] }
        val selected = core.prefs.entries.map { it.packageName }.toSet()
        // Personal launcher activities only. Work-profile activities stay in WorkCatalog.
        val catalog = core.all.filterNot { it.packageName in selected }
        val settingsPkg = SuggestedApps.settingsPackage(getApplication())
        val granted = NlsStatus.isGranted(getApplication())
        val allowByPkg = core.prefs.entries.associate { it.packageName to it.allowNotif }
        val rows = whitelist.map { appItem ->
            AvisosRow(
                app = appItem,
                allowed = allowByPkg[appItem.packageName] != false,
                onHome = true,
            )
        }
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
            removeIsSettings = remove != null &&
                LaunchpadRules.needsSettingsConfirm(remove.packageName, settingsPkg),
            nlsFilterEnabled = core.prefs.nlsFilterEnabled,
            nlsGranted = granted,
            nlsConnected = FocoNotificationListener.isConnected(),
            nlsActive = NlsStatus.isFilterActive(
                getApplication(),
                core.prefs.nlsFilterEnabled,
                FocoNotificationListener.isConnected(),
            ),
            hasWorkProfile = UserProfileHelper.hasWorkProfile(getApplication()),
            avisosRows = rows,
            nlsMessage = message,
        )
    }

    private data class CoreSnap(
        val dest: SettingsDest,
        val prefs: LauncherPrefs,
        val all: List<LaunchableApp>,
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
