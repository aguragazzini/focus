@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.foco.launcher.core

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.foco.launcher.R
import kotlinx.coroutines.delay
import com.foco.launcher.notification.NlsRecovery
import com.foco.launcher.notification.SilenceLevel
import com.foco.launcher.registry.AppGroup
import com.foco.launcher.registry.AppOrder
import com.foco.launcher.registry.HomePageSpec
import com.foco.launcher.registry.HomePages
import com.foco.launcher.registry.ArrangedSection
import com.foco.launcher.registry.GroupDrag
import com.foco.launcher.registry.GroupLayout
import com.foco.launcher.registry.GroupMutations
import com.foco.launcher.registry.GroupSection
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.SuggestedApps
import com.foco.launcher.work.WorkApp
import com.foco.launcher.work.WorkCatalogRules
import com.foco.launcher.work.WorkSectionKind
import kotlinx.coroutines.launch

@Composable
fun HomeLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(FocoSpace.gapLg))
            Text(
                text = stringResource(R.string.home_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLaunch: (String) -> Unit,
    onLaunchWork: (WorkApp) -> Unit,
    onOpenFocoSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onEditApps: () -> Unit,
    onAddApps: () -> Unit,
    onChooseDefault: () -> Unit,
    onOpenAvisos: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onRequestCalendar: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenAgendaEvent: (AgendaEvent) -> Unit = {},
    onOpenWorkCalendar: () -> Unit = {},
    onRefreshWork: () -> Unit,
    onRemovePersonal: (String) -> Unit,
    onCreateGroup: (GroupSection, String, String) -> Unit,
    onAddToGroup: (String, String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onRemoveFromGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onDragOntoApp: (GroupSection, String, String, String, String) -> Unit,
    onDragIntoFolder: (String, String) -> Unit,
    onDragEject: (String, String) -> Unit,
    onWorkPaused: (Boolean) -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onCrossHint: () -> Unit,
    onEnsureWorkIcon: (String) -> Unit,
    onOpenWorkSettings: () -> Unit,
    onQuietTap: () -> Unit,
    onMessageShown: () -> Unit,
    requestEdit: Boolean = false,
    onEditRequestConsumed: () -> Unit = {},
    onCreatePage: (String, String) -> Unit = { _, _ -> },
    onRenamePage: (String, String) -> Unit = { _, _ -> },
    onMovePage: (String, Int) -> Unit = { _, _ -> },
    onHidePage: (String, Boolean) -> Unit = { _, _ -> },
    onDeletePage: (String) -> Unit = {},
    onPhonePaused: (Boolean) -> Unit = {},
    onSilence: (SilenceLevel) -> Unit = {},
    onArmSilence: () -> Unit = {},
    onPackagePaused: (String, Boolean) -> Unit = { _, _ -> },
    onAddBlock: (String, String) -> Unit = { _, _ -> },
    onRemoveBlock: (String, String) -> Unit = { _, _ -> },
    onMoveBlock: (String, String, Int) -> Unit = { _, _, _ -> },
    onUpdateBlock: (String, com.foco.launcher.registry.PageBlock) -> Unit = { _, _ -> },
    homeToken: Int = 0,
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    var confirmApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var naming by remember { mutableStateOf<GroupNameRequest?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var workQuery by rememberSaveable { mutableStateOf("") }
    var openedGroupId by remember { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val drag = remember { HomeDragState() }
    val density = LocalDensity.current
    val iconPx = with(density) { (if (state.namesOnly) 28.dp else 48.dp).toPx() }
    var ghostOrigin by remember { mutableStateOf(Offset.Zero) }

    val personalArranged = remember(state.groups, state.apps) {
        GroupLayout.arrange(GroupSection.PERSONAL, state.groups, state.apps.map { it.packageName })
    }
    val workArranged = remember(state.groups, state.workApps) {
        GroupLayout.arrange(GroupSection.WORK, state.groups, state.workApps.map { it.key })
    }
    val openSheet = sheet
    LaunchedEffect(openSheet, state.groups, state.apps, state.workApps) {
        val open = openSheet as? HomeSheet.Open ?: return@LaunchedEffect
        val group = state.groups.find { it.id == open.groupId }
        val live = when (group?.section) {
            GroupSection.PERSONAL -> state.apps.map { it.packageName }.toSet()
            GroupSection.WORK -> state.workApps.map { it.key }.toSet()
            null -> emptySet()
        }
        if (group == null || group.members.none { it in live }) {
            sheet = null
        }
    }
    LaunchedEffect(openedGroupId, state.groups, state.apps, state.workApps, state.workSectionPaused) {
        val id = openedGroupId ?: return@LaunchedEffect
        val group = state.groups.find { it.id == id }
        val live = when (group?.section) {
            GroupSection.PERSONAL -> state.apps.map { it.packageName }.toSet()
            GroupSection.WORK -> state.workApps.map { it.key }.toSet()
            null -> emptySet()
        }
        val hiddenWork = group?.section == GroupSection.WORK && state.workSectionPaused
        if (group == null || group.members.none { it in live } || hiddenWork) {
            openedGroupId = null
            drag.cancel()
        }
    }

    fun labelFor(section: GroupSection, id: String): String {
        return when (section) {
            GroupSection.PERSONAL -> state.apps.find { it.packageName == id }?.label.orEmpty()
            GroupSection.WORK -> state.workApps.find { it.key == id }?.label.orEmpty()
        }
    }

    fun startDrag(
        section: GroupSection,
        cell: HomeCell,
        fromGroupId: String?,
        blocked: Set<String>,
        window: Offset,
    ) {
        drag.start(
            section = section,
            memberId = cell.id,
            label = cell.label,
            fromGroupId = fromGroupId,
            blockedTargetIds = blocked,
            x = window.x,
            y = window.y,
            iconWidth = iconPx,
            iconHeight = iconPx,
            icon = if (state.namesOnly) null else cell.icon,
            nowMs = SystemClock.uptimeMillis(),
        )
    }

    fun moveDrag(window: Offset) {
        drag.move(window.x, window.y, SystemClock.uptimeMillis())
    }

    fun endDrag() {
        val finished = drag.finish() ?: return
        val (session, action) = finished
        when (action) {
            GroupDrag.Action.CREATE -> {
                val targetId = session.hover.targetId ?: return
                onDragOntoApp(
                    session.section,
                    session.memberId,
                    session.label,
                    targetId,
                    labelFor(session.section, targetId),
                )
            }
            GroupDrag.Action.ADD -> {
                val groupId = session.hover.targetId ?: return
                onDragIntoFolder(groupId, session.memberId)
            }
            GroupDrag.Action.REMOVE -> {
                val groupId = session.fromGroupId ?: return
                onDragEject(groupId, session.memberId)
            }
            GroupDrag.Action.REJECT_CROSS -> onCrossHint()
            GroupDrag.Action.SNAP_BACK -> Unit
        }
    }

    LaunchedEffect(requestEdit) {
        if (requestEdit) {
            sheet = null
            editing = true
            onEditRequestConsumed()
        }
    }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        val activating = msg == context.getString(R.string.silence_fail)
        if (activating) {
            val shown = launch {
                snackbar.showSnackbar(msg, duration = SnackbarDuration.Indefinite)
            }
            delay(2_000)
            snackbar.currentSnackbarData?.dismiss()
            shown.join()
        } else {
            snackbar.showSnackbar(msg)
        }
        onMessageShown()
    }

    val workVisible = remember(state.workApps, workQuery, state.workKind) {
        val allowed = WorkCatalogRules.showSearch(state.workKind, state.workApps.size)
        val query = if (allowed) workQuery else ""
        WorkCatalogRules.filterVisible(
            items = state.workApps,
            query = query,
            label = { it.label },
            packageName = { it.packageName },
        )
    }
    val searchOpen = WorkCatalogRules.showSearch(state.workKind, state.workApps.size)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                val muted = data.visuals.message == context.getString(R.string.silence_fail)
                Snackbar(
                    snackbarData = data,
                    containerColor = if (muted) FocoInkElevated else FocoPaper,
                    contentColor = if (muted) FocoPaperDim else FocoInk,
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val next = coords.localToWindow(Offset.Zero)
                    if (next != ghostOrigin) ghostOrigin = next
                },
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            HomeOverflow(
                showWorkRefresh = state.workKind != WorkSectionKind.Hidden,
                showWorkPause = state.hasWorkProfile,
                workPaused = state.workSectionPaused,
                onToggleWork = { onWorkPaused(!state.workSectionPaused) },
                onAddApps = onAddApps,
                onEditApps = onEditApps,
                onOpenFocoSettings = onOpenFocoSettings,
                onChooseDefault = onChooseDefault,
                onOpenSystemSettings = onOpenSystemSettings,
                onRefreshWork = onRefreshWork,
            )
            val scope = rememberCoroutineScope()
            val visible = HomePages.visible(state.homePages)
            val landing = HomePages.landingIndex(visible)
            val pagerState = rememberPagerState(
                initialPage = landing.coerceIn(0, (visible.size - 1).coerceAtLeast(0)),
            ) { visible.size.coerceAtLeast(1) }
            val pagerFling = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                ),
            )
            LaunchedEffect(visible.size) {
                val last = (visible.size - 1).coerceAtLeast(0)
                if (pagerState.currentPage > last) pagerState.scrollToPage(last)
            }
            LaunchedEffect(homeToken) {
                if (homeToken > 0) editing = false
                val pages = HomePages.visible(state.homePages)
                val last = (pages.size - 1).coerceAtLeast(0)
                val target = HomePages.landingIndex(pages).coerceIn(0, last)
                if (pagerState.currentPage != target) pagerState.scrollToPage(target)
            }
            val pageLabels = visible.map { pageLabel(it) }
            val cueIndex = pagerState.currentPage.coerceIn(0, (pageLabels.size - 1).coerceAtLeast(0))
            val cueSpec = visible.getOrNull(cueIndex)
            if (editing) {
                HomeEditor(
                    pages = state.homePages,
                    apps = state.apps,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onRename = onRenamePage,
                    onMove = onMovePage,
                    onHide = onHidePage,
                    onDelete = onDeletePage,
                    onCreate = onCreatePage,
                    onAddBlock = onAddBlock,
                    onRemoveBlock = onRemoveBlock,
                    onMoveBlock = onMoveBlock,
                    onUpdateBlock = onUpdateBlock,
                    onDismiss = { editing = false },
                )
            }
            if (!editing) {
            HomePagerCue(
                labels = pageLabels,
                page = cueIndex,
                showLabel = cueSpec == null || !HomePages.drawsPageTitle(cueSpec),
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                onEdit = {
                    sheet = null
                    editing = true
                },
            )
            val bannerPageId = visible.firstOrNull { it.type == HomePages.TYPE_PERSONAL }?.id
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondViewportPageCount = 1,
                flingBehavior = pagerFling,
                userScrollEnabled = drag.chrome == null,
                verticalAlignment = Alignment.Top,
                key = { index -> visible.getOrNull(index)?.id ?: index },
            ) { page ->
                val spec = visible.getOrNull(page)
                when (spec?.type) {
                    HomePages.TYPE_CLOCK -> ClockHomePage(
                        blocks = com.foco.launcher.registry.PageBlocks.effective(spec),
                        state = state,
                        onOpenClock = onOpenClock,
                        onOpenCalendar = onOpenCalendar,
                        onOpenSystemSettings = onOpenSystemSettings,
                        onNotificationsPaused = onNotificationsPaused,
                        onPhonePaused = onPhonePaused,
                        onSilence = onSilence,
                        onLaunch = onLaunch,
                        onOpenAvisos = onOpenAvisos,
                        onArmSilence = onArmSilence,
                        onUpdateBlock = { onUpdateBlock(spec.id, it) },
                        showChromePill = visible.size < 2,
                    )
                    HomePages.TYPE_AGENDA -> {
                        val blocks = com.foco.launcher.registry.PageBlocks.effective(spec)
                        if (blocks.size == 1 && blocks[0].type == com.foco.launcher.registry.PageBlocks.AGENDA) {
                            AgendaHomePage(
                                title = spec.label,
                                hasWorkProfile = state.hasWorkProfile,
                                onRequestPermission = onRequestCalendar,
                                onOpenAppSettings = onOpenAppSettings,
                                onOpenEvent = onOpenAgendaEvent,
                                onOpenWorkCalendar = onOpenWorkCalendar,
                                onOpenSystemSettings = onOpenSystemSettings,
                            )
                        } else {
                            BlockPage(
                                blocks = blocks,
                                state = state,
                                onOpenClock = onOpenClock,
                                onOpenCalendar = onOpenCalendar,
                                onOpenSystemSettings = onOpenSystemSettings,
                                onNotificationsPaused = onNotificationsPaused,
                                onPhonePaused = onPhonePaused,
                                onSilence = onSilence,
                                onLaunch = onLaunch,
                                onOpenAvisos = onOpenAvisos,
                                onArmSilence = onArmSilence,
                                onUpdateBlock = { onUpdateBlock(spec.id, it) },
                            )
                        }
                    }
                    HomePages.TYPE_PERSONAL -> PersonalHomePage(
                        state = state,
                        arranged = personalArranged,
                        drag = drag,
                        pageKey = spec.id,
                        title = spec.label,
                        showBanners = spec.id == bannerPageId,
                        showPause = true,
                        onAddApps = onAddApps,
                        onOpenSystemSettings = onOpenSystemSettings,
                        onChooseDefault = onChooseDefault,
                        onOpenAvisos = onOpenAvisos,
                        onArmSilence = onArmSilence,
                        onNotificationsPaused = onNotificationsPaused,
                        onPhonePaused = onPhonePaused,
                        onSilence = onSilence,
                        onPackagePaused = onPackagePaused,
                        onLaunch = onLaunch,
                        onOpenGroup = { openedGroupId = it },
                        onPersonalSheet = { sheet = HomeSheet.Personal(it) },
                        onGroupSheet = {
                            openedGroupId = null
                            sheet = HomeSheet.Open(it)
                        },
                        onDragStart = { cell, window ->
                            startDrag(GroupSection.PERSONAL, cell, null, emptySet(), window)
                        },
                        onDrag = { window -> moveDrag(window) },
                        onDragEnd = { endDrag() },
                        header = {
                            BlockPage(
                                blocks = com.foco.launcher.registry.PageBlocks.effective(spec).filter {
                                    it.type != com.foco.launcher.registry.PageBlocks.SILENCIO &&
                                        it.type != com.foco.launcher.registry.PageBlocks.PAUSAR
                                },
                                state = state,
                                onOpenClock = onOpenClock,
                                onOpenCalendar = onOpenCalendar,
                                onOpenSystemSettings = onOpenSystemSettings,
                                onNotificationsPaused = onNotificationsPaused,
                                onPhonePaused = onPhonePaused,
                                onSilence = onSilence,
                                onLaunch = onLaunch,
                                onOpenAvisos = onOpenAvisos,
                                onArmSilence = onArmSilence,
                                onUpdateBlock = { onUpdateBlock(spec.id, it) },
                                scroll = false,
                            )
                        },
                    )
                    HomePages.TYPE_APPS -> PersonalHomePage(
                        state = state,
                        arranged = personalArranged,
                        drag = drag,
                        pageKey = spec.id,
                        title = spec.label,
                        showBanners = false,
                        showPause = false,
                        titleFallback = R.string.home_page_apps,
                        onAddApps = onAddApps,
                        onOpenSystemSettings = onOpenSystemSettings,
                        onChooseDefault = onChooseDefault,
                        onOpenAvisos = onOpenAvisos,
                        onArmSilence = onArmSilence,
                        onNotificationsPaused = onNotificationsPaused,
                        onPhonePaused = onPhonePaused,
                        onSilence = onSilence,
                        onPackagePaused = onPackagePaused,
                        onLaunch = onLaunch,
                        onOpenGroup = { openedGroupId = it },
                        onPersonalSheet = { sheet = HomeSheet.Personal(it) },
                        onGroupSheet = {
                            openedGroupId = null
                            sheet = HomeSheet.Open(it)
                        },
                        onDragStart = { cell, window ->
                            startDrag(GroupSection.PERSONAL, cell, null, emptySet(), window)
                        },
                        onDrag = { window -> moveDrag(window) },
                        onDragEnd = { endDrag() },
                        header = {
                            BlockPage(
                                blocks = com.foco.launcher.registry.PageBlocks.effective(spec),
                                state = state,
                                onOpenClock = onOpenClock,
                                onOpenCalendar = onOpenCalendar,
                                onOpenSystemSettings = onOpenSystemSettings,
                                onNotificationsPaused = onNotificationsPaused,
                                onPhonePaused = onPhonePaused,
                                onSilence = onSilence,
                                onLaunch = onLaunch,
                                onOpenAvisos = onOpenAvisos,
                                onArmSilence = onArmSilence,
                                onUpdateBlock = { onUpdateBlock(spec.id, it) },
                                scroll = false,
                            )
                        },
                    )
                    HomePages.TYPE_DIET -> {
                        val meals = remember(context) {
                            DietPlan.peek() ?: runCatching {
                                context.assets.open("plan_ragazzini.json").bufferedReader().use { reader ->
                                    DietPlan.parse(reader.readText())
                                }
                            }.getOrNull()?.also { DietPlan.store(it) }
                        }
                        val blocks = com.foco.launcher.registry.PageBlocks.effective(spec)
                        if (blocks.size == 1 && blocks[0].type == com.foco.launcher.registry.PageBlocks.COMIDA) {
                            DietPage(
                                plan = meals,
                                onOpenSystemSettings = onOpenSystemSettings,
                            )
                        } else {
                            BlockPage(
                                blocks = blocks,
                                state = state,
                                onOpenClock = onOpenClock,
                                onOpenCalendar = onOpenCalendar,
                                onOpenSystemSettings = onOpenSystemSettings,
                                onNotificationsPaused = onNotificationsPaused,
                                onPhonePaused = onPhonePaused,
                                onSilence = onSilence,
                                onLaunch = onLaunch,
                                onOpenAvisos = onOpenAvisos,
                                onArmSilence = onArmSilence,
                                onUpdateBlock = { onUpdateBlock(spec.id, it) },
                            )
                        }
                    }
                    HomePages.TYPE_WORK -> WorkHomePage(
                        state = state,
                        pageKey = spec.id,
                        title = spec.label,
                        arranged = workArranged,
                        visible = workVisible,
                        searchOpen = searchOpen,
                        query = if (searchOpen) workQuery else "",
                        drag = drag,
                        onQuery = { workQuery = it },
                        onOpenWorkSettings = onOpenWorkSettings,
                        onRefreshWork = onRefreshWork,
                        onWorkPaused = onWorkPaused,
                        onOpenSystemSettings = onOpenSystemSettings,
                        onQuietTap = onQuietTap,
                        onLaunchWork = onLaunchWork,
                        onEnsureWorkIcon = onEnsureWorkIcon,
                        onOpenGroup = { openedGroupId = it },
                        onWorkSheet = { sheet = HomeSheet.Work(it) },
                        onGroupSheet = {
                            openedGroupId = null
                            sheet = HomeSheet.Open(it)
                        },
                        onDragStart = { cell, window ->
                            startDrag(GroupSection.WORK, cell, null, emptySet(), window)
                        },
                        onDrag = { window -> moveDrag(window) },
                        onDragEnd = { endDrag() },
                        header = {
                            BlockPage(
                                blocks = com.foco.launcher.registry.PageBlocks.effective(spec),
                                state = state,
                                onOpenClock = onOpenClock,
                                onOpenCalendar = onOpenCalendar,
                                onOpenSystemSettings = onOpenSystemSettings,
                                onNotificationsPaused = onNotificationsPaused,
                                onPhonePaused = onPhonePaused,
                                onSilence = onSilence,
                                onLaunch = onLaunch,
                                onOpenAvisos = onOpenAvisos,
                                onArmSilence = onArmSilence,
                                onUpdateBlock = { onUpdateBlock(spec.id, it) },
                                scroll = false,
                            )
                        },
                    )
                    else -> Unit
                }
            }
            }

        val openGroup = openedGroupId?.let { id -> state.groups.find { it.id == id } }
        if (openGroup != null) {
            OpenFolderOverlay(
                group = openGroup,
                personalApps = state.apps,
                workApps = state.workApps,
                workIcons = state.workIcons,
                namesOnly = state.namesOnly,
                workMuted = state.workKind == WorkSectionKind.Quiet,
                drag = drag,
                onDismiss = { openedGroupId = null },
                onPanelBounds = { drag.folderPanel = it },
                onLaunchPersonal = onLaunch,
                onLaunchWork = { app ->
                    if (state.workKind == WorkSectionKind.Quiet) onQuietTap() else onLaunchWork(app)
                },
                onLongClick = {
                    openedGroupId = null
                    sheet = HomeSheet.Open(openGroup.id)
                },
                onDragStart = { cell, window ->
                    startDrag(
                        section = openGroup.section,
                        cell = cell,
                        fromGroupId = openGroup.id,
                        blocked = openGroup.members.filter { it != cell.id }.toSet(),
                        window = window,
                    )
                },
                onDrag = { window -> moveDrag(window) },
                onDragEnd = { endDrag() },
                onEnsureMember = if (openGroup.section == GroupSection.WORK) onEnsureWorkIcon else null,
            )
            DisposableEffect(openGroup.id) {
                onDispose { drag.folderPanel = null }
            }
        }

        HomeDragGhost(
            drag = drag,
            namesOnly = state.namesOnly,
            origin = ghostOrigin,
        )

    val activeSheet = sheet
    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = FocoInkElevated,
            contentColor = FocoPaper,
            tonalElevation = 0.dp,
            dragHandle = { BottomSheetDefaults.DragHandle(color = FocoPaperDim) },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            when (activeSheet) {
                is HomeSheet.Personal -> PersonalAppSheet(
                    app = activeSheet.app,
                    paused = activeSheet.app.packageName in state.pausedPackages,
                    onOpen = {
                        val pkg = activeSheet.app.packageName
                        sheet = null
                        onLaunch(pkg)
                    },
                    onPause = {
                        val pkg = activeSheet.app.packageName
                        val paused = pkg in state.pausedPackages
                        onPackagePaused(pkg, !paused)
                    },
                    onAddToGroup = {
                        sheet = HomeSheet.Pick(
                            section = GroupSection.PERSONAL,
                            memberId = activeSheet.app.packageName,
                            label = activeSheet.app.label,
                        )
                    },
                    onRemove = {
                        val target = activeSheet.app
                        sheet = null
                        val settingsPkg = SuggestedApps.settingsPackage(context)
                        if (LaunchpadRules.needsSettingsConfirm(target.packageName, settingsPkg)) {
                            confirmApp = target
                        } else {
                            onRemovePersonal(target.packageName)
                        }
                    },
                )
                is HomeSheet.Work -> WorkAppSheet(
                    app = activeSheet.app,
                    paused = activeSheet.app.packageName in state.pausedPackages,
                    onOpen = {
                        val target = activeSheet.app
                        sheet = null
                        if (state.workKind == WorkSectionKind.Quiet) onQuietTap() else onLaunchWork(target)
                    },
                    onPause = {
                        val pkg = activeSheet.app.packageName
                        val paused = pkg in state.pausedPackages
                        onPackagePaused(pkg, !paused)
                    },
                    onAddToGroup = {
                        sheet = HomeSheet.Pick(
                            section = GroupSection.WORK,
                            memberId = activeSheet.app.key,
                            label = activeSheet.app.label,
                        )
                    },
                )
                is HomeSheet.Pick -> GroupPickSheet(
                    label = activeSheet.label,
                    groups = state.groups
                        .filter { it.section == activeSheet.section }
                        .sortedBy { it.order },
                    onPick = { groupId ->
                        val memberId = activeSheet.memberId
                        sheet = null
                        onAddToGroup(groupId, memberId)
                    },
                    onCreate = {
                        naming = GroupNameRequest(
                            section = activeSheet.section,
                            memberId = activeSheet.memberId,
                            groupId = null,
                            initial = "",
                        )
                        sheet = null
                    },
                )
                is HomeSheet.Open -> {
                    val group = state.groups.find { it.id == activeSheet.groupId }
                    if (group != null) {
                        if (group.section == GroupSection.WORK) {
                            LaunchedEffect(group.id, group.members) {
                                group.members.forEach(onEnsureWorkIcon)
                            }
                        }
                        OpenGroupSheet(
                            group = group,
                            personalApps = state.apps,
                            workApps = state.workApps,
                            workIcons = state.workIcons,
                            workMuted = state.workKind == WorkSectionKind.Quiet,
                            onLaunchPersonal = { pkg ->
                                sheet = null
                                onLaunch(pkg)
                            },
                            onLaunchWork = { app ->
                                sheet = null
                                if (state.workKind == WorkSectionKind.Quiet) onQuietTap() else onLaunchWork(app)
                            },
                            onRemoveMember = { memberId ->
                                onRemoveFromGroup(group.id, memberId)
                            },
                            onRename = {
                                naming = GroupNameRequest(
                                    section = group.section,
                                    memberId = null,
                                    groupId = group.id,
                                    initial = group.name,
                                )
                            },
                            onDelete = { confirmDeleteId = group.id },
                        )
                    }
                }
            }
        }
    }

    val nameRequest = naming
    if (nameRequest != null) {
        GroupNameDialog(
            create = nameRequest.groupId == null,
            initial = nameRequest.initial,
            onDismiss = { naming = null },
            onConfirm = { raw ->
                val request = nameRequest
                naming = null
                if (request.groupId != null) {
                    onRenameGroup(request.groupId, raw)
                } else if (request.memberId != null) {
                    onCreateGroup(request.section, raw, request.memberId)
                }
            },
        )
    }

    val deleteId = confirmDeleteId
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            text = {
                Text(
                    text = stringResource(R.string.group_delete_body),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            confirmButton = {
                FocoTextButton(
                    onClick = {
                        onDeleteGroup(deleteId)
                        confirmDeleteId = null
                        sheet = null
                    },
                ) { Text(stringResource(R.string.group_delete)) }
            },
            dismissButton = {
                FocoTextButton(onClick = { confirmDeleteId = null }) {
                    Text(stringResource(R.string.group_cancel))
                }
            },
        )
    }

    val pendingConfirm = confirmApp
    if (pendingConfirm != null) {
        AlertDialog(
            onDismissRequest = { confirmApp = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            text = {
                Text(
                    text = stringResource(R.string.confirm_remove_settings),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            confirmButton = {
                FocoTextButton(
                    onClick = {
                        onRemovePersonal(pendingConfirm.packageName)
                        confirmApp = null
                    },
                ) { Text(stringResource(R.string.confirm_remove_anyway)) }
            },
            dismissButton = {
                FocoTextButton(onClick = { confirmApp = null }) {
                    Text(stringResource(R.string.confirm_leave))
                }
            },
        )
    }

        }
    }
    }
}

