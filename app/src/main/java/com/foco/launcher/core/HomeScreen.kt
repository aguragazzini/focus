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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.foco.launcher.R
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
    onRefreshWork: () -> Unit,
    onRemovePersonal: (String) -> Unit,
    onEnsureWorkIcon: (String) -> Unit,
    onOpenWorkSettings: () -> Unit,
    onQuietTap: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var sheetApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var confirmApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var workQuery by rememberSaveable { mutableStateOf("") }

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
            StatusStrip(state = state, onOpenFocoSettings = onOpenFocoSettings)
            when (state.banner) {
                HomeBanner.NotDefault -> NotDefaultBanner(onChooseDefault)
                HomeBanner.Nls -> NlsOffBanner(onOpenAvisos)
                HomeBanner.None -> Unit
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
            ) {
                item(key = "personal-header") {
                    PersonalHeader(
                        hasApps = state.apps.isNotEmpty(),
                        onEdit = onEditApps,
                        onAdd = onAddApps,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (state.apps.isEmpty()) {
                    item(key = "personal-empty") {
                        PersonalEmptyInline(onOpenSystemSettings = onOpenSystemSettings)
                    }
                } else {
                    val rows = state.apps.map { it.toCell() }.chunked(4)
                    items(
                        items = rows,
                        key = { row -> "p:" + row.joinToString("|") { it.id } },
                    ) { row ->
                        AppRow(
                            cells = row,
                            iconEpoch = 0L,
                            onClick = { cell -> onLaunch(cell.id) },
                            onLongClick = { cell ->
                                state.apps.firstOrNull { it.packageName == cell.id }?.let { sheetApp = it }
                            },
                            onEnsureIcon = null,
                        )
                    }
                }
                if (state.workKind != WorkSectionKind.Hidden) {
                    item(key = "work-divider") {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                            thickness = 1.dp,
                            color = FocoLine,
                        )
                    }
                    item(key = "work-header") {
                        WorkHeader(
                            kind = state.workKind,
                            count = WorkCatalogRules.headerCount(state.workApps.size),
                            refreshing = state.workRefreshing,
                            query = if (searchOpen) workQuery else "",
                            showSearch = searchOpen,
                            showQuietLink = state.workLink,
                            onQuery = { workQuery = it },
                            onRefresh = onRefreshWork,
                            onOpenWorkSettings = onOpenWorkSettings,
                        )
                    }
                    when {
                        WorkCatalogRules.showErrorCopy(state.workKind) -> {
                            item(key = "work-error") {
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
                        WorkCatalogRules.showWorkEmptyCopy(state.workKind) -> {
                            item(key = "work-empty") {
                                Text(
                                    text = stringResource(R.string.work_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        searchOpen && workQuery.isNotBlank() && workVisible.isEmpty() -> {
                            item(key = "work-search-empty") {
                                Text(
                                    text = stringResource(R.string.work_search_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        WorkCatalogRules.showWorkGrid(state.workKind, workVisible.size) -> {
                            val byKey = state.workApps.associateBy { it.key }
                            val muted = state.workKind == WorkSectionKind.Quiet
                            val rows = workVisible.map { app ->
                                app.toCell(state.workIcons[app.key], muted)
                            }.chunked(4)
                            items(
                                items = rows,
                                key = { row -> "w:" + row.joinToString("|") { it.id } },
                            ) { row ->
                                AppRow(
                                    cells = row,
                                    iconEpoch = state.workIconEpoch,
                                    onClick = { cell ->
                                        if (muted) {
                                            onQuietTap()
                                        } else {
                                            byKey[cell.id]?.let(onLaunchWork)
                                        }
                                    },
                                    onLongClick = null,
                                    onEnsureIcon = { cell -> onEnsureWorkIcon(cell.id) },
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

    val pressed = sheetApp
    if (pressed != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetApp = null },
            containerColor = FocoInkElevated,
            contentColor = FocoPaper,
            tonalElevation = 0.dp,
            dragHandle = { BottomSheetDefaults.DragHandle(color = FocoPaperDim) },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text(
                    text = pressed.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FocoPaper,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                SheetAction(
                    label = stringResource(R.string.lp_remove),
                    onClick = {
                        val target = pressed
                        sheetApp = null
                        val settingsPkg = SuggestedApps.settingsPackage(context)
                        if (LaunchpadRules.needsSettingsConfirm(target.packageName, settingsPkg)) {
                            confirmApp = target
                        } else {
                            onRemovePersonal(target.packageName)
                        }
                    },
                )
                SheetAction(
                    label = stringResource(R.string.lp_open),
                    onClick = {
                        val pkg = pressed.packageName
                        sheetApp = null
                        onLaunch(pkg)
                    },
                )
            }
        }
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
                OverflowItem(R.string.menu_add, { menu = false; onAddApps() })
                OverflowItem(R.string.menu_edit, { menu = false; onEditApps() })
                OverflowItem(R.string.menu_settings, { menu = false; onOpenFocoSettings() })
                OverflowItem(R.string.menu_default, { menu = false; onChooseDefault() })
                OverflowItem(R.string.menu_system, { menu = false; onOpenSystemSettings() })
                if (showWorkRefresh) {
                    OverflowItem(R.string.menu_refresh_work, { menu = false; onRefreshWork() })
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
private fun StatusStrip(state: HomeUiState, onOpenFocoSettings: () -> Unit) {
    val filterWord = stringResource(
        if (state.filterActive) R.string.strip_filter_on else R.string.strip_filter_off,
    )
    val workWord = stringResource(
        when (state.workPresence) {
            WorkPresence.Unknown -> R.string.strip_work_unknown
            WorkPresence.Yes -> R.string.strip_work_yes
            WorkPresence.No -> R.string.strip_work_no
            WorkPresence.Paused -> R.string.strip_work_pause
        },
    )
    val parts = buildList {
        add(stringResource(R.string.strip_personal, state.apps.size))
        add(stringResource(R.string.strip_filter, filterWord))
        add(stringResource(R.string.strip_work, workWord))
        if (LaunchpadRules.showBioCell(state.showBio)) {
            val bio = if (state.bioOnCount > 0) {
                stringResource(R.string.strip_bio_on, state.bioOnCount)
            } else {
                stringResource(R.string.strip_bio_off)
            }
            add(stringResource(R.string.strip_bio, bio))
        }
    }
    val line = LaunchpadRules.joinStatus(parts)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 2.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FocoInkElevated)
            .clickable(onClick = onOpenFocoSettings)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = FocoPaperDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
private fun NlsOffBanner(onOpenAvisos: () -> Unit) {
    BannerRow(
        message = stringResource(R.string.nls_banner),
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
private fun PersonalHeader(hasApps: Boolean, onEdit: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            text = stringResource(R.string.section_personal),
            modifier = Modifier.weight(1f),
        )
        HeaderAction(
            text = stringResource(if (hasApps) R.string.personal_edit else R.string.home_add),
            onClick = if (hasApps) onEdit else onAdd,
        )
    }
}

@Composable
private fun WorkHeader(
    kind: WorkSectionKind,
    count: Int,
    refreshing: Boolean,
    query: String,
    showSearch: Boolean,
    showQuietLink: Boolean,
    onQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenWorkSettings: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.section_work),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.04.em,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Outlined.Work,
            contentDescription = stringResource(R.string.cd_work_badge),
            modifier = Modifier
                .padding(start = 6.dp)
                .size(18.dp),
            tint = FocoPaperDim,
        )
        if (WorkCatalogRules.showWorkCount(kind)) {
            Text(
                text = stringResource(R.string.work_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = FocoPaperDim,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.work_refresh),
                    modifier = Modifier.size(20.dp),
                    tint = FocoPaperDim,
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.work_sub),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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

@Composable
private fun PersonalEmptyInline(onOpenSystemSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().longPressEmpty(onOpenSystemSettings)) {
        Text(
            text = stringResource(R.string.home_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FocoTextButton(onClick = onOpenSystemSettings) {
            Text(stringResource(R.string.home_system_settings))
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
private fun HeaderAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = FocoPaperDim,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
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
    val icon: Bitmap?,
    val muted: Boolean = false,
)

private fun LaunchableApp.toCell(): HomeCell = HomeCell(packageName, label, icon)

private fun WorkApp.toCell(icon: Bitmap?, muted: Boolean): HomeCell = HomeCell(key, label, icon, muted)

@Composable
private fun AppRow(
    cells: List<HomeCell>,
    iconEpoch: Long,
    onClick: (HomeCell) -> Unit,
    onLongClick: ((HomeCell) -> Unit)?,
    onEnsureIcon: ((HomeCell) -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        if (image != null) {
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
private fun Modifier.longPressEmpty(onLongClick: () -> Unit): Modifier {
    return this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
        onLongClick = onLongClick,
    )
}
