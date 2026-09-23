@file:OptIn(ExperimentalFoundationApi::class)

package com.foco.launcher.core

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.foco.launcher.R
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.work.WorkApp

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
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        onMessageShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            HomeOverflow(
                onOpenFocoSettings = onOpenFocoSettings,
                onEditApps = onEditApps,
                onOpenSystemSettings = onOpenSystemSettings,
            )

            when (state.banner) {
                HomeBanner.NotDefault -> NotDefaultBanner(onChooseDefault)
                HomeBanner.Nls -> NlsOffBanner(onOpenAvisos)
                HomeBanner.None -> Unit
            }

            if (state.apps.isEmpty() && !state.workProfile) {
                EmptyHome(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .longPressEmpty(onOpenSystemSettings),
                    onAddApps = onAddApps,
                    onOpenSystemSettings = onOpenSystemSettings,
                )
            } else {
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        SectionTitle(stringResource(R.string.section_personal))
                        Spacer(Modifier.height(12.dp))
                        if (state.apps.isEmpty()) {
                            PersonalEmptyInline(
                                onAddApps = onAddApps,
                                onOpenSystemSettings = onOpenSystemSettings,
                            )
                        } else {
                            AppGrid(state.apps.map { it.toCell() }) { cell ->
                                onLaunch(cell.id)
                            }
                        }
                        if (state.workProfile) {
                            Spacer(Modifier.height(28.dp))
                            SectionTitle(stringResource(R.string.section_work))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.work_sub),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            if (state.workApps.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.work_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                val byKey = state.workApps.associateBy { it.key }
                                AppGrid(state.workApps.map { it.toCell() }) { cell ->
                                    byKey[cell.id]?.let(onLaunchWork)
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .longPressEmpty(onOpenSystemSettings),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeOverflow(
    onOpenFocoSettings: () -> Unit,
    onEditApps: () -> Unit,
    onOpenSystemSettings: () -> Unit,
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
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_edit_apps)) },
                    onClick = {
                        menu = false
                        onEditApps()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_open_settings)) },
                    onClick = {
                        menu = false
                        onOpenFocoSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_system_settings)) },
                    onClick = {
                        menu = false
                        onOpenSystemSettings()
                    },
                )
            }
        }
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
private fun EmptyHome(
    modifier: Modifier,
    onAddApps: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        FocoTextButton(onClick = onAddApps) {
            Text(stringResource(R.string.home_add))
        }
        FocoTextButton(onClick = onOpenSystemSettings) {
            Text(stringResource(R.string.home_system_settings))
        }
    }
}

private data class HomeCell(
    val id: String,
    val label: String,
    val icon: Bitmap,
)

private fun LaunchableApp.toCell(): HomeCell = HomeCell(packageName, label, icon)

private fun WorkApp.toCell(): HomeCell = HomeCell(key, label, icon)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.04.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PersonalEmptyInline(
    onAddApps: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
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
    FocoTextButton(onClick = onAddApps) {
        Text(stringResource(R.string.home_add))
    }
    FocoTextButton(onClick = onOpenSystemSettings) {
        Text(stringResource(R.string.home_system_settings))
    }
}

@Composable
private fun AppGrid(cells: List<HomeCell>, onClick: (HomeCell) -> Unit) {
    cells.chunked(4).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { cell ->
                AppCell(
                    cell = cell,
                    modifier = Modifier.weight(1f),
                    onClick = { onClick(cell) },
                )
            }
            repeat(4 - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AppCell(cell: HomeCell, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bitmap = remember(cell.id, cell.icon) { cell.icon.asImageBitmap() }
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = cell.label,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = cell.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
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