@Composable
private fun HomeEditor(
    pages: List<HomePageSpec>,
    apps: List<com.foco.launcher.registry.LaunchableApp>,
    onRename: (String, String) -> Unit,
    onMove: (String, Int) -> Unit,
    onHide: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onCreate: (String, String) -> Unit,
    onAddBlock: (String, String) -> Unit,
    onRemoveBlock: (String, String) -> Unit,
    onMoveBlock: (String, String, Int) -> Unit,
    onUpdateBlock: (String, com.foco.launcher.registry.PageBlock) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf("pages") }
    var renameId by remember { mutableStateOf<String?>(null) }
    var blocksPageId by remember { mutableStateOf<String?>(null) }
    val blocksPage = pages.find { it.id == blocksPageId }
    val renameTarget = pages.find { it.id == renameId }
    Column(modifier = modifier.padding(horizontal = FocoSpace.page)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (blocksPage != null) R.string.home_edit_blocks else R.string.home_edit,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaper,
            )
            FocoTextButton(onClick = {
                if (blocksPage != null || mode != "pages") {
                    blocksPageId = null
                    mode = "pages"
                } else {
                    onDismiss()
                }
            }) {
                Text(
                    stringResource(
                        if (blocksPage != null || mode != "pages") R.string.home_blocks_back else R.string.home_edit_done,
                    ),
                )
            }
        }
        when {
            blocksPage != null && mode == "catalog" -> {
                val current = com.foco.launcher.registry.PageBlocks.effective(blocksPage)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(com.foco.launcher.registry.PageBlocks.ALL, key = { it }) { type ->
                        val taken = !com.foco.launcher.registry.PageBlocks.allowsDuplicate(type) &&
                            current.any { it.type == type }
                        Text(
                            text = blockLabel(type) + if (taken) " · " + stringResource(R.string.home_blocks_have) else "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !taken) {
                                    onAddBlock(blocksPage.id, type)
                                    mode = "blocks"
                                }
                                .padding(vertical = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (taken) FocoPaperDim else FocoPaper,
                        )
                    }
                }
            }
            blocksPage != null -> {
                val current = com.foco.launcher.registry.PageBlocks.effective(blocksPage)
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)) {
                    items(current.size, key = { current[it].id }) { index ->
                        val block = current[index]
                        Text(
                            text = blockLabel(block.type),
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = FocoPaper,
                        )
                        if (block.type == com.foco.launcher.registry.PageBlocks.NOTA) {
                            Text(
                                text = block.note.ifBlank { stringResource(R.string.block_note_hint) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = FocoPaperDim,
                            )
                        }
                        if (block.type == com.foco.launcher.registry.PageBlocks.FAVORITOS) {
                            apps.forEach { app ->
                                val on = app.packageName in block.pins
                                Text(
                                    text = (if (on) "● " else "○ ") + app.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val pins = if (on) block.pins.filterNot { it == app.packageName }
                                            else (block.pins + app.packageName).distinct().take(8)
                                            onUpdateBlock(blocksPage.id, block.copy(pins = pins))
                                        }
                                        .padding(vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FocoPaper,
                                )
                            }
                        }
                        Row {
                            if (index > 0) {
                                FocoTextButton(onClick = { onMoveBlock(blocksPage.id, block.id, -1) }) {
                                    Text(stringResource(R.string.home_edit_up))
                                }
                            }
                            if (index < current.lastIndex) {
                                FocoTextButton(onClick = { onMoveBlock(blocksPage.id, block.id, 1) }) {
                                    Text(stringResource(R.string.home_edit_down))
                                }
                            }
                            FocoTextButton(onClick = { onRemoveBlock(blocksPage.id, block.id) }) {
                                Text(stringResource(R.string.home_edit_delete))
                            }
                        }
                    }
                }
                FocoTextButton(onClick = { mode = "catalog" }) {
                    Text(stringResource(R.string.home_blocks_add))
                }
            }
            mode == "types" -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        HomePageTypePicker(
                            onPick = { type, _ ->
                                mode = "pages"
                                onCreate(type, "")
                            },
                        )
                    }
                }
            }
            else -> {
                val visibleCount = pages.count { !it.hidden }
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)) {
                    items(pages.size, key = { pages[it].id }) { index ->
                        val page = pages[index]
                        Text(
                            text = pageLabel(page),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { renameId = page.id }
                                .padding(top = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (page.hidden) FocoPaperDim else FocoPaper,
                        )
                        Row {
                            if (index > 0) {
                                FocoTextButton(onClick = { onMove(page.id, -1) }) {
                                    Text(stringResource(R.string.home_edit_up))
                                }
                            }
                            if (index < pages.lastIndex) {
                                FocoTextButton(onClick = { onMove(page.id, 1) }) {
                                    Text(stringResource(R.string.home_edit_down))
                                }
                            }
                            if (page.hidden || visibleCount > 1) {
                                FocoTextButton(onClick = { onHide(page.id, !page.hidden) }) {
                                    Text(stringResource(if (page.hidden) R.string.home_edit_show else R.string.home_edit_hide))
                                }
                            }
                            if (HomePages.canDelete(pages, page.id)) {
                                FocoTextButton(onClick = { onDelete(page.id) }) {
                                    Text(stringResource(R.string.home_edit_delete))
                                }
                            }
                            FocoTextButton(onClick = {
                                blocksPageId = page.id
                                mode = "blocks"
                            }) {
                                Text(stringResource(R.string.home_edit_blocks))
                            }
                        }
                    }
                }
                if (pages.size < HomePages.MAX) {
                    FocoTextButton(onClick = { mode = "types" }) {
                        Text(stringResource(R.string.home_edit_add))
                    }
                }
            }
        }
    }
    val naming = renameTarget
    if (naming != null) {
        HomePageNameDialog(
            initial = pageLabel(naming),
            onDismiss = { renameId = null },
            onConfirm = { raw ->
                onRename(naming.id, raw)
                renameId = null
            },
        )
    }
}


