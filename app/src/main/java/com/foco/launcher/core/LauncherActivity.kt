package com.foco.launcher.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.launcher.FocoApp
import com.foco.launcher.settings.SettingsActivity
import com.foco.launcher.settings.SetupActivity

class LauncherActivity : ComponentActivity() {
    private lateinit var vm: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FocoApp
        vm = ViewModelProvider(this, HomeViewModel.factory(app))[HomeViewModel::class.java]
        setContent {
            FocoTheme {
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(state.setupDone, state.ready) {
                    if (state.ready && !state.setupDone) {
                        startActivity(SetupActivity.intent(this@LauncherActivity))
                        if (!LaunchController.isDefaultHome(this@LauncherActivity)) {
                            finish()
                        }
                    }
                }

                HomeScreen(
                    state = state,
                    onLaunch = { pkg ->
                        if (!LaunchController.openApp(this, app.registry, pkg)) {
                            vm.showOpenFail()
                        }
                    },
                    onOpenFocoSettings = {
                        startActivity(SettingsActivity.intent(this))
                    },
                    onOpenSystemSettings = {
                        LaunchController.openSystemSettings(this)
                    },
                    onEditApps = {
                        startActivity(SettingsActivity.intent(this, SettingsActivity.DEST_EDIT))
                    },
                    onAddApps = {
                        startActivity(SettingsActivity.intent(this, SettingsActivity.DEST_ADD))
                    },
                    onChooseDefault = {
                        LaunchController.openHomePicker(this)
                    },
                    onMessageShown = vm::clearMessage,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.onResume()
    }
}
