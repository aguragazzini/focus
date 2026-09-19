package com.foco.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foco.launcher.FocoApp
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.SuggestedApp
import com.foco.launcher.registry.SuggestedApps
import com.foco.launcher.registry.SuggestedKind
import com.foco.launcher.registry.WhitelistMutations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SetupStep { Welcome, PickApps, SetDefault }

data class SetupUiState(
    val step: SetupStep = SetupStep.Welcome,
    val suggested: List<SuggestedApp> = emptyList(),
    val others: List<LaunchableApp> = emptyList(),
    val selected: Set<String> = emptySet(),
    val settingsPackage: String? = null,
    val settingsSelected: Boolean = false,
    val canContinue: Boolean = false,
    val canSetDefault: Boolean = false,
    val isDefaultHome: Boolean = false,
    val finished: Boolean = false,
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FocoApp
    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { hydrate() }
    }

    fun onWelcomeContinue() {
        _state.update { it.copy(step = SetupStep.PickApps) }
    }

    fun backToApps() {
        _state.update { it.copy(step = SetupStep.PickApps) }
    }

    fun toggle(packageName: String) {
        _state.update { current ->
            val next = current.selected.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            current.copy(
                selected = next,
                canContinue = next.isNotEmpty(),
                settingsSelected = current.settingsPackage != null && current.settingsPackage in next,
                canSetDefault = current.settingsPackage != null && current.settingsPackage in next,
            )
        }
    }

    fun persistSelectionAndContinue() {
        val snapshot = _state.value
        if (snapshot.selected.isEmpty()) return
        viewModelScope.launch {
            val settingsPkg = snapshot.settingsPackage
            val packages = snapshot.selected.map { pkg ->
                val isPhone = SuggestedApps.isPhone(getApplication(), pkg)
                pkg to !isPhone
            }
            app.prefsStore.update { prefs ->
                prefs.copy(entries = WhitelistMutations.addAll(emptyList(), packages))
            }
            _state.update {
                it.copy(
                    step = SetupStep.SetDefault,
                    canSetDefault = settingsPkg != null && settingsPkg in snapshot.selected,
                    settingsSelected = settingsPkg != null && settingsPkg in snapshot.selected,
                )
            }
        }
    }

    fun finishSetup() {
        viewModelScope.launch {
            app.prefsStore.setSetupDone(true)
            _state.update { it.copy(finished = true) }
        }
    }

    fun onResume(isDefault: Boolean) {
        _state.update { it.copy(isDefaultHome = isDefault) }
        if (isDefault && itOnSetDefault()) {
            finishSetup()
        }
    }

    private fun itOnSetDefault(): Boolean = _state.value.step == SetupStep.SetDefault

    private suspend fun hydrate() {
        val prefs = app.prefsStore.prefs.first()
        val launchables = app.registry.allLaunchables()
        val suggested = SuggestedApps.resolve(getApplication())
        val suggestedPkgs = suggested.map { it.packageName }.toSet()
        val others = launchables.filterNot { it.packageName in suggestedPkgs }
        val settingsPkg = suggested.find { it.kind == SuggestedKind.SETTINGS }?.packageName
        val preselected = if (prefs.entries.isNotEmpty()) {
            prefs.entries.map { it.packageName }.toSet()
        } else {
            suggestedPkgs
        }
        val step = when {
            prefs.setupDone -> SetupStep.SetDefault
            prefs.entries.isNotEmpty() -> SetupStep.SetDefault
            else -> SetupStep.Welcome
        }
        if (prefs.setupDone) {
            _state.update { it.copy(finished = true) }
            return
        }
        _state.value = SetupUiState(
            step = if (step == SetupStep.SetDefault) SetupStep.SetDefault else SetupStep.Welcome,
            suggested = suggested,
            others = others,
            selected = preselected,
            settingsPackage = settingsPkg,
            settingsSelected = settingsPkg != null && settingsPkg in preselected,
            canContinue = preselected.isNotEmpty(),
            canSetDefault = settingsPkg != null && settingsPkg in preselected,
            isDefaultHome = false,
        )
        if (prefs.entries.isNotEmpty()) {
            _state.update { it.copy(step = SetupStep.SetDefault) }
        }
    }

    companion object {
        fun factory(app: FocoApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SetupViewModel(app) as T
            }
        }
    }
}