@Composable
private fun HomePageTypePicker(onPick: (String, String) -> Unit) {
    val types = listOf(
        HomePages.TYPE_CLOCK to R.string.page_clock,
        HomePages.TYPE_AGENDA to R.string.page_agenda,
        HomePages.TYPE_PERSONAL to R.string.section_personal,
        HomePages.TYPE_APPS to R.string.home_page_apps,
        HomePages.TYPE_DIET to R.string.page_diet,
        HomePages.TYPE_WORK to R.string.section_work,
    )
    types.forEach { (type, labelRes) ->
        val label = stringResource(labelRes)
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick(type, label) }
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = FocoPaper,
        )
    }
}

@Composable
private fun HomePageNameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var raw by remember { mutableStateOf(initial) }
    val clean = raw.trim().replace(Regex("\\s+"), " ").take(GroupMutations.NAME_MAX)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        text = {
            OutlinedTextField(
                value = raw,
                onValueChange = { raw = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = FocoPaper),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(clean) }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocoPaper,
                    unfocusedTextColor = FocoPaper,
                    focusedBorderColor = FocoPaperDim,
                    unfocusedBorderColor = FocoLine,
                    cursorColor = FocoPaper,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        },
        confirmButton = {
            FocoTextButton(onClick = { onConfirm(clean) }) {
                Text(stringResource(R.string.home_edit_done))
            }
        },
        dismissButton = {
            FocoTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.group_cancel))
            }
        },
    )
}

