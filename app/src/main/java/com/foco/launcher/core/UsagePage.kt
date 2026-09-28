package com.foco.launcher.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.launcher.R
import com.foco.launcher.usage.UsageAccess
import com.foco.launcher.usage.UsageApp
import com.foco.launcher.usage.UsageFormat
import com.foco.launcher.usage.UsageDay
import com.foco.launcher.usage.UsageReader
import com.foco.launcher.usage.UsageReport
import com.foco.launcher.usage.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

@Composable
fun UsageHomePage(
    title: String,
    onLaunch: (String) -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = HomeClockFormat.CIVIL_ZONE,
    active: Boolean = true,
    namesOnly: Boolean = false,
    personal: Set<String> = emptySet(),
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var snapshot by remember { mutableStateOf<UsageSnapshot?>(null) }
    var byToday by remember { mutableStateOf(true) }
    var asked by remember { mutableStateOf(false) }
    var openFailed by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(lifecycle, zone, active, refreshTick) {
        if (!active) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val current = System.currentTimeMillis()
            snapshot = withContext(Dispatchers.IO) {
                UsageReader.load(context, current, zone)
            }
        }
    }
    val loaded = snapshot
    val failText = stringResource(R.string.uso_read_fail)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(loaded?.failed, loaded) {
        if (loaded?.failed != true) return@LaunchedEffect
        val shown = launch {
            snackbar.showSnackbar(failText, duration = SnackbarDuration.Indefinite)
        }
        delay(2_000)
        snackbar.currentSnackbarData?.dismiss()
        shown.join()
    }
    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
        } else if (loaded.failed) {
            item(key = "uso-failed") { Spacer(Modifier.height(FocoSpace.section)) }
        } else {
            val rows = UsageReport.rank(loaded.apps, byToday)
            val total = UsageReport.totalMs(loaded.apps, byToday)
            val weekTotal = UsageReport.totalMs(loaded.apps, byToday = false)
            val peak = rows.maxOfOrNull { if (byToday) it.todayMs else it.weekMs } ?: 0L
            val leak = UsageReport.leak(loaded.apps, byToday, personal)
            val screenMs = loaded.screenOnTodayMs
            item(key = "uso-total") {
                UsageTotal(
                    totalLabel = UsageFormat.duration(total),
                    subtitle = stringResource(
                        if (byToday) R.string.uso_chip_today else R.string.uso_week_label,
                    ),
                    screenLine = if (byToday && screenMs != null) {
                        stringResource(R.string.uso_screen, UsageFormat.duration(screenMs))
                    } else {
                        null
                    },
                    weekLine = if (byToday) {
                        stringResource(R.string.uso_week_label) + " · " + UsageFormat.duration(weekTotal)
                    } else {
                        null
                    },
                    leakLine = if (leak == null) {
                        null
                    } else {
                        stringResource(R.string.uso_leak, leak.count, UsageFormat.duration(leak.ms))
                    },
                    days = loaded.days,
                    zone = zone,
                    byToday = byToday,
                    onToday = { byToday = true },
                    onWeek = { byToday = false },
                    onRefresh = { refreshTick += 1 },
                )
            }
            if (rows.isEmpty()) {
                item(key = "uso-empty") {
                    Text(
                        text = stringResource(
                            if (byToday) R.string.uso_empty_today else R.string.uso_empty_week,
                        ),
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
                        peak = peak,
                        namesOnly = namesOnly,
                        outside = app.packageName !in personal,
                        readAtMillis = loaded.readAtMillis,
                        zone = zone,
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
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = FocoInkElevated,
                contentColor = FocoPaperDim,
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
    subtitle: String,
    screenLine: String?,
    weekLine: String?,
    leakLine: String?,
    days: List<UsageDay>,
    zone: ZoneId,
    byToday: Boolean,
    onToday: () -> Unit,
    onWeek: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = totalLabel,
            modifier = Modifier.fillMaxWidth(),
            color = FocoPaper,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 36.sp, lineHeight = 42.sp),
        )
        Spacer(Modifier.height(FocoSpace.hair))
        Text(
            text = subtitle,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = FocoPaperDim,
            textAlign = TextAlign.Center,
        )
        if (screenLine != null) {
            Spacer(Modifier.height(FocoSpace.hair))
            Text(
                text = screenLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocoSpace.page),
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaper,
                textAlign = TextAlign.Center,
            )
        }
        if (weekLine != null) {
            Spacer(Modifier.height(FocoSpace.gapLg))
            Text(
                text = weekLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocoSpace.page),
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaper,
                textAlign = TextAlign.Center,
            )
        }
        if (days.any { it.foregroundMs > 0L }) {
            Spacer(Modifier.height(FocoSpace.gap))
            DayBars(days, zone)
        }
        Spacer(Modifier.height(FocoSpace.gapLg))
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UsageChip(stringResource(R.string.uso_chip_today), byToday, onToday)
            Spacer(Modifier.width(FocoSpace.gap))
            UsageChip(stringResource(R.string.uso_chip_week), !byToday, onWeek)
        }
        FocoTextButton(onClick = onRefresh) {
            Text(stringResource(R.string.uso_refresh))
        }
        if (leakLine != null) {
            Text(
                text = leakLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocoSpace.page),
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stringResource(R.string.uso_more),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = FocoPaperDim,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FocoSpace.gap))
    }
}

@Composable
private fun DayBars(days: List<UsageDay>, zone: ZoneId) {
    val max = days.maxOf { it.foregroundMs }.coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocoSpace.page),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val fraction = (day.foregroundMs.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(FocoLine),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .background(FocoPaper.copy(alpha = 0.20f)),
                    )
                }
                Text(
                    text = UsageFormat.weekday(day.startMillis, zone),
                    color = FocoPaperDim,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, lineHeight = 12.sp),
                )
            }
        }
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
    peak: Long,
    namesOnly: Boolean,
    outside: Boolean,
    readAtMillis: Long,
    zone: ZoneId,
    onLaunch: (String) -> Unit,
) {
    val open = if (app.launchable) {
        Modifier
            .clickable(onClick = { onLaunch(app.packageName) })
            .semantics { role = Role.Button }
    } else {
        Modifier
    }
    val fraction = if (peak > 0L) (ms.toFloat() / peak.toFloat()).coerceIn(0f, 1f) else 0f
    val used = UsageFormat.lastUsed(app.lastUsedMillis, readAtMillis, zone)
    val fuera = stringResource(R.string.uso_fuera)
    val note = buildList {
        if (outside) add(fuera)
        if (used != null) add(used)
    }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(open)
            .padding(horizontal = FocoSpace.page, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UsageGlyph(app = app, namesOnly = namesOnly)
        Column(modifier = Modifier.padding(start = FocoSpace.gap).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    color = FocoPaper,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = UsageFormat.duration(ms),
                    modifier = Modifier.padding(start = FocoSpace.gap),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    color = FocoPaperDim,
                    maxLines = 1,
                )
            }
            if (note.isNotEmpty()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocoPaperDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                        .background(FocoPaper.copy(alpha = 0.20f)),
                )
            }
        }
    }
}

@Composable
private fun UsageGlyph(app: UsageApp, namesOnly: Boolean) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, app.packageName, namesOnly) {
        if (namesOnly) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(app.packageName)
                    .toBitmap(width = 96, height = 96)
                    .asImageBitmap()
            }.getOrNull()
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FocoInkElevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tileInitial(app.label),
                style = MaterialTheme.typography.labelLarge,
                color = FocoPaperDim,
            )
        }
    }
}
