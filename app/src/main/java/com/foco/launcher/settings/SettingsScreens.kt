package com.foco.launcher.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.foco.launcher.R
import com.foco.launcher.core.FocoInk
import com.foco.launcher.core.FocoLine
import com.foco.launcher.core.FocoPaper
import com.foco.launcher.core.FocoPaperDim
import com.foco.launcher.core.FocoPrimaryButton
import com.foco.launcher.core.FocoSpace
import com.foco.launcher.core.FocoTextButton
import com.foco.launcher.core.LaunchpadRules
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.security.PinSettingsRow
import com.foco.launcher.work.WorkCatalogRules
import com.foco.launcher.work.WorkSettingsStatus

@Composable
fun SettingsHost(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenAvisos: () -> Unit,
    onChooseDefault: () -> Unit,
    onOpenDefaultApps: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onToggleAdd: (String) -> Unit,
    onConfirmAdd: () -> Unit,
    onRemove: (LaunchableApp) -> Unit,
    onConfirmRemove: () -> Unit,
    onDismissRemove: () -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onToggleFilter: (Boolean) -> Unit,
    onOpenNlsSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onSkipNls: () -> Unit,
    onSetNotifAllowed: (String, Boolean) -> Unit,
    onNlsMessageShown: () -> Unit,
    onRefreshWork: () -> Unit,
    onOpenWorkSettings: () -> Unit,
    onWorkPaused: (Boolean) -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onPhonePaused: (Boolean) -> Unit,
    onNamesOnly: (Boolean) -> Unit,
    onEditHome: () -> Unit,
) {
    BackHandler(onBack = onBack)
    when (state.dest) {
        SettingsDest.Main -> SettingsMain(
            state = state,
            onBack = onBack,
            onOpenEdit = onOpenEdit,
            onOpenAvisos = onOpenAvisos,
            onChooseDefault = onChooseDefault,
            onOpenDefaultApps = onOpenDefaultApps,
            onOpenSystemSettings = onOpenSystemSettings,
            onRefreshWork = onRefreshWork,
            onOpenWorkSettings = onOpenWorkSettings,
            onWorkPaused = onWorkPaused,
            onNotificationsPaused = onNotificationsPaused,
            onPhonePaused = onPhonePaused,
            onNamesOnly = onNamesOnly,
            onEditHome = onEditHome,
        )
        SettingsDest.Edit -> EditAppsScreen(
            state = state,
            onBack = onBack,
            onOpenAdd = onOpenAdd,
            onRemove = onRemove,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )
        SettingsDest.Add -> AddAppsScreen(
            state = state,
            onBack = onBack,
            onToggleAdd = onToggleAdd,
            onConfirmAdd = onConfirmAdd,
        )
        SettingsDest.Avisos -> AvisosScreen(
            state = state,
            onBack = onBack,
            onToggleFilter = onToggleFilter,
            onOpenNlsSettings = onOpenNlsSettings,
            onOpenAppInfo = onOpenAppInfo,
            onSetNotifAllowed = onSetNotifAllowed,
            onNlsMessageShown = onNlsMessageShown,
        )
        SettingsDest.NlsOnboarding -> NlsOnboardingScreen(
            showReturnedUngranted = state.nlsShowRestrictedReturn,
            failMessage = state.nlsMessage,
            onOpenSettings = onOpenNlsSettings,
            onOpenAppInfo = onOpenAppInfo,
            onSkip = onSkipNls,
        )
    }
    if (state.removeCandidate != null) {
        RemoveDialog(
            isLast = state.removeIsLast,
            isSettings = state.removeIsSettings,
            onConfirm = onConfirmRemove,
            onDismiss = onDismissRemove,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMain(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenAvisos: () -> Unit,
    onChooseDefault: () -> Unit,
    onOpenDefaultApps: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRefreshWork: () -> Unit,
    onOpenWorkSettings: () -> Unit,
    onWorkPaused: (Boolean) -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onPhonePaused: (Boolean) -> Unit,
    onNamesOnly: (Boolean) -> Unit,
    onEditHome: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_apps),
                    subtitle = stringResource(R.string.settings_apps_count, state.whitelist.size),
                    onClick = onOpenEdit,
                )
                HorizontalDivider()
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_look),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.look_no_icons),
                        subtitle = stringResource(R.string.look_no_icons_sub),
                        checked = state.namesOnly,
                        onCheckedChange = onNamesOnly,
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_home),
                        subtitle = stringResource(R.string.settings_home_sub),
                        onClick = onEditHome,
                    )
                    PinSettingsRow()
                }
                HorizontalDivider()
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_avisos_block),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Text(
                        text = stringResource(
                            if (state.notificationsPaused) R.string.nls_pause_status else R.string.nls_list_status,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FocoPaperDim,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.nls_pause),
                        subtitle = stringResource(R.string.nls_pause_sub),
                        checked = state.notificationsPaused,
                        onCheckedChange = onNotificationsPaused,
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.phone_pause),
                        subtitle = stringResource(R.string.phone_pause_sub),
                        checked = state.phonePaused,
                        onCheckedChange = onPhonePaused,
                    )
                }
                SettingsRow(
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(notificationsSubtitle(state)),
                    onClick = onOpenAvisos,
                )
                HorizontalDivider()
                WorkProfileBlock(
                    state = state,
                    onRefresh = onRefreshWork,
                    onOpenWorkSettings = onOpenWorkSettings,
                    onWorkPaused = onWorkPaused,
                )
                HorizontalDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_default),
                    subtitle = stringResource(
                        if (state.isDefaultHome) {
                            R.string.settings_default_foco
                        } else {
                            R.string.settings_default_other
                        },
                    ),
                    onClick = onChooseDefault,
                )
                HorizontalDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_previous_launcher),
                    subtitle = stringResource(R.string.settings_previous_sub),
                    onClick = onOpenDefaultApps,
                )
                HorizontalDivider()
                SettingsRow(stringResource(R.string.settings_system), onClick = onOpenSystemSettings)
                HorizontalDivider()
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_version),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = state.versionName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_pause_note),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_about_calendar),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkProfileBlock(
    state: SettingsUiState,
    onRefresh: () -> Unit,
    onOpenWorkSettings: () -> Unit,
    onWorkPaused: (Boolean) -> Unit,
) {
    val status = WorkCatalogRules.settingsStatus(
        loaded = state.workLoaded,
        hasProfile = state.workProfile,
        quietEnabled = state.workQuiet,
        loadFailed = state.workFailed,
        activityCount = state.workCount,
    )
    val subtitle = when {
        state.workProfile && state.workSectionPaused -> stringResource(R.string.work_pause_status)
        status == WorkSettingsStatus.Unknown -> stringResource(R.string.strip_work_unknown)
        status == WorkSettingsStatus.Absent -> stringResource(R.string.settings_work_off)
        status == WorkSettingsStatus.Visible -> stringResource(R.string.settings_work_on)
        status == WorkSettingsStatus.Quiet -> stringResource(R.string.settings_work_quiet)
        else -> stringResource(R.string.work_load_error)
    }
    val body = when (status) {
        WorkSettingsStatus.Absent -> stringResource(R.string.settings_work_none)
        WorkSettingsStatus.Visible, WorkSettingsStatus.Quiet -> stringResource(R.string.settings_work_body)
        WorkSettingsStatus.Unknown, WorkSettingsStatus.Failed -> null
    }
    val showLink = LaunchpadRules.showWorkSettingsLink(state.workLink, state.workProfile)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_work),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = FocoPaperDim)
        }
        if (state.workProfile && !state.workSectionPaused && status != WorkSettingsStatus.Visible) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.work_visible_status),
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
            )
        }
        if (state.workProfile) {
            SettingsSwitchRow(
                title = stringResource(R.string.work_pause),
                subtitle = stringResource(R.string.work_pause_sub),
                checked = state.workSectionPaused,
                onCheckedChange = onWorkPaused,
                horizontalInset = 0.dp,
            )
        }
        if (status == WorkSettingsStatus.Visible || status == WorkSettingsStatus.Quiet) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.work_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
            )
        }
        if (state.workNote != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.workNote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocoTextButton(onClick = onRefresh, enabled = !state.workRefreshing) {
                Text(stringResource(R.string.settings_work_refresh))
            }
            if (showLink) {
                FocoTextButton(onClick = onOpenWorkSettings) {
                    Text(stringResource(R.string.settings_work_open))
                }
            }
        }
    }
}

