package com.foco.launcher.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import com.foco.launcher.security.PinGate
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private var requestEdit by mutableStateOf(false)
    private var homeToken by mutableIntStateOf(0)
    private var openPage by mutableStateOf<String?>(null)
    private var pinAction by mutableStateOf<(() -> Unit)?>(null)
    private var pinWrong by mutableStateOf(false)
    private val calendarPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestEdit = intent.getBooleanExtra(EXTRA_EDIT_HOME, false)
        openPage = readOpenPage(intent)
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
                } else Box(Modifier.fillMaxSize()) {
                    HomeScreen(
                    state = state,
                    onLaunch = { pkg ->
                        guardLaunch(pkg) {
                            val settingsPkg = SuggestedApps.settingsPackage(this@LauncherActivity)
                            val opened = if (LaunchpadRules.needsSettingsConfirm(pkg, settingsPkg)) {
                                LaunchController.openSystemSettings(this@LauncherActivity)
                            } else {
                                LaunchController.openApp(this@LauncherActivity, app.registry, pkg)
                            }
                            if (!opened) vm.showOpenFail()
                        }
                    },
                    onLaunchWork = { workApp: WorkApp ->
                        guardLaunch(workApp.packageName) {
                            if (!LaunchController.openWorkApp(this@LauncherActivity, workApp)) {
                                vm.showOpenFail()
                            }
                        }
                    },
                    onOpenFocoSettings = { openFocoSettings(SettingsActivity.DEST_MAIN) },
                    onOpenSystemSettings = {
                        if (!LaunchController.openSystemSettings(this@LauncherActivity)) vm.showOpenFail()
                    },
                    onOpenClock = { guardLaunch(null) { LaunchController.openClock(this@LauncherActivity) } },
                    onOpenCalendar = { guardLaunch(null) { LaunchController.openCalendar(this@LauncherActivity) } },
                    onRequestCalendar = { AgendaAccess.request(this@LauncherActivity, calendarPermission) },
                    onOpenAppSettings = {
                        if (!LaunchController.openAppDetails(this@LauncherActivity)) vm.showOpenFail()
                    },
                    onOpenAgendaEvent = { event ->
                        guardLaunch(null) {
                            val opened = LaunchController.openAgendaEvent(
                                this@LauncherActivity,
                                event.eventId,
                                event.beginMillis,
                                event.endMillis,
                                event.allDay,
                                event.side == AgendaSide.WORK,
                            )
                            if (!opened) vm.showOpenFail()
                        }
                    },
                    onOpenWorkCalendar = {
                        guardLaunch(null) {
                            if (!LaunchController.openWorkCalendar(this@LauncherActivity)) vm.showOpenFail()
                        }
                    },
                    onEditApps = { openFocoSettings(SettingsActivity.DEST_EDIT) },
                    onAddApps = { openFocoSettings(SettingsActivity.DEST_ADD) },
                    onChooseDefault = {
                        LaunchController.openHomePicker(this@LauncherActivity)
                    },
                    onOpenAvisos = {
                        vm.armSilenceOnConnect()
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
                        LaunchController.openWorkProfileSettings(this@LauncherActivity)
                    },
                    onQuietTap = vm::showQuietBlocked,
                    onMessageShown = vm::clearMessage,
                    requestEdit = requestEdit,
                    onEditRequestConsumed = {
                        requestEdit = false
                        intent.removeExtra(EXTRA_EDIT_HOME)
                    },
                    onCreatePage = vm::createHomePage,
                    onRenamePage = vm::renameHomePage,
                    onMovePage = vm::moveHomePage,
                    onHidePage = vm::setHomePageHidden,
                    onDeletePage = vm::deleteHomePage,
                    onAddBlock = vm::addHomeBlock,
                    onRemoveBlock = vm::removeHomeBlock,
                    onMoveBlock = vm::moveHomeBlock,
                    onUpdateBlock = vm::updateHomeBlock,
                    onPhonePaused = vm::setPhonePaused,
                    onSilence = vm::setSilence,
                    onArmSilence = vm::armSilenceOnConnect,
                    onPackagePaused = vm::setPackagePaused,
                    homeToken = homeToken,
                    openPage = openPage,
                    onOpenPageConsumed = {
                        openPage = null
                        intent.removeExtra(EXTRA_OPEN_PAGE)
                    },
                )
                    val pendingPin = pinAction
                    if (pendingPin != null) {
                        PinGate(
                            error = pinWrong,
                            onSubmit = { pin ->
                                val store = (application as FocoApp).pinStore
                                if (store.verify(pin)) {
                                    pinWrong = false
                                    pinAction = null
                                    pendingPin()
                                } else {
                                    pinWrong = true
                                }
                            },
                            onDismiss = {
                                pinWrong = false
                                pinAction = null
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val page = readOpenPage(intent)
        if (intent.getBooleanExtra(EXTRA_EDIT_HOME, false)) {
            requestEdit = true
        } else if (page != null) {
            openPage = page
        } else {
            homeToken++
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (intent.getBooleanExtra(EXTRA_EDIT_HOME, false)) return
        if (readOpenPage(intent) != null || openPage != null) return
        homeToken++
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.onResume()
    }

    companion object {
        const val EXTRA_EDIT_HOME = "edit_home"
        const val EXTRA_OPEN_PAGE = "open_page"

        fun readOpenPage(intent: Intent): String? {
            return intent.getStringExtra(EXTRA_OPEN_PAGE)?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        }

        fun pageIntent(context: android.content.Context, type: String): Intent {
            return Intent(context, LauncherActivity::class.java)
                .putExtra(EXTRA_OPEN_PAGE, type)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
        }

        fun editIntent(context: android.content.Context): Intent {
            return Intent(context, LauncherActivity::class.java)
                .putExtra(EXTRA_EDIT_HOME, true)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
        }
    }

    private fun guardLaunch(packageName: String?, action: () -> Unit) {
        if (packageName != null && packageName in vm.state.value.pausedPackages) {
            vm.showAppPaused()
            return
        }
        val store = (application as FocoApp).pinStore
        if (!store.isEnabled()) {
            action()
            return
        }
        pinWrong = false
        pinAction = action
    }

    private fun openFocoSettings(dest: String) {
        try {
            startActivity(SettingsActivity.intent(this, dest))
        } catch (_: Exception) {
            vm.showOpenFail()
        }
    }
}
