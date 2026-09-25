package com.foco.launcher.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foco.launcher.FocoApp
import com.foco.launcher.R
import com.foco.launcher.notification.FocoNotificationListener
import com.foco.launcher.notification.NlsRecovery
import com.foco.launcher.notification.NlsStatus
import com.foco.launcher.registry.AppGroup
import com.foco.launcher.registry.GroupMutations
import com.foco.launcher.registry.GroupSection
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.LauncherPrefs
import com.foco.launcher.registry.WhitelistMutations
import com.foco.launcher.registry.withEntries
import com.foco.launcher.security.BiometricGate
import com.foco.launcher.work.WorkApp
import com.foco.launcher.work.WorkCatalogRules
import com.foco.launcher.work.WorkHomeState
import com.foco.launcher.work.WorkSectionKind
import com.foco.launcher.work.snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val prefsReady: Boolean = false,
    val iconsReady: Boolean = false,
    val setupDone: Boolean = false,
    val isDefaultHome: Boolean = false,
    val apps: List<LaunchableApp> = emptyList(),
    val groups: List<AppGroup> = emptyList(),
    val message: String? = null,
    val banner: HomeBanner = HomeBanner.None,
    val nlsAttention: NlsRecovery.Attention = NlsRecovery.Attention.None,
    val filterActive: Boolean = false,
    val workPresence: WorkPresence = WorkPresence.Unknown,
    val workKind: WorkSectionKind = WorkSectionKind.Hidden,
    val workApps: List<WorkApp> = emptyList(),
    val workIcons: Map<String, android.graphics.Bitmap> = emptyMap(),
    val workIconEpoch: Long = 0L,
    val workRefreshing: Boolean = false,
    val workLink: Boolean = false,
    val showBio: Boolean = false,
    val bioOnCount: Int = 0,
    val hasWorkProfile: Boolean = false,
    val workSectionPaused: Boolean = false,
    val notificationsPaused: Boolean = false,
    val namesOnly: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FocoApp
    private val message = MutableStateFlow<String?>(null)
    private val resumeTick = MutableStateFlow(0)
    private val workLinkResolved = MutableStateFlow(false)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refreshDeviceFacts()
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
                pair
            }.combine(workLinkResolved) { pair, link ->
                pair to link
            }.combine(app.workCatalog.state) { packed, work ->
                packed to work
            }.combine(FocoNotificationListener.connected) { packedWork, connected ->
                val (packed, work) = packedWork
                val (pair, link) = packed
                val (snap, msg) = pair
                buildUi(snap, msg, link, work, connected)
            }.combine(app.workCatalog.icons) { ui, icons ->
                ui.copy(workIcons = icons)
            }.combine(app.workCatalog.iconEpoch) { ui, epoch ->
                ui.copy(workIconEpoch = epoch)
            }.collect { _state.value = it }
        }
    }

    fun onResume() {
        resumeTick.value += 1
        refreshDeviceFacts()
        NlsStatus.requestRebind(getApplication())
        app.registry.refreshIfPackagesChanged()
        app.workCatalog.refresh()
    }

    fun refreshWork() {
        val ctx = getApplication<Application>()
        app.workCatalog.refresh(manual = true) { before, after ->
            val changed = WorkCatalogRules.refreshChanged(before.snapshot(), after.snapshot())
            message.value = ctx.getString(
                if (changed) R.string.work_refreshed else R.string.work_refresh_noop,
            )
        }
    }

    fun showOpenFail() {
        message.value = getApplication<Application>().getString(R.string.open_fail)
    }

    fun showQuietBlocked() {
        message.value = getApplication<Application>().getString(R.string.work_quiet_tap)
    }

    fun clearMessage() {
        message.value = null
    }

    fun removePersonal(packageName: String) {
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                prefs.withEntries(WhitelistMutations.remove(prefs.entries, packageName))
            }
        }
    }

    fun createGroup(section: GroupSection, rawName: String, memberId: String) {
        val id = UUID.randomUUID().toString()
        val workAllowed = workMemberIds()
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val allowed = allowedIds(prefs, section, workAllowed)
                val next = GroupMutations.create(prefs.groups, section, rawName, memberId, id, allowed)
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun addToGroup(groupId: String, memberId: String) {
        val workAllowed = workMemberIds()
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val section = prefs.groups.find { it.id == groupId }?.section ?: return@update prefs
                val allowed = allowedIds(prefs, section, workAllowed)
                val next = GroupMutations.addMember(prefs.groups, groupId, memberId, allowed)
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun renameGroup(groupId: String, rawName: String) {
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val next = GroupMutations.rename(prefs.groups, groupId, rawName)
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun removeFromGroup(groupId: String, memberId: String) {
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val next = GroupMutations.removeMember(prefs.groups, groupId, memberId)
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val next = GroupMutations.delete(prefs.groups, groupId)
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun dragOntoApp(
        section: GroupSection,
        draggedId: String,
        draggedLabel: String,
        targetId: String,
        targetLabel: String,
    ) {
        val id = UUID.randomUUID().toString()
        val workAllowed = workMemberIds()
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val allowed = allowedIds(prefs, section, workAllowed)
                val next = GroupMutations.placeOnApp(
                    groups = prefs.groups,
                    section = section,
                    draggedId = draggedId,
                    targetId = targetId,
                    newGroupId = id,
                    labelDragged = draggedLabel,
                    labelTarget = targetLabel,
                    allowedIds = allowed,
                )
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun dragIntoFolder(groupId: String, memberId: String) {
        val workAllowed = workMemberIds()
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val section = prefs.groups.find { it.id == groupId }?.section ?: return@update prefs
                val allowed = allowedIds(prefs, section, workAllowed)
                val next = GroupMutations.dragIntoFolder(prefs.groups, groupId, memberId, allowed)
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun dragEject(groupId: String, memberId: String) {
        viewModelScope.launch {
            app.prefsStore.update { prefs ->
                val next = GroupMutations.eject(prefs.groups, groupId, memberId)
                if (next == prefs.groups) prefs else prefs.copy(groups = next)
            }
        }
    }

    fun setWorkSectionPaused(paused: Boolean) {
        viewModelScope.launch {
            app.prefsStore.setWorkSectionPaused(paused)
            if (!paused) app.workCatalog.refresh()
        }
    }

    fun setNotificationsPaused(paused: Boolean) {
        viewModelScope.launch {
            app.prefsStore.setNotificationsPaused(paused)
        }
    }

    fun showCrossSectionHint() {
        message.value = getApplication<Application>().getString(R.string.drag_cross_hint)
    }

    private fun workMemberIds(): Set<String> {
        val work = app.workCatalog.state.value
        if (!work.loaded || work.loadFailed || !work.hasWorkProfile) return emptySet()
        return work.apps.map { it.key }.toSet()
    }

    private fun allowedIds(
        prefs: LauncherPrefs,
        section: GroupSection,
        workAllowed: Set<String>,
    ): Set<String> {
        return when (section) {
            GroupSection.PERSONAL -> prefs.entries.map { it.packageName }.toSet()
            GroupSection.WORK -> workAllowed
        }
    }

    fun ensureWorkIcon(key: String) {
        app.workCatalog.ensureIcon(key)
    }

    private fun refreshDeviceFacts() {
        viewModelScope.launch(Dispatchers.Default) {
            workLinkResolved.value = LaunchController.workProfileSettingsResolves(getApplication())
        }
    }

    private fun buildUi(
        snap: StartupSnap,
        msg: String?,
        linkResolved: Boolean,
        work: WorkHomeState,
        listenerConnected: Boolean,
    ): HomeUiState {
        if (!snap.startupReady) {
            return HomeUiState(message = msg)
        }
        val isDefault = LaunchController.isDefaultHome(getApplication())
        val granted = NlsStatus.isGranted(getApplication())
        val attention = NlsRecovery.attention(
            granted = granted,
            filterEnabled = snap.prefs.nlsFilterEnabled,
            connected = listenerConnected,
        )
        val kind = if (!work.loaded) {
            WorkSectionKind.Hidden
        } else {
            WorkCatalogRules.sectionKind(
                hasWorkProfile = work.hasWorkProfile,
                loadFailed = work.loadFailed,
                quietEnabled = work.quietEnabled,
                activityCount = work.apps.size,
            )
        }
        val shown = if (kind == WorkSectionKind.Hidden) emptyList() else work.apps
        return HomeUiState(
            prefsReady = true,
            iconsReady = snap.iconsLoaded,
            setupDone = snap.prefs.setupDone,
            isDefaultHome = isDefault,
            apps = visible(snap.prefs, snap.apps),
            groups = snap.prefs.groups,
            message = msg,
            banner = selectHomeBanner(
                setupDone = snap.prefs.setupDone,
                isDefaultHome = isDefault,
                nlsNeedsGrant = attention != NlsRecovery.Attention.None,
            ),
            nlsAttention = attention,
            filterActive = NlsStatus.isFilterActive(
                getApplication(),
                snap.prefs.nlsFilterEnabled,
                listenerConnected,
            ),
            workPresence = LaunchpadRules.workPresence(
                loaded = work.loaded,
                hasWorkProfile = work.hasWorkProfile,
                quietEnabled = work.quietEnabled,
                loadFailed = work.loadFailed,
            ),
            workKind = kind,
            workApps = shown,
            workRefreshing = work.refreshing,
            workLink = LaunchpadRules.showWorkSettingsLink(linkResolved, work.hasWorkProfile),
            showBio = BiometricGate.ENABLED_IN_LAUNCH_PATH,
            bioOnCount = if (BiometricGate.ENABLED_IN_LAUNCH_PATH) {
                snap.prefs.entries.count { it.bioEnabled }
            } else {
                0
            },
            hasWorkProfile = work.loaded && work.hasWorkProfile,
            workSectionPaused = snap.prefs.workSectionPaused,
            notificationsPaused = snap.prefs.notificationsPaused,
            namesOnly = snap.prefs.namesOnly,
        )
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