@Composable
private fun ClockHomePage(
    blocks: List<com.foco.launcher.registry.PageBlock>,
    state: HomeUiState,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onPhonePaused: (Boolean) -> Unit,
    onSilence: (SilenceLevel) -> Unit,
    onOpenAvisos: () -> Unit,
    onArmSilence: () -> Unit = {},
    onLaunch: (String) -> Unit,
    onUpdateBlock: (com.foco.launcher.registry.PageBlock) -> Unit,
    showChromePill: Boolean = false,
) {
    val defaultClock = blocks.map { it.type } == com.foco.launcher.registry.PageBlocks.defaultsFor(HomePages.TYPE_CLOCK)
    if (defaultClock) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            DefaultClockGlance(
                onOpenClock = onOpenClock,
                onOpenCalendar = onOpenCalendar,
            )
            if (showChromePill) {
                Spacer(Modifier.height(FocoSpace.gap))
                SilenceControl(
                    notificationsPaused = state.notificationsPaused,
                    phonePaused = state.phonePaused,
                    listenerReady = state.nlsReady,
                    onChange = onSilence,
                    onActivate = {
                        onArmSilence()
                        onOpenAvisos()
                    },
                    modifier = Modifier.padding(horizontal = FocoSpace.page),
                )
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .homeLongPress(onOpenSystemSettings),
            )
        }
    } else {
        BlockPage(
            blocks = blocks,
            state = state,
            onOpenClock = onOpenClock,
            onOpenCalendar = onOpenCalendar,
            onOpenSystemSettings = onOpenSystemSettings,
            onNotificationsPaused = onNotificationsPaused,
            onPhonePaused = onPhonePaused,
            onSilence = onSilence,
            onOpenAvisos = onOpenAvisos,
            onArmSilence = onArmSilence,
            onLaunch = onLaunch,
            onUpdateBlock = onUpdateBlock,
            showPill = showChromePill && blocks.none {
                it.type == com.foco.launcher.registry.PageBlocks.SILENCIO ||
                    it.type == com.foco.launcher.registry.PageBlocks.PAUSAR
            },
        )
    }
}

