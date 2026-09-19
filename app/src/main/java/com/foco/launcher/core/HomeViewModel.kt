package com.foco.launcher.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foco.launcher.FocoApp
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.LauncherPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val ready: Boolean = false,
    val setupDone: Boolean = false,
    val isDefaultHome: Boolean = false,
    val apps: List<LaunchableApp> = emptyList(),
    val message: String? = null,
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
                app.prefsStore.prefs,
                app.registry.launchables,
                message,
                resumeTick,
            ) { prefs, _, msg, _ ->
                Triple(prefs, visible(prefs), msg)
            }.collect { (prefs, apps, msg) ->
                _state.value = HomeUiState(
                    ready = true,
                    setupDone = prefs.setupDone,
                    isDefaultHome = LaunchController.isDefaultHome(getApplication()),
                    apps = apps,
                    message = msg,
                )
            }
        }
    }

    fun onResume() {
        resumeTick.value += 1
        app.registry.refreshIfPackagesChanged()
    }

    fun showOpenFail() {
        message.value = getApplication<Application>().getString(com.foco.launcher.R.string.open_fail)
    }

    fun clearMessage() {
        message.value = null
    }

    private suspend fun visible(prefs: LauncherPrefs): List<LaunchableApp> {
        return app.registry.visibleApps(prefs)
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
