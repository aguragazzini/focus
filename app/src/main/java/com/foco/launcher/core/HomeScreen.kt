@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.foco.launcher.core

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.foco.launcher.R
import com.foco.launcher.notification.NlsRecovery
import com.foco.launcher.registry.AppGroup
import com.foco.launcher.registry.ArrangedSection
import com.foco.launcher.registry.GroupLayout
import com.foco.launcher.registry.GroupMutations
import com.foco.launcher.registry.GroupSection
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.SuggestedApps
import com.foco.launcher.work.WorkApp
import com.foco.launcher.work.WorkCatalogRules
import com.foco.launcher.work.WorkSectionKind

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
            Spacer(Modifier.height(12.dp))
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
    onRefreshWork: () -> Unit,
    onRemovePersonal: (String) -> Unit,
    onCreateGroup: (GroupSection, String, String) -> Unit,
    onAddToGroup: (String, String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onRemoveFromGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onEnsureWorkIcon: (String) -> Unit,
    onOpenWorkSettings: () -> Unit,
    onQuietTap: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    var confirmApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var naming by remember { mutableStateOf<GroupNameRequest?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var workQuery by rememberSaveable { mutableStateOf("") }

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

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
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
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            HomeOverflow(
                showWorkRefresh = state.workKind != WorkSectionKind.Hidden,
                onAddApps = onAddApps,
                onEditApps = onEditApps,
                onOpenFocoSettings = onOpenFocoSettings,
                onChooseDefault = onChooseDefault,
                onOpenSystemSettings = onOpenSystemSettings,
                onRefreshWork = onRefreshWork,
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                item(key = "clock") {
                    Spacer(Modifier.height(12.dp))
                    HomeClock(
                        onOpenClock = onOpenClock,
                        onOpenCalendar = onOpenCalendar,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        readGlance = { HomeGlance.line(context) },
                    )
                    Spacer(Modifier.height(32.dp))
                }
                item(key = "banner") {
                    when (state.banner) {
                        HomeBanner.NotDefault -> NotDefaultBanner(onChooseDefault)
                        HomeBanner.Nls -> NlsOffBanner(
                            disconnected = state.nlsAttention == NlsRecovery.Attention.Disconnected,
                            onOpenAvisos = onOpenAvisos,
                        )
                        HomeBanner.None -> Unit
                    }
                    if (state.banner != HomeBanner.None) {
                        Spacer(Modifier.height(16.dp))
                    }
                }
                item(key = "personal-header") {
                    SectionTitle(
                        text = stringResource(R.string.section_personal),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (personalArranged.groups.isEmpty() && personalArranged.looseIds.isEmpty()) {
                    item(key = "personal-empty") {
                        PersonalEmptyInline(
                            onAddApps = onAddApps,
                            onOpenSystemSettings = onOpenSystemSettings,
                        )
                    }
                } else {
                    val personalByPkg = state.apps.associateBy { it.packageName }
                    val rows = sectionCells(
                        arranged = personalArranged,
                        loose = { id -> personalByPkg[id]?.toCell() },
                        folder = { group ->
                            val members = group.members.mapNotNull { personalByPkg[it] }
                            group.toCell(members.map { it.icon })
                        },
                    ).chunked(4)
                    items(
                        items = rows,
                        key = { row -> "p:" + row.joinToString("|") { it.key } },
                    ) { row ->
                        AppRow(
                            cells = row,
                            iconEpoch = 0L,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            onClick = { cell ->
                                if (cell.groupId != null) {
                                    sheet = HomeSheet.Open(cell.groupId)
                                } else {
                                    onLaunch(cell.id)
                                }
                            },
                            onLongClick = { cell ->
                                if (cell.groupId != null) {
                                    sheet = HomeSheet.Open(cell.groupId)
                                } else {
                                    personalByPkg[cell.id]?.let { sheet = HomeSheet.Personal(it) }
                                }
                            },
                            onEnsureIcon = null,
                        )
                    }
                }
                if (state.workKind != WorkSectionKind.Hidden) {
                    item(key = "work-divider") {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 1.dp,
                            color = FocoLine,
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                    item(key = "work-header") {
                        WorkHeader(
                            kind = state.workKind,
                            query = if (searchOpen) workQuery else "",
                            showSearch = searchOpen,
                            showQuietLink = state.workLink,
                            onQuery = { workQuery = it },
                            onOpenWorkSettings = onOpenWorkSettings,
                        )
                    }
                    when {
                        WorkCatalogRules.showErrorCopy(state.workKind) -> {
                            item(key = "work-error") {
                                Column(Modifier.padding(horizontal = 24.dp)) {
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
                            item(key = "work-empty") {
                                Text(
                                    text = stringResource(R.string.work_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                        }
                        searchOpen && workQuery.isNotBlank() && workVisible.isEmpty() -> {
                            item(key = "work-search-empty") {
                                Text(
                                    text = stringResource(R.string.work_search_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                        }
                        WorkCatalogRules.showWorkGrid(
                            state.workKind,
                            if (searchOpen && workQuery.isNotBlank()) {
                                workVisible.size
                            } else {
                                workArranged.groups.size + workArranged.looseIds.size
                            },
                        ) -> {
                            val byKey = state.workApps.associateBy { it.key }
                            val muted = state.workKind == WorkSectionKind.Quiet
                            val searching = searchOpen && workQuery.isNotBlank()
                            val cells = if (searching) {
                                workVisible.map { app -> app.toCell(state.workIcons[app.key], muted) }
                            } else {
                                sectionCells(
                                    arranged = workArranged,
                                    loose = { id ->
                                        byKey[id]?.toCell(state.workIcons[id], muted)
                                    },
                                    folder = { group ->
                                        val icons = group.members.map { state.workIcons[it] }
                                        group.toCell(icons, muted)
                                    },
                                )
                            }
                            val rows = cells.chunked(4)
                            items(
                                items = rows,
                                key = { row -> "w:" + row.joinToString("|") { it.key } },
                            ) { row ->
                                AppRow(
                                    cells = row,
                                    iconEpoch = state.workIconEpoch,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    onClick = { cell ->
                                        if (cell.groupId != null) {
                                            sheet = HomeSheet.Open(cell.groupId)
                                        } else if (muted) {
                                            onQuietTap()
                                        } else {
                                            byKey[cell.id]?.let(onLaunchWork)
                                        }
                                    },
                                    onLongClick = { cell ->
                                        if (cell.groupId != null) {
                                            sheet = HomeSheet.Open(cell.groupId)
                                        } else {
                                            byKey[cell.id]?.let { sheet = HomeSheet.Work(it) }
                                        }
                                    },
                                    onEnsureIcon = { cell ->
                                        if (cell.groupId != null) {
                                            workArranged.groups
                                                .find { it.id == cell.groupId }
                                                ?.members
                                                ?.take(4)
                                                ?.forEach(onEnsureWorkIcon)
                                        } else {
                                            onEnsureWorkIcon(cell.id)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                item(key = "escape") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .longPressEmpty(onOpenSystemSettings),
                    )
                }
            }
        }

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
                    onOpen = {
                        val pkg = activeSheet.app.packageName
                        sheet = null
                        onLaunch(pkg)
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
                    onOpen = {
                        val target = activeSheet.app
                        sheet = null
                        if (state.workKind == WorkSectionKind.Quiet) onQuietTap() else onLaunchWork(target)
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

@Composable
private fun HomeOverflow(
    showWorkRefresh: Boolean,
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
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                OverflowItem(R.string.menu_add) { pending = onAddApps; menu = false }
                OverflowItem(R.string.menu_edit) { pending = onEditApps; menu = false }
                OverflowItem(R.string.menu_settings) { pending = onOpenFocoSettings; menu = false }
                OverflowItem(R.string.menu_default) { pending = onChooseDefault; menu = false }
                OverflowItem(R.string.menu_system) { pending = onOpenSystemSettings; menu = false }
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
                .padding(start = 24.dp),
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
    onQuery: (String) -> Unit,
    onOpenWorkSettings: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        SectionTitle(text = stringResource(R.string.section_work))
        if (WorkCatalogRules.showQuietCopy(kind)) {
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PersonalEmptyInline(onAddApps: () -> Unit, onOpenSystemSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .longPressEmpty(onOpenSystemSettings),
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
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.04.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

private data class HomeCell(
    val id: String,
    val label: String,
    val icon: Bitmap? = null,
    val muted: Boolean = false,
    val groupId: String? = null,
    val folderIcons: List<Bitmap?> = emptyList(),
) {
    val key: String get() = if (groupId != null) "g:$groupId" else id
}

private fun LaunchableApp.toCell(): HomeCell = HomeCell(packageName, label, icon)

private fun WorkApp.toCell(icon: Bitmap?, muted: Boolean): HomeCell = HomeCell(key, label, icon, muted)

private fun AppGroup.toCell(icons: List<Bitmap?>, muted: Boolean = false): HomeCell {
    return HomeCell(
        id = id,
        label = name,
        muted = muted,
        groupId = id,
        folderIcons = icons.take(4),
    )
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
    iconEpoch: Long,
    onClick: (HomeCell) -> Unit,
    onLongClick: ((HomeCell) -> Unit)?,
    onEnsureIcon: ((HomeCell) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cells.forEach { cell ->
            AppCell(
                cell = cell,
                iconEpoch = iconEpoch,
                modifier = Modifier.weight(1f),
                onClick = { onClick(cell) },
                onLongClick = onLongClick?.let { callback -> { callback(cell) } },
                onEnsureIcon = onEnsureIcon?.let { callback -> { callback(cell) } },
            )
        }
        repeat(4 - cells.size) {
            Spacer(Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun AppCell(
    cell: HomeCell,
    iconEpoch: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onEnsureIcon: (() -> Unit)?,
) {
    if (onEnsureIcon != null) {
        LaunchedEffect(cell.id, iconEpoch) { onEnsureIcon() }
    }
    val bitmap = cell.icon
    val image = if (bitmap != null) remember(cell.id, bitmap) { bitmap.asImageBitmap() } else null
    val iconAlpha = if (cell.muted) 0.4f else 1f
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (cell.groupId != null) {
            FolderIcon(
                icons = cell.folderIcons,
                description = stringResource(R.string.group_cd, cell.label),
                alpha = iconAlpha,
            )
        } else if (image != null) {
            Image(
                bitmap = image,
                contentDescription = cell.label,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .alpha(iconAlpha),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FocoLine)
                    .alpha(iconAlpha),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = cell.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (cell.muted) FocoPaperDim else MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FolderIcon(icons: List<Bitmap?>, description: String, alpha: Float) {
    val slots = List(4) { index -> icons.getOrNull(index) }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FocoLine)
            .alpha(alpha)
            .padding(4.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (row in slots.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (icon in row) {
                        MiniIcon(icon)
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniIcon(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
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
    onOpen: () -> Unit,
    onAddToGroup: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        SheetTitle(app.label)
        SheetAction(stringResource(R.string.lp_open), onOpen)
        SheetAction(stringResource(R.string.group_add), onAddToGroup)
        SheetAction(stringResource(R.string.lp_remove), onRemove)
    }
}

@Composable
private fun WorkAppSheet(
    app: WorkApp,
    onOpen: () -> Unit,
    onAddToGroup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        SheetTitle(app.label)
        SheetAction(stringResource(R.string.lp_open), onOpen)
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
                for (member in group.members) {
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
                for (member in group.members) {
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
            .padding(horizontal = 24.dp, vertical = 8.dp),
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
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
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
private fun Modifier.longPressEmpty(onLongClick: () -> Unit): Modifier {
    return this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
        onLongClick = onLongClick,
    )
}
