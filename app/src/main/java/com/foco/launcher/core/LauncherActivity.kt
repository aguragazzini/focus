package com.foco.launcher.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.launcher.FocoApp
import com.foco.launcher.notification.NlsRecovery
import com.foco.launcher.registry.SuggestedApps
import com.foco.launcher.settings.SettingsActivity
import com.foco.launcher.settings.SetupActivity
import com.foco.launcher.work.WorkApp

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

                if (!state.prefsReady) {
                    HomeLoading()
                } else if (!state.setupDone) {
                    LaunchedEffect(Unit) {
                        startActivity(SetupActivity.intent(this@LauncherActivity))
                        if (!LaunchController.isDefaultHome(this@LauncherActivity)) {
                            finish()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    )
                } else if (!state.iconsReady) {
                    HomeLoading()
                } else HomeScreen(
                    state = state,
                    onLaunch = { pkg ->
                        val settingsPkg = SuggestedApps.settingsPackage(this)
                        val opened = if (LaunchpadRules.needsSettingsConfirm(pkg, settingsPkg)) {
                            LaunchController.openSystemSettings(this)
                        } else {
                            LaunchController.openApp(this, app.registry, pkg)
                        }
                        if (!opened) vm.showOpenFail()
                    },
                    onLaunchWork = { workApp: WorkApp ->
                        if (!LaunchController.openWorkApp(this, workApp)) {
                            vm.showOpenFail()
                        }
                    },
                    onOpenFocoSettings = { openFocoSettings(SettingsActivity.DEST_MAIN) },
                    onOpenSystemSettings = {
                        if (!LaunchController.openSystemSettings(this)) vm.showOpenFail()
                    },
                    onOpenClock = { LaunchController.openClock(this) },
                    onOpenCalendar = { LaunchController.openCalendar(this) },
                    onEditApps = { openFocoSettings(SettingsActivity.DEST_EDIT) },
                    onAddApps = { openFocoSettings(SettingsActivity.DEST_ADD) },
                    onChooseDefault = {
                        LaunchController.openHomePicker(this)
                    },
                    onOpenAvisos = {
                        val dest = if (vm.state.value.nlsAttention == NlsRecovery.Attention.Disconnected) {
                            SettingsActivity.DEST_AVISOS
                        } else {
                            SettingsActivity.DEST_NLS_ONBOARDING
                        }
                        openFocoSettings(dest)
                    },
                    onRefreshWork = vm::refreshWork,
                    onRemovePersonal = vm::removePersonal,
                    onCreateGroup = vm::createGroup,
                    onAddToGroup = vm::addToGroup,
                    onRenameGroup = vm::renameGroup,
                    onRemoveFromGroup = vm::removeFromGroup,
                    onDeleteGroup = vm::deleteGroup,
                    onDragOntoApp = vm::dragOntoApp,
                    onDragIntoFolder = vm::dragIntoFolder,
                    onDragEject = vm::dragEject,
                    onWorkPaused = vm::setWorkSectionPaused,
                    onNotificationsPaused = vm::setNotificationsPaused,
                    onCrossHint = vm::showCrossSectionHint,
                    onEnsureWorkIcon = vm::ensureWorkIcon,
                    onOpenWorkSettings = {
                        LaunchController.openWorkProfileSettings(this)
                    },
                    onQuietTap = vm::showQuietBlocked,
                    onMessageShown = vm::clearMessage,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.onResume()
    }

    private fun openFocoSettings(dest: String) {
        try {
            startActivity(SettingsActivity.intent(this, dest))
        } catch (_: Exception) {
            vm.showOpenFail()
        }
    }
}