private fun notificationsSubtitle(state: SettingsUiState): Int {
    return when {
        state.nlsActive -> R.string.nls_filter_status_active
        !state.nlsGranted -> R.string.nls_settings_need_grant
        state.nlsFilterEnabled && !state.nlsConnected -> R.string.nls_settings_disconnected
        else -> R.string.settings_notifications_sub
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    horizontalInset: Dp = FocoSpace.page,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = horizontalInset, vertical = FocoSpace.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocoInk,
                checkedTrackColor = FocoPaper,
                uncheckedThumbColor = FocoPaperDim,
                uncheckedTrackColor = FocoLine,
                uncheckedBorderColor = FocoLine,
            ),
        )
    }
}

@Composable
private fun SettingsRow(title: String, onClick: () -> Unit, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAppsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenAdd: () -> Unit,
    onRemove: (LaunchableApp) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.edit_done))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAdd,
                containerColor = FocoPaper,
                contentColor = FocoInk,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.edit_add))
            }
        },
    ) { padding ->
        if (state.whitelist.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.edit_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                itemsIndexed(state.whitelist, key = { _, app -> app.packageName }) { index, app ->
                    EditRow(
                        app = app,
                        canUp = index > 0,
                        canDown = index < state.whitelist.lastIndex,
                        onMoveUp = { onMoveUp(app.packageName) },
                        onMoveDown = { onMoveDown(app.packageName) },
                        onRemove = { onRemove(app) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EditRow(
    app: LaunchableApp,
    canUp: Boolean,
    canDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val bitmap = remember(app.packageName, app.icon) { app.icon.asImageBitmap() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        val moveColors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        )
        IconButton(onClick = onMoveUp, enabled = canUp, colors = moveColors) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = stringResource(R.string.edit_move_up),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canDown, colors = moveColors) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.edit_move_down),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.edit_remove))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onToggleAdd: (String) -> Unit,
    onConfirmAdd: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.add_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            FocoPrimaryButton(
                onClick = onConfirmAdd,
                enabled = state.pendingAdd.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.add_done))
            }
        },
    ) { padding ->
        if (state.catalog.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.add_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                itemsIndexed(state.catalog, key = { _, app -> app.packageName }) { _, app ->
                    val bitmap = remember(app.packageName, app.icon) { app.icon.asImageBitmap() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleAdd(app.packageName) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = app.packageName in state.pendingAdd,
                            onCheckedChange = { onToggleAdd(app.packageName) },
                        )
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoveDialog(
    isLast: Boolean,
    isSettings: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val body = when {
        isSettings -> stringResource(R.string.confirm_remove_settings)
        isLast -> stringResource(R.string.remove_last_warn)
        else -> null
    }
    val confirmLabel = if (isSettings) {
        stringResource(R.string.confirm_remove_anyway)
    } else {
        stringResource(R.string.remove_confirm)
    }
    val dismissLabel = if (isSettings) {
        stringResource(R.string.confirm_leave)
    } else {
        stringResource(R.string.remove_cancel)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        text = {
            Text(
                text = body ?: stringResource(R.string.edit_remove),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            FocoTextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            FocoTextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}