@Composable
private fun BlockPage(
    blocks: List<com.foco.launcher.registry.PageBlock>,
    state: HomeUiState,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onPhonePaused: (Boolean) -> Unit,
    onSilence: (SilenceLevel) -> Unit,
    onOpenAvisos: () -> Unit = {},
    onArmSilence: () -> Unit = {},
    onLaunch: (String) -> Unit,
    onUpdateBlock: (com.foco.launcher.registry.PageBlock) -> Unit,
    scroll: Boolean = true,
    showPill: Boolean = false,
) {
    if (!scroll && blocks.isEmpty() && !showPill) return
    val body: @Composable () -> Unit = {
        PageBlockColumn(
            blocks = blocks,
            apps = state.apps,
            workApps = state.workApps,
            hasWorkProfile = state.hasWorkProfile,
            notificationsPaused = state.notificationsPaused,
            phonePaused = state.phonePaused,
            listenerReady = state.nlsReady,
            onOpenClock = onOpenClock,
            onOpenCalendar = onOpenCalendar,
            onSilence = onSilence,
            onActivateListener = {
                onArmSilence()
                onOpenAvisos()
            },
            onNotificationsPaused = onNotificationsPaused,
            onPhonePaused = onPhonePaused,
            onLaunch = onLaunch,
            onUpdateBlock = onUpdateBlock,
            showEmpty = scroll,
        )
        if (showPill) {
            Spacer(Modifier.height(FocoSpace.gap))
            SilenceControl(
                notificationsPaused = state.notificationsPaused,
                phonePaused = state.phonePaused,
                listenerReady = state.nlsReady,
                onChange = onSilence,
                onActivate = {
                    onArmSilence()
                    onOpenAvisos()
                },
                modifier = Modifier.padding(horizontal = FocoSpace.page),
            )
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .homeLongPress(onOpenSystemSettings),
        )
    }
    if (scroll) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) { body() }
    } else {
        body()
    }
}

@Composable
private fun PersonalHomePage(
    state: HomeUiState,
    arranged: ArrangedSection,
    drag: HomeDragState,
    pageKey: String,
    title: String,
    showBanners: Boolean,
    showPause: Boolean,
    titleFallback: Int = R.string.section_personal,
    onAddApps: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onChooseDefault: () -> Unit,
    onOpenAvisos: () -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onPhonePaused: (Boolean) -> Unit,
    onSilence: (SilenceLevel) -> Unit,
    onArmSilence: () -> Unit = {},
    onPackagePaused: (String, Boolean) -> Unit,
    onLaunch: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onPersonalSheet: (LaunchableApp) -> Unit,
    onGroupSheet: (String) -> Unit,
    onDragStart: (HomeCell, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    header: @Composable () -> Unit = {},
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "$pageKey:blocks") { header() }
        if (showBanners) {
            item(key = "$pageKey:banner") {
                when (state.banner) {
                    HomeBanner.NotDefault -> NotDefaultBanner(onChooseDefault)
                    HomeBanner.Nls -> NlsOffBanner(
                        disconnected = state.nlsAttention == NlsRecovery.Attention.Disconnected,
                        onOpenAvisos = onOpenAvisos,
                    )
                    HomeBanner.None -> Unit
                }
                if (state.banner != HomeBanner.None) {
                    Spacer(Modifier.height(FocoSpace.section))
                }
            }
        }
        item(key = "$pageKey:personal-header") {
            Spacer(Modifier.height(FocoSpace.gap))
            SectionTitle(
                text = title.ifBlank { stringResource(titleFallback) },
                modifier = Modifier.padding(horizontal = FocoSpace.page),
            )
            if (showPause) {
                Spacer(Modifier.height(FocoSpace.gap))
                SilenceControl(
                    notificationsPaused = state.notificationsPaused,
                    phonePaused = state.phonePaused,
                    listenerReady = state.nlsReady,
                    onChange = onSilence,
                    onActivate = {
                        onArmSilence()
                        onOpenAvisos()
                    },
                    modifier = Modifier.padding(horizontal = FocoSpace.page),
                )
            }
            Spacer(Modifier.height(FocoSpace.section))
        }
        if (arranged.groups.isEmpty() && arranged.looseIds.isEmpty()) {
            item(key = "$pageKey:personal-empty") {
                PersonalEmptyInline(
                    onAddApps = onAddApps,
                    onOpenSystemSettings = onOpenSystemSettings,
                )
            }
        } else {
            val personalByPkg = state.apps.associateBy { it.packageName }
            val paused = state.pausedPackages.toSet()
            val rows = sectionCells(
                arranged = arranged,
                loose = { id ->
                    personalByPkg[id]?.toCell()?.copy(paused = id in paused)
                },
                folder = { group ->
                    val members = membersByLabel(group.members) { personalByPkg[it]?.label ?: it }
                        .mapNotNull { personalByPkg[it] }
                    group.toCell(members.map { it.icon }, labels = members.map { it.label })
                },
            ).chunked(4)
            items(
                items = rows,
                key = { row -> "$pageKey:p:" + row.joinToString("|") { it.key } },
            ) { row ->
                val rowKey = "$pageKey:p:" + row.joinToString("|") { it.key }
                AppRow(
                    cells = row,
                    section = GroupSection.PERSONAL,
                    rowKey = rowKey,
                    iconEpoch = 0L,
                    namesOnly = state.namesOnly,
                    drag = drag,
                    dragApps = true,
                    fromGroupId = null,
                    modifier = Modifier.padding(horizontal = FocoSpace.page),
                    onClick = { cell ->
                        if (cell.groupId != null) {
                            onOpenGroup(cell.groupId)
                        } else {
                            onLaunch(cell.id)
                        }
                    },
                    onLongClick = { cell ->
                        if (cell.groupId != null) {
                            onGroupSheet(cell.groupId)
                        } else {
                            personalByPkg[cell.id]?.let(onPersonalSheet)
                        }
                    },
                    onEnsureIcon = null,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                )
            }
            item(key = "$pageKey:personal-pad") {
                GridPad(drag = drag, section = GroupSection.PERSONAL, rowKey = "$pageKey:personal-pad")
            }
        }
        item(key = "$pageKey:escape") {
            EscapePad(onOpenSystemSettings)
        }
    }
}

