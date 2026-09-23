package com.foco.launcher.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foco.launcher.FocoApp
import com.foco.launcher.notification.NlsStatus
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.LauncherPrefs
import com.foco.launcher.work.WorkApp
import com.foco.launcher.work.WorkCatalogRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val prefsReady: Boolean = false,
    val iconsReady: Boolean = false,
    val setupDone: Boolean = false,
    val isDefaultHome: Boolean = false,
    val apps: List<LaunchableApp> = emptyList(),
    val message: String? = null,
    val banner: HomeBanner = HomeBanner.None,
    val workProfile: Boolean = false,
    val workApps: List<WorkApp> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FocoApp
    private val message = MutableStateFlow<String?>(null)
    private val resumeTick = MutableStateFlow(0)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                app.startupReady,
                app.prefsStore.prefs,
                app.registry.launchables,
                app.registry.loaded,
            ) { startupReady, prefs, all, iconsLoaded ->
                StartupSnap(startupReady, prefs, all, iconsLoaded)
            }.combine(message) { snap, msg ->
                snap to msg
            }.combine(resumeTick) { pair, _ ->
                val (snap, msg) = pair
                if (!snap.startupReady) {
                    HomeUiState(message = msg)
                } else {
                    val isDefault = LaunchController.isDefaultHome(getApplication())
                    val nlsNeedsGrant = snap.prefs.nlsFilterEnabled &&
                        !NlsStatus.isGranted(getApplication())
                    HomeUiState(
                        prefsReady = true,
                        iconsReady = snap.iconsLoaded,
                        setupDone = snap.prefs.setupDone,
                        isDefaultHome = isDefault,
                        apps = visible(snap.prefs, snap.apps),
                        message = msg,
                        banner = selectHomeBanner(
                            setupDone = snap.prefs.setupDone,
                            isDefaultHome = isDefault,
                            nlsNeedsGrant = nlsNeedsGrant,
                        ),
                    )
                }
            }.combine(app.workCatalog.state) { ui, work ->
                val showWork = work.loaded && WorkCatalogRules.showSection(work.hasWorkProfile)
                ui.copy(
                    workProfile = showWork,
                    workApps = if (showWork) work.apps else emptyList(),
                )
            }.collect { _state.value = it }
        }
    }

    fun onResume() {
        resumeTick.value += 1
        app.registry.refreshIfPackagesChanged()
        app.workCatalog.refresh()
    }

    fun showOpenFail() {
        message.value = getApplication<Application>().getString(com.foco.launcher.R.string.open_fail)
    }

    fun clearMessage() {
        message.value = null
    }

    private data class StartupSnap(
        val startupReady: Boolean,
        val prefs: LauncherPrefs,
        val apps: List<LaunchableApp>,
        val iconsLoaded: Boolean,
    )

    private fun visible(prefs: LauncherPrefs, all: List<LaunchableApp>): List<LaunchableApp> {
        if (all.isEmpty()) return emptyList()
        val byPkg = all.associateBy { it.packageName }
        return prefs.entries
            .sortedBy { it.order }
            .mapNotNull { byPkg[it.packageName] }
    }

    companion object {
        fun factory(app: FocoApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(app) as T
            }
        }
    }
}
