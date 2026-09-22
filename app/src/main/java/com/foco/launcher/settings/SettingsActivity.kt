package com.foco.launcher.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.launcher.FocoApp
import com.foco.launcher.core.FocoTheme
import com.foco.launcher.core.LaunchController
import com.foco.launcher.notification.NlsStatus

class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels { SettingsViewModel.factory(application as FocoApp) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.open(intentDest(intent))
        setContent {
            FocoTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                SettingsHost(
                    state = state,
                    onBack = {
                        if (!vm.onBack()) finish()
                    },
                    onOpenEdit = vm::openEdit,
                    onOpenAdd = vm::openAdd,
                    onOpenAvisos = vm::openAvisos,
                    onChooseDefault = { LaunchController.openHomePicker(this) },
                    onOpenDefaultApps = { LaunchController.openDefaultAppsSettings(this) },
                    onOpenSystemSettings = { LaunchController.openSystemSettings(this) },
                    onToggleAdd = vm::togglePending,
                    onConfirmAdd = vm::confirmAdd,
                    onRemove = vm::requestRemove,
                    onConfirmRemove = vm::confirmRemove,
                    onDismissRemove = vm::dismissRemove,
                    onMoveUp = { vm.move(it, -1) },
                    onMoveDown = { vm.move(it, 1) },
                    onToggleFilter = vm::requestEnableFilter,
                    onOpenNlsSettings = { NlsStatus.openListenerSettings(this) },
                    onSkipNls = vm::skipNlsOnboarding,
                    onSetNotifAllowed = vm::setNotifAllowed,
                    onNlsMessageShown = vm::clearNlsMessage,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.open(intentDest(intent))
    }

    override fun onResume() {
        super.onResume()
        vm.refreshDefault(LaunchController.isDefaultHome(this))
        vm.refreshNls()
        (application as FocoApp).registry.refreshIfPackagesChanged()
        (application as FocoApp).registry.invalidate()
    }

    companion object {
        const val EXTRA_DEST = "dest"
        const val DEST_MAIN = "main"
        const val DEST_EDIT = "edit"
        const val DEST_ADD = "add"
        const val DEST_AVISOS = "avisos"
        const val DEST_NLS_ONBOARDING = "nls"

        fun intent(context: Context, dest: String = DEST_MAIN): Intent {
            return Intent(context, SettingsActivity::class.java).putExtra(EXTRA_DEST, dest)
        }

        fun intentDest(intent: Intent?): String {
            return intent?.getStringExtra(EXTRA_DEST) ?: DEST_MAIN
        }
    }
}