@Composable
private fun WorkHomePage(
    state: HomeUiState,
    pageKey: String,
    title: String,
    arranged: ArrangedSection,
    visible: List<WorkApp>,
    searchOpen: Boolean,
    query: String,
    drag: HomeDragState,
    onQuery: (String) -> Unit,
    onOpenWorkSettings: () -> Unit,
    onRefreshWork: () -> Unit,
    onWorkPaused: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onQuietTap: () -> Unit,
    onLaunchWork: (WorkApp) -> Unit,
    onEnsureWorkIcon: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onWorkSheet: (WorkApp) -> Unit,
    onGroupSheet: (String) -> Unit,
    onDragStart: (HomeCell, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    header: @Composable () -> Unit = {},
) {
    val focoPaused = state.workSectionPaused && state.hasWorkProfile
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "$pageKey:blocks") { header() }
        item(key = "$pageKey:work-header") {
            Spacer(Modifier.height(FocoSpace.gap))
            SectionTitle(
                text = title.ifBlank { stringResource(R.string.section_work) },
                modifier = Modifier.padding(horizontal = FocoSpace.page),
            )
            if (state.hasWorkProfile) {
                Spacer(Modifier.height(FocoSpace.gap))
                PagePause(
                    paused = focoPaused,
                    idle = stringResource(R.string.work_pause),
                    active = stringResource(R.string.work_paused_chip),
                    onClick = { onWorkPaused(!state.workSectionPaused) },
                )
            }
            Spacer(Modifier.height(FocoSpace.gapLg))
        }
        if (focoPaused) {
            item(key = "$pageKey:work-paused") {
                Text(
                    text = stringResource(R.string.work_pause_status),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FocoPaperDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FocoSpace.page),
                )
            }
        } else if (state.workKind == WorkSectionKind.Hidden) {
            item(key = "$pageKey:work-absent") {
                Text(
                    text = stringResource(R.string.settings_work_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = FocoSpace.page),
                )
            }
        } else {
            item(key = "$pageKey:work-tools") {
                WorkHeader(
                    kind = state.workKind,
                    query = query,
                    showSearch = searchOpen,
                    showQuietLink = state.workLink,
                    showTitle = false,
                    onQuery = onQuery,
                    onOpenWorkSettings = onOpenWorkSettings,
                )
            }
            when {
                WorkCatalogRules.showErrorCopy(state.workKind) -> {
                    item(key = "$pageKey:work-error") {
                        Column(Modifier.padding(horizontal = FocoSpace.page)) {
                            Text(
                                text = stringResource(R.string.work_load_error),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FocoTextButton(onClick = onRefreshWork) {
                                Text(stringResource(R.string.work_retry))
                            }
                        }
                    }
                }
                WorkCatalogRules.showWorkEmptyCopy(state.workKind) -> {
                    item(key = "$pageKey:work-empty") {
                        Text(
                            text = stringResource(R.string.work_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = FocoSpace.page),
                        )
                    }
                }
                searchOpen && query.isNotBlank() && visible.isEmpty() -> {
                    item(key = "$pageKey:work-search-empty") {
                        Text(
                            text = stringResource(R.string.work_search_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = FocoSpace.page),
                        )
                    }
                }
                WorkCatalogRules.showWorkGrid(
                    state.workKind,
                    if (searchOpen && query.isNotBlank()) {
                        visible.size
                    } else {
                        arranged.groups.size + arranged.looseIds.size
                    },
                ) -> {
                    val byKey = state.workApps.associateBy { it.key }
                    val muted = state.workKind == WorkSectionKind.Quiet
                    val pausedPkgs = state.pausedPackages.toSet()
                    val searching = searchOpen && query.isNotBlank()
                    val cells = if (searching) {
                        visible.map { app ->
                            app.toCell(state.workIcons[app.key], muted, app.packageName in pausedPkgs)
                        }
                    } else {
                        sectionCells(
                            arranged = arranged,
                            loose = { id ->
                                byKey[id]?.let { app ->
                                    app.toCell(state.workIcons[id], muted, app.packageName in pausedPkgs)
                                }
                            },
                            folder = { group ->
                                val ordered = membersByLabel(group.members) { byKey[it]?.label ?: it }
                                val icons = ordered.map { state.workIcons[it] }
                                val labels = ordered.mapNotNull { byKey[it]?.label }
                                group.toCell(icons, muted, labels)
                            },
                        )
                    }
                    val rows = cells.chunked(4)
                    items(
                        items = rows,
                        key = { row -> "$pageKey:w:" + row.joinToString("|") { it.key } },
                    ) { row ->
                        val rowKey = "$pageKey:w:" + row.joinToString("|") { it.key }
                        AppRow(
                            cells = row,
                            section = GroupSection.WORK,
                            rowKey = rowKey,
                            iconEpoch = state.workIconEpoch,
                            namesOnly = state.namesOnly,
                            drag = drag,
                            dragApps = !searching,
                            fromGroupId = null,
                            modifier = Modifier.padding(horizontal = FocoSpace.page),
                            onClick = { cell ->
                                if (cell.groupId != null) {
                                    onOpenGroup(cell.groupId)
                                } else if (muted) {
                                    onQuietTap()
                                } else {
                                    byKey[cell.id]?.let(onLaunchWork)
                                }
                            },
                            onLongClick = { cell ->
                                if (cell.groupId != null) {
                                    onGroupSheet(cell.groupId)
                                } else {
                                    byKey[cell.id]?.let(onWorkSheet)
                                }
                            },
                            onEnsureIcon = { cell ->
                                if (cell.groupId != null) {
                                    arranged.groups
                                        .find { it.id == cell.groupId }
                                        ?.members
                                        ?.let { ids -> membersByLabel(ids) { byKey[it]?.label ?: it } }
                                        ?.take(4)
                                        ?.forEach(onEnsureWorkIcon)
                                } else {
                                    onEnsureWorkIcon(cell.id)
                                }
                            },
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                        )
                    }
                    if (!searching) {
                        item(key = "$pageKey:work-pad") {
                            GridPad(drag = drag, section = GroupSection.WORK, rowKey = "$pageKey:work-pad")
                        }
                    }
                }
            }
        }
        item(key = "$pageKey:escape") {
            EscapePad(onOpenSystemSettings)
        }
    }
}

@Composable
private fun pageLabel(page: HomePageSpec): String {
    if (page.label.isNotBlank()) return page.label
    return stringResource(
        when (page.type) {
            HomePages.TYPE_CLOCK -> R.string.page_clock
            HomePages.TYPE_AGENDA -> R.string.page_agenda
            HomePages.TYPE_PERSONAL -> R.string.section_personal
            HomePages.TYPE_DIET -> R.string.page_diet
            HomePages.TYPE_WORK -> R.string.section_work
            else -> R.string.home_page_apps
        },
    )
}

@Composable
private fun HomePagerCue(
    labels: List<String>,
    page: Int,
    showLabel: Boolean,
    onSelect: (Int) -> Unit,
    onEdit: () -> Unit,
) {
    val safePage = page.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
    val current = labels.getOrNull(safePage).orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onEdit)
            .padding(top = FocoSpace.hair, bottom = FocoSpace.gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            labels.forEachIndexed { index, label ->
                val selected = index == safePage
                Box(
                    modifier = Modifier
                        .size(FocoSpace.touch)
                        .clickable { onSelect(index) }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (selected) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (selected) FocoPaper else FocoLine),
                    )
                }
            }
        }
        if (showLabel && current.isNotBlank()) {
            Text(
                text = current,
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun PagePause(
    paused: Boolean,
    idle: String,
    active: String,
    onClick: () -> Unit,
) {
    val label = if (paused) active else idle
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocoSpace.page),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(FocoSpace.touch)
                .clip(RoundedCornerShape(50))
                .background(if (paused) FocoInkElevated else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = FocoSpace.gapLg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (paused) FocoPaper else FocoPaperDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { contentDescription = label },
            )
        }
    }
}

@Composable
private fun EscapePad(onOpenSystemSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .homeLongPress(onOpenSystemSettings),
    )
}


@Composable
private fun HomeOverflow(
    showWorkRefresh: Boolean,
    showWorkPause: Boolean,
    workPaused: Boolean,
    onToggleWork: () -> Unit,
    onAddApps: () -> Unit,
    onEditApps: () -> Unit,
    onOpenFocoSettings: () -> Unit,
    onChooseDefault: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRefreshWork: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(menu, pending) {
        if (menu || pending == null) return@LaunchedEffect
        val action = pending
        pending = null
        action?.invoke()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.home_overflow),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = FocoInkElevated,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                OverflowItem(R.string.menu_add) { pending = onAddApps; menu = false }
                OverflowItem(R.string.menu_edit) { pending = onEditApps; menu = false }
                OverflowItem(R.string.menu_settings) { pending = onOpenFocoSettings; menu = false }
                OverflowItem(R.string.menu_default) { pending = onChooseDefault; menu = false }
                OverflowItem(R.string.menu_system) { pending = onOpenSystemSettings; menu = false }
                if (showWorkPause) {
                    OverflowItem(if (workPaused) R.string.work_resume else R.string.work_pause) {
                        pending = onToggleWork
                        menu = false
                    }
                }
                if (showWorkRefresh) {
                    OverflowItem(R.string.menu_refresh_work) { pending = onRefreshWork; menu = false }
                }
            }
        }
    }
}

@Composable
private fun OverflowItem(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = onClick,
    )
}

@Composable
private fun NotDefaultBanner(onChooseDefault: () -> Unit) {
    BannerRow(
        message = stringResource(R.string.banner_not_default),
        action = stringResource(R.string.banner_choose),
        onAction = onChooseDefault,
    )
}

@Composable
private fun NlsOffBanner(disconnected: Boolean, onOpenAvisos: () -> Unit) {
    BannerRow(
        message = stringResource(
            if (disconnected) R.string.nls_banner_disconnected else R.string.nls_banner,
        ),
        action = stringResource(R.string.nls_banner_cta),
        onAction = onOpenAvisos,
    )
}

@Composable
private fun BannerRow(message: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = FocoSpace.page),
        )
        FocoTextButton(onClick = onAction) {
            Text(action)
        }
    }
}

