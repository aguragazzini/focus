package com.foco.launcher.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.launcher.R
import com.foco.launcher.usage.UsageAccess
import com.foco.launcher.usage.UsageApp
import com.foco.launcher.usage.UsageFormat
import com.foco.launcher.usage.UsageReader
import com.foco.launcher.usage.UsageReport
import com.foco.launcher.usage.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.ZoneId

@Composable
fun UsageHomePage(
    title: String,
    onLaunch: (String) -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = HomeClockFormat.CIVIL_ZONE,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var snapshot by remember { mutableStateOf<UsageSnapshot?>(null) }
    var byToday by remember { mutableStateOf(true) }
    var asked by remember { mutableStateOf(false) }
    var openFailed by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycle, zone) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val current = System.currentTimeMillis()
                snapshot = withContext(Dispatchers.IO) {
                    UsageReader.load(context, current, zone)
                }
                delay(60_000L)
            }
        }
    }
    val loaded = snapshot
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "uso-head") {
            Spacer(Modifier.height(FocoSpace.gap))
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FocoSpace.page),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(FocoSpace.hair))
            }
            Text(
                text = stringResource(R.string.uso_scope),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocoSpace.page),
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FocoSpace.gapLg))
        }
        if (loaded == null) {
            item(key = "uso-wait") { Spacer(Modifier.height(FocoSpace.section)) }
        } else if (!loaded.granted) {
            item(key = "uso-permission") {
                UsagePermission(
                    showRestricted = asked,
                    openFailed = openFailed,
                    onActivate = {
                        asked = true
                        openFailed = !UsageAccess.openSettings(context)
                    },
                )
            }
        } else {
            val rows = UsageReport.rank(loaded.apps, byToday)
            val total = UsageReport.totalMs(loaded.apps, byToday)
            item(key = "uso-total") {
                UsageTotal(
                    totalLabel = UsageFormat.duration(total),
                    byToday = byToday,
                    onToday = { byToday = true },
                    onWeek = { byToday = false },
                )
            }
            if (rows.isEmpty()) {
                item(key = "uso-empty") {
                    Text(
                        text = stringResource(R.string.uso_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FocoSpace.page),
                        style = MaterialTheme.typography.bodyLarge,
                        color = FocoPaperDim,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(rows, key = { it.packageName }) { app ->
                    UsageRow(
                        app = app,
                        ms = if (byToday) app.todayMs else app.weekMs,
                        total = total,
                        onLaunch = onLaunch,
                    )
                }
            }
        }
        item(key = "uso-escape") {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
    }
}

@Composable
private fun UsagePermission(
    showRestricted: Boolean,
    openFailed: Boolean,
    onActivate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocoSpace.page),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.uso_permission_title),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = FocoPaper,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FocoSpace.gap))
        Text(
            text = stringResource(R.string.uso_permission),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = FocoPaper,
            textAlign = TextAlign.Center,
        )
        FocoTextButton(onClick = onActivate) {
            Text(stringResource(R.string.uso_permission_cta))
        }
        if (openFailed) {
            Text(
                text = stringResource(R.string.uso_open_fail),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
                textAlign = TextAlign.Center,
            )
        }
        if (showRestricted) {
            Spacer(Modifier.height(FocoSpace.gap))
            Text(
                text = stringResource(R.string.uso_restricted),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun UsageTotal(
    totalLabel: String,
    byToday: Boolean,
    onToday: () -> Unit,
    onWeek: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = totalLabel,
            style = MaterialTheme.typography.titleLarge,
            color = FocoPaper,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FocoSpace.gap))
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UsageChip(stringResource(R.string.uso_chip_today), byToday, onToday)
            Spacer(Modifier.width(FocoSpace.gap))
            UsageChip(stringResource(R.string.uso_chip_week), !byToday, onWeek)
        }
        Spacer(Modifier.height(FocoSpace.gapLg))
    }
}

@Composable
private fun UsageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val border = if (selected) FocoPaper else FocoLine
    val color = if (selected) FocoPaper else FocoPaperDim
    Text(
        text = label,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}

@Composable
private fun UsageRow(
    app: UsageApp,
    ms: Long,
    total: Long,
    onLaunch: (String) -> Unit,
) {
    val open = if (app.launchable) {
        Modifier
            .clickable(onClick = { onLaunch(app.packageName) })
            .semantics { role = Role.Button }
    } else {
        Modifier
    }
    val fraction = if (total > 0L) (ms.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(open)
            .padding(horizontal = FocoSpace.page, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FocoLine),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tileInitial(app.label),
                style = MaterialTheme.typography.labelLarge,
                color = FocoPaperDim,
            )
        }
        Column(modifier = Modifier.padding(start = FocoSpace.gap).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FocoPaper,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = UsageFormat.duration(ms),
                    modifier = Modifier.padding(start = FocoSpace.gap),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FocoPaper,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(FocoLine),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(2.dp)
                        .background(FocoPaperDim),
                )
            }
        }
    }
}
