package com.foco.launcher.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.foco.launcher.FocoApp
import com.foco.launcher.R
import com.foco.launcher.core.FocoTheme
import com.foco.launcher.core.LaunchController
import com.foco.launcher.core.LauncherActivity
import com.foco.launcher.notification.NlsStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    onOpenSystemSettings = {
                        if (!LaunchController.openSystemSettings(this)) {
                            Toast.makeText(this, R.string.open_fail, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleAdd = vm::togglePending,
                    onConfirmAdd = vm::confirmAdd,
                    onRemove = vm::requestRemove,
                    onConfirmRemove = vm::confirmRemove,
                    onDismissRemove = vm::dismissRemove,
                    onMoveUp = { vm.move(it, -1) },
                    onMoveDown = { vm.move(it, 1) },
                    onToggleFilter = vm::requestEnableFilter,
                    onOpenNlsSettings = {
                        vm.markNlsSettingsOpened()
                        if (!NlsStatus.openListenerSettings(this)) vm.showNlsOpenFailed()
                    },
                    onOpenAppInfo = {
                        if (!LaunchController.openAppDetails(this)) {
                            Toast.makeText(this, R.string.open_fail, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSkipNls = vm::skipNlsOnboarding,
                    onSetNotifAllowed = vm::setNotifAllowed,
                    onNlsMessageShown = vm::clearNlsMessage,
                    onRefreshWork = vm::refreshWorkList,
                    onOpenWorkSettings = { LaunchController.openWorkProfileSettings(this) },
                    onWorkPaused = vm::setWorkSectionPaused,
                    onNotificationsPaused = vm::setNotificationsPaused,
                    onPhonePaused = vm::setPhonePaused,
                    onNamesOnly = vm::setNamesOnly,
                    onEditHome = {
                        startActivity(LauncherActivity.editIntent(this))
                        finish()
                    },
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
        NlsStatus.requestRebind(this)
        lifecycleScope.launch(Dispatchers.Default) {
            val resolved = LaunchController.workProfileSettingsResolves(this@SettingsActivity)
            withContext(Dispatchers.Main) { vm.setWorkLinkResolved(resolved) }
        }
        (application as FocoApp).registry.invalidate()
        (application as FocoApp).workCatalog.refresh()
    }

    companion object {
        const val EXTRA_DEST = "dest"
        const val DEST_MAIN = "main"
        const val DEST_EDIT = "edit"
        const val DEST_ADD = "add"
        const val DEST_AVISOS = "avisos"
        const val DEST_NLS_ONBOARDING = "nls"

        fun intent(context: Context, dest: String = DEST_MAIN): Intent {
            // Own task so a HOME redelivery cannot clear Foco settings off the launcher stack.
            return Intent(context, SettingsActivity::class.java)
                .putExtra(EXTRA_DEST, dest)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                )
        }

        fun intentDest(intent: Intent?): String {
            return intent?.getStringExtra(EXTRA_DEST) ?: DEST_MAIN
        }
    }
}