@Composable
private fun WorkHeader(
    kind: WorkSectionKind,
    query: String,
    showSearch: Boolean,
    showQuietLink: Boolean,
    showTitle: Boolean = true,
    onQuery: (String) -> Unit,
    onOpenWorkSettings: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(modifier = Modifier.padding(horizontal = FocoSpace.page)) {
        if (showTitle) {
            SectionTitle(text = stringResource(R.string.section_work))
        }
        if (WorkCatalogRules.showQuietCopy(kind)) {
            Spacer(Modifier.height(FocoSpace.gap))
            Text(
                text = stringResource(R.string.work_quiet_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (showQuietLink) {
                FocoTextButton(onClick = onOpenWorkSettings) {
                    Text(stringResource(R.string.work_quiet_cta))
                }
            }
        }
        if (showSearch) {
            Spacer(Modifier.height(FocoSpace.gap))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = FocoPaper),
                placeholder = {
                    Text(
                        text = stringResource(R.string.work_search),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocoPaper,
                    unfocusedTextColor = FocoPaper,
                    focusedBorderColor = FocoPaperDim,
                    unfocusedBorderColor = FocoLine,
                    cursorColor = FocoPaper,
                    focusedPlaceholderColor = FocoPaperDim,
                    unfocusedPlaceholderColor = FocoPaperDim,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        }
        Spacer(Modifier.height(FocoSpace.gapLg))
    }
}

@Composable
private fun PersonalEmptyInline(onAddApps: () -> Unit, onOpenSystemSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocoSpace.page)
            .homeLongPress(onOpenSystemSettings),
    ) {
        Text(
            text = stringResource(R.string.home_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        FocoTextButton(onClick = onAddApps) {
            Text(stringResource(R.string.home_add))
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.04.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = FocoPaper,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FocoSpace.page, vertical = FocoSpace.section),
    )
}

private data class HomeCell(
    val id: String,
    val label: String,
    val icon: Bitmap? = null,
    val muted: Boolean = false,
    val paused: Boolean = false,
    val packageName: String? = null,
    val groupId: String? = null,
    val folderIcons: List<Bitmap?> = emptyList(),
    val memberLabels: List<String> = emptyList(),
) {
    val key: String get() = if (groupId != null) "g:$groupId" else id
}

private fun LaunchableApp.toCell(): HomeCell = HomeCell(
    id = packageName,
    label = label,
    icon = icon,
    packageName = packageName,
)

private fun WorkApp.toCell(icon: Bitmap?, muted: Boolean, paused: Boolean = false): HomeCell = HomeCell(
    id = key,
    label = label,
    icon = icon,
    muted = muted,
    paused = paused,
    packageName = packageName,
)

private fun AppGroup.toCell(
    icons: List<Bitmap?>,
    muted: Boolean = false,
    labels: List<String> = emptyList(),
): HomeCell {
    return HomeCell(
        id = id,
        label = name,
        muted = muted,
        groupId = id,
        folderIcons = icons.take(4),
        memberLabels = labels.take(4),
    )
}

/** Apps inside a folder. The folder tile itself stays in the user's group order. */
private fun membersByLabel(ids: List<String>, labelOf: (String) -> String): List<String> {
    return AppOrder.byLabel(ids, labelOf) { it }
}

private fun sectionCells(
    arranged: ArrangedSection,
    loose: (String) -> HomeCell?,
    folder: (AppGroup) -> HomeCell,
): List<HomeCell> {
    val tiles = ArrayList<HomeCell>(arranged.groups.size + arranged.looseIds.size)
    for (group in arranged.groups) tiles += folder(group)
    for (id in arranged.looseIds) {
        loose(id)?.let { tiles += it }
    }
    return tiles
}

@Composable
private fun AppRow(
    cells: List<HomeCell>,
    section: GroupSection,
    rowKey: String,
    iconEpoch: Long,
    namesOnly: Boolean,
    drag: HomeDragState,
    dragApps: Boolean,
    fromGroupId: String?,
    onClick: (HomeCell) -> Unit,
    onLongClick: ((HomeCell) -> Unit)?,
    onEnsureIcon: ((HomeCell) -> Unit)?,
    onDragStart: (HomeCell, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { drag.putRow(rowKey, section, it.toDragBounds()) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cells.forEach { cell ->
            AppCell(
                cell = cell,
                section = section,
                iconEpoch = iconEpoch,
                namesOnly = namesOnly,
                drag = drag,
                dragApps = dragApps,
                fromGroupId = fromGroupId,
                modifier = Modifier.weight(1f),
                onClick = { onClick(cell) },
                onLongClick = onLongClick?.let { callback -> { callback(cell) } },
                onEnsureIcon = onEnsureIcon?.let { callback -> { callback(cell) } },
                onDragStart = { window -> onDragStart(cell, window) },
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
        }
        repeat(4 - cells.size) {
            Spacer(Modifier.weight(1f))
        }
    }
    DisposableEffect(rowKey) {
        onDispose { drag.removeRow(rowKey) }
    }
    Spacer(Modifier.height(FocoSpace.section))
}

@Composable
private fun AppCell(
    cell: HomeCell,
    section: GroupSection,
    iconEpoch: Long,
    namesOnly: Boolean,
    drag: HomeDragState,
    dragApps: Boolean,
    fromGroupId: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onEnsureIcon: (() -> Unit)?,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    if (onEnsureIcon != null) {
        LaunchedEffect(cell.id, iconEpoch) { onEnsureIcon() }
    }
    val tileKey = "$section:${fromGroupId ?: "grid"}:${cell.key}"
    val coords = remember { CoordRef() }
    DisposableEffect(tileKey) {
        onDispose { drag.removeTile(tileKey) }
    }
    val chrome = drag.chrome
    val highlight = highlightFor(chrome, cell.id, cell.groupId)
    val lifted = chrome != null &&
        chrome.section == section &&
        chrome.memberId == cell.id &&
        chrome.fromGroupId == fromGroupId &&
        cell.groupId == null
    val canDrag = dragApps && cell.groupId == null
    val iconAlpha = if (cell.muted || cell.paused) 0.4f else 1f
    Column(
        modifier = modifier
            .then(
                if (canDrag) {
                    Modifier.homeTileGesture(
                        key = tileKey,
                        enabled = true,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onDragStart = { local ->
                            val window = coords.value?.takeIf { it.isAttached }?.localToWindow(local)
                                ?: return@homeTileGesture
                            onDragStart(window)
                        },
                        onDrag = { local ->
                            val window = coords.value?.takeIf { it.isAttached }?.localToWindow(local)
                                ?: return@homeTileGesture
                            onDrag(window)
                        },
                        onDragEnd = onDragEnd,
                    )
                } else {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                },
            )
            .onGloballyPositioned { coords.value = it }
            .padding(vertical = 4.dp)
            .alpha(if (lifted) 0.28f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TileFace(
            cell = cell,
            namesOnly = namesOnly,
            highlight = highlight,
            iconAlpha = iconAlpha,
            modifier = Modifier.onGloballyPositioned { coords ->
                drag.putTile(
                    key = tileKey,
                    tile = GroupDrag.Tile(
                        section = section,
                        id = cell.groupId ?: cell.id,
                        folder = cell.groupId != null,
                        bounds = coords.toDragBounds(),
                    ),
                )
            },
        )
        Spacer(Modifier.height(6.dp))
        val pausedMark = stringResource(R.string.app_paused_mark)
        Text(
            text = if (cell.paused) cell.label + "\n" + pausedMark else cell.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (cell.muted || cell.paused) FocoPaperDim else MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun TileFace(
    cell: HomeCell,
    namesOnly: Boolean,
    highlight: CellHighlight,
    iconAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val ring = when (highlight) {
        CellHighlight.Valid -> FocoPaper
        CellHighlight.Invalid -> FocoPaperDim
        CellHighlight.None -> Color.Transparent
    }
    val shift = remember { Animatable(0f) }
    LaunchedEffect(highlight) {
        if (highlight == CellHighlight.Invalid) {
            shift.snapTo(4f)
            shift.animateTo(0f, tween(90))
        } else {
            shift.snapTo(0f)
        }
    }
    Box(
        modifier = modifier.offset { IntOffset(shift.value.toInt(), 0) },
        contentAlignment = Alignment.Center,
    ) {
        if (highlight == CellHighlight.Valid) {
            Box(
                modifier = Modifier
                    .size(if (namesOnly && cell.groupId == null) 28.dp else 48.dp)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(shape)
                    .background(FocoLine),
            )
        }
        val face = Modifier
            .border(2.dp, ring, shape)
            .alpha(iconAlpha)
        if (cell.groupId != null) {
            FolderIcon(
                icons = cell.folderIcons,
                labels = cell.memberLabels,
                namesOnly = namesOnly,
                description = stringResource(R.string.group_cd, cell.label),
                modifier = face,
            )
        } else if (namesOnly) {
            NameGlyph(
                label = cell.label,
                modifier = face,
            )
        } else {
            val bitmap = cell.icon
            val image = if (bitmap != null) remember(cell.id, bitmap) { bitmap.asImageBitmap() } else null
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = cell.label,
                    modifier = face
                        .size(48.dp)
                        .clip(shape),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = face
                        .size(48.dp)
                        .clip(shape)
                        .background(FocoLine)
                        .semantics { contentDescription = cell.label },
                )
            }
        }
    }
}

@Composable
private fun NameGlyph(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(FocoLine)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tileInitial(label),
            style = MaterialTheme.typography.labelLarge,
            color = FocoPaperDim,
        )
    }
}

@Composable
private fun FolderIcon(
    icons: List<Bitmap?>,
    labels: List<String>,
    namesOnly: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FocoLine)
            .padding(4.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (indexRow in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (column in 0 until 2) {
                        val index = indexRow * 2 + column
                        if (namesOnly) {
                            MiniLetter(labels.getOrNull(index))
                        } else {
                            MiniIcon(icons.getOrNull(index))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLetter(label: String?) {
    Box(
        modifier = Modifier.size(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(
                text = tileInitial(label),
                style = MaterialTheme.typography.labelSmall,
                color = FocoPaper,
            )
        }
    }
}

@Composable
private fun MiniIcon(bitmap: Bitmap?) {
    if (bitmap != null) {
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(FocoInk),
        )
    }
}

@Composable
private fun PersonalAppSheet(
    app: LaunchableApp,
    paused: Boolean,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onAddToGroup: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = FocoSpace.gapLg),
    ) {
        SheetTitle(app.label)
        if (!paused) {
            SheetAction(stringResource(R.string.lp_open), onOpen)
        }
        SheetAction(
            stringResource(if (paused) R.string.app_resume else R.string.app_pause),
            onPause,
        )
        SheetAction(stringResource(R.string.group_add), onAddToGroup)
        SheetAction(stringResource(R.string.lp_remove), onRemove)
    }
}

@Composable
private fun WorkAppSheet(
    app: WorkApp,
    paused: Boolean,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onAddToGroup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = FocoSpace.gapLg),
    ) {
        SheetTitle(app.label)
        if (!paused) {
            SheetAction(stringResource(R.string.lp_open), onOpen)
        }
        SheetAction(
            stringResource(if (paused) R.string.app_resume else R.string.app_pause),
            onPause,
        )
        SheetAction(stringResource(R.string.group_add), onAddToGroup)
    }
}

@Composable
private fun GroupPickSheet(
    label: String,
    groups: List<AppGroup>,
    onPick: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        SheetTitle(label)
        for (group in groups) {
            SheetAction(group.name) { onPick(group.id) }
        }
        SheetAction(stringResource(R.string.group_new), onCreate)
    }
}

@Composable
private fun OpenGroupSheet(
    group: AppGroup,
    personalApps: List<LaunchableApp>,
    workApps: List<WorkApp>,
    workIcons: Map<String, Bitmap>,
    workMuted: Boolean,
    onLaunchPersonal: (String) -> Unit,
    onLaunchWork: (WorkApp) -> Unit,
    onRemoveMember: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        SheetTitle(group.name)
        when (group.section) {
            GroupSection.PERSONAL -> {
                val byPkg = personalApps.associateBy { it.packageName }
                val ordered = membersByLabel(group.members) { byPkg[it]?.label ?: it }
                for (member in ordered) {
                    val app = byPkg[member] ?: continue
                    MemberRow(
                        label = app.label,
                        icon = app.icon,
                        onOpen = { onLaunchPersonal(app.packageName) },
                        onRemove = { onRemoveMember(member) },
                    )
                }
            }
            GroupSection.WORK -> {
                val byKey = workApps.associateBy { it.key }
                val ordered = membersByLabel(group.members) { byKey[it]?.label ?: it }
                for (member in ordered) {
                    val app = byKey[member] ?: continue
                    MemberRow(
                        label = app.label,
                        icon = workIcons[member],
                        muted = workMuted,
                        onOpen = { onLaunchWork(app) },
                        onRemove = { onRemoveMember(member) },
                    )
                }
            }
        }
        SheetAction(stringResource(R.string.group_rename), onRename)
        SheetAction(stringResource(R.string.group_delete), onDelete)
    }
}

@Composable
private fun MemberRow(
    label: String,
    icon: Bitmap?,
    muted: Boolean = false,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = FocoSpace.page, vertical = FocoSpace.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alpha = if (muted) 0.4f else 1f
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .alpha(alpha),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FocoLine)
                    .alpha(alpha),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = FocoPaper,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        FocoTextButton(onClick = onRemove) {
            Text(stringResource(R.string.group_remove))
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = FocoPaper,
        modifier = Modifier.padding(horizontal = FocoSpace.page, vertical = FocoSpace.gap),
    )
}

@Composable
private fun GroupNameDialog(
    create: Boolean,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial) }
    val clean = GroupMutations.normalizeName(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = stringResource(if (create) R.string.group_name_title else R.string.group_rename),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(GroupMutations.NAME_MAX) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = FocoPaper),
                placeholder = {
                    Text(
                        text = stringResource(R.string.group_name_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { clean?.let(onConfirm) }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocoPaper,
                    unfocusedTextColor = FocoPaper,
                    focusedBorderColor = FocoPaperDim,
                    unfocusedBorderColor = FocoLine,
                    cursorColor = FocoPaper,
                    focusedPlaceholderColor = FocoPaperDim,
                    unfocusedPlaceholderColor = FocoPaperDim,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        },
        confirmButton = {
            FocoTextButton(onClick = { clean?.let(onConfirm) }, enabled = clean != null) {
                Text(stringResource(if (create) R.string.group_create else R.string.group_save))
            }
        },
        dismissButton = {
            FocoTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.group_cancel))
            }
        },
    )
}

