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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.foco.launcher.R
import com.foco.launcher.registry.LaunchableApp

@Composable
fun SettingsHost(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenAvisos: () -> Unit,
    onChooseDefault: () -> Unit,
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
    onSkipNls: () -> Unit,
    onSetNotifAllowed: (String, Boolean) -> Unit,
    onOpenAvisosAdd: () -> Unit,
    onTogglePendingAvisos: (String) -> Unit,
    onConfirmAvisosAdd: () -> Unit,
    onNlsMessageShown: () -> Unit,
) {
    BackHandler(onBack = onBack)
    when (state.dest) {
        SettingsDest.Main -> SettingsMain(
            state = state,
            onBack = onBack,
            onOpenEdit = onOpenEdit,
            onOpenAvisos = onOpenAvisos,
            onChooseDefault = onChooseDefault,
            onOpenSystemSettings = onOpenSystemSettings,
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
            onSetNotifAllowed = onSetNotifAllowed,
            onOpenAvisosAdd = onOpenAvisosAdd,
            onNlsMessageShown = onNlsMessageShown,
        )
        SettingsDest.AvisosAdd -> AvisosAddScreen(
            state = state,
            onBack = onBack,
            onToggle = onTogglePendingAvisos,
            onConfirm = onConfirmAvisosAdd,
        )
        SettingsDest.NlsOnboarding -> NlsOnboardingScreen(
            onOpenSettings = onOpenNlsSettings,
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
    onOpenSystemSettings: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                SettingsRow(stringResource(R.string.settings_apps), onClick = onOpenEdit)
                HorizontalDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_sub),
                    onClick = onOpenAvisos,
                )
                HorizontalDivider()
                SettingsRow(stringResource(R.string.settings_default), onClick = onChooseDefault)
                HorizontalDivider()
                SettingsRow(stringResource(R.string.settings_previous_launcher), onClick = onChooseDefault)
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
                        text = stringResource(R.string.settings_week1_note),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
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
            FloatingActionButton(onClick = onOpenAdd) {
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
        IconButton(onClick = onMoveUp, enabled = canUp) {
            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.edit_move_up))
        }
        IconButton(onClick = onMoveDown, enabled = canDown) {
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = stringResource(R.string.edit_move_down))
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
            Button(
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
        isSettings -> stringResource(R.string.remove_settings_warn)
        isLast -> stringResource(R.string.remove_last_warn)
        else -> null
    }
    val confirmLabel = if (isSettings) {
        stringResource(R.string.remove_settings_confirm)
    } else {
        stringResource(R.string.remove_confirm)
    }
    val dismissLabel = if (isSettings) {
        stringResource(R.string.remove_settings_keep)
    } else {
        stringResource(R.string.remove_cancel)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(body ?: stringResource(R.string.edit_remove))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}