private sealed interface HomeSheet {
    data class Personal(val app: LaunchableApp) : HomeSheet
    data class Work(val app: WorkApp) : HomeSheet
    data class Pick(val section: GroupSection, val memberId: String, val label: String) : HomeSheet
    data class Open(val groupId: String) : HomeSheet
}

private data class GroupNameRequest(
    val section: GroupSection,
    val memberId: String?,
    val groupId: String?,
    val initial: String,
)

@Composable
internal fun Modifier.homeLongPress(onLongClick: () -> Unit): Modifier {
    return this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
        onLongClick = onLongClick,
    )
}

@Composable
private fun GridPad(drag: HomeDragState, section: GroupSection, rowKey: String) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .onGloballyPositioned { drag.putRow(rowKey, section, it.toDragBounds()) },
    )
    DisposableEffect(rowKey) {
        onDispose { drag.removeRow(rowKey) }
    }
}

@Composable
private fun OpenFolderOverlay(
    group: AppGroup,
    personalApps: List<LaunchableApp>,
    workApps: List<WorkApp>,
    workIcons: Map<String, Bitmap>,
    namesOnly: Boolean,
    workMuted: Boolean,
    drag: HomeDragState,
    onDismiss: () -> Unit,
    onPanelBounds: (GroupDrag.Bounds) -> Unit,
    onLaunchPersonal: (String) -> Unit,
    onLaunchWork: (WorkApp) -> Unit,
    onLongClick: () -> Unit,
    onDragStart: (HomeCell, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onEnsureMember: ((String) -> Unit)?,
) {
    val scrimAlpha = if (drag.chrome?.fromGroupId == group.id) 0.35f else 0.72f
    val cells = when (group.section) {
        GroupSection.PERSONAL -> {
            val byPkg = personalApps.associateBy { it.packageName }
            membersByLabel(group.members) { byPkg[it]?.label ?: it }
                .mapNotNull { id -> byPkg[id]?.toCell() }
        }
        GroupSection.WORK -> {
            val byKey = workApps.associateBy { it.key }
            membersByLabel(group.members) { byKey[it]?.label ?: it }
                .mapNotNull { id -> byKey[id]?.toCell(workIcons[id], workMuted) }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(3f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FocoInk.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FocoInkElevated)
                .onGloballyPositioned { onPanelBounds(it.toDragBounds()) }
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaper,
                modifier = Modifier.padding(horizontal = FocoSpace.page, vertical = FocoSpace.gap),
            )
            cells.chunked(4).forEach { row ->
                AppRow(
                    cells = row,
                    section = group.section,
                    rowKey = "open:${group.id}:" + row.joinToString("|") { it.key },
                    iconEpoch = 0L,
                    namesOnly = namesOnly,
                    drag = drag,
                    dragApps = true,
                    fromGroupId = group.id,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    onClick = { cell ->
                        if (group.section == GroupSection.PERSONAL) {
                            onLaunchPersonal(cell.id)
                        } else {
                            workApps.find { it.key == cell.id }?.let(onLaunchWork)
                        }
                    },
                    onLongClick = { onLongClick() },
                    onEnsureIcon = onEnsureMember?.let { ensure -> { cell -> ensure(cell.id) } },
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                )
            }
        }
    }
}

@Composable
private fun HomeDragGhost(drag: HomeDragState, namesOnly: Boolean, origin: Offset) {
    val pose = drag.ghost ?: return
    DragGhost(pose = pose, namesOnly = namesOnly, origin = origin)
}

@Composable
private fun DragGhost(pose: GhostPose, namesOnly: Boolean, origin: Offset) {
    val shape = RoundedCornerShape(12.dp)
    val x = pose.pointerX - origin.x - pose.iconWidth / 2f
    val y = pose.pointerY - origin.y - pose.iconHeight / 2f
    Box(
        modifier = Modifier
            .zIndex(5f)
            .offset { IntOffset(x.toInt(), y.toInt()) }
            .graphicsLayer {
                scaleX = 1.06f
                scaleY = 1.06f
                shadowElevation = 8.dp.toPx()
                this.shape = shape
                clip = false
            },
    ) {
        if (namesOnly) {
            NameGlyph(label = pose.label)
        } else {
            val bitmap = pose.icon
            val image = if (bitmap != null) remember(bitmap) { bitmap.asImageBitmap() } else null
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(shape),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(shape)
                        .background(FocoLine),
                )
            }
        }
    }
}
