package com.foco.launcher.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.ZoneId

@Composable
fun AgendaHomePage(
    title: String,
    hasWorkProfile: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenEvent: (AgendaEvent) -> Unit,
    onOpenWorkCalendar: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = HomeClockFormat.CIVIL_ZONE,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var snapshot by remember { mutableStateOf<AgendaSnapshot?>(null) }
    LaunchedEffect(lifecycle, hasWorkProfile, zone) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val current = System.currentTimeMillis()
                now = current
                snapshot = withContext(Dispatchers.IO) {
                    AgendaReader.load(context, current, zone, hasWorkProfile)
                }
                val remainder = Math.floorMod(current, 60_000L)
                val wait = if (remainder == 0L) 60_000L else 60_000L - remainder
                delay(wait.coerceIn(250L, 60_000L))
            }
        }
    }
    val loaded = snapshot
    val dateLabel = HomeClockFormat.date(now, zone)
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "agenda-head") {
            Spacer(Modifier.height(FocoSpace.gap))
            Text(
                text = title.ifBlank { stringResource(R.string.page_agenda) },
                modifier = Modifier.padding(horizontal = FocoSpace.page),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(FocoSpace.hair))
            Text(
                text = dateLabel,
                modifier = Modifier.padding(horizontal = FocoSpace.page),
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaper,
            )
            Spacer(Modifier.height(FocoSpace.gapLg))
        }
        if (loaded == null) {
            item(key = "agenda-wait") { Spacer(Modifier.height(FocoSpace.section)) }
        } else if (!loaded.granted) {
            item(key = "agenda-permission") {
                Column(Modifier.padding(horizontal = FocoSpace.page)) {
                    Text(
                        text = stringResource(R.string.agenda_permission),
                        style = MaterialTheme.typography.bodyLarge,
                        color = FocoPaper,
                    )
                    FocoTextButton(onClick = onRequestPermission) {
                        Text(stringResource(R.string.agenda_permission_cta))
                    }
                    FocoTextButton(onClick = onOpenAppSettings) {
                        Text(stringResource(R.string.agenda_permission_settings))
                    }
                }
            }
        } else {
            item(key = "agenda-personal-label") {
                SectionLabel(stringResource(R.string.section_personal))
            }
            if (loaded.personal.isEmpty()) {
                item(key = "agenda-personal-empty") { EmptyLine() }
            } else {
                items(loaded.personal, key = { "p:${it.eventId}:${it.beginMillis}" }) { event ->
                    AgendaEventRow(event = event, zone = zone, onOpen = { onOpenEvent(event) })
                }
            }
            if (loaded.workAccess != WorkCalendarAccess.ABSENT) {
                item(key = "agenda-work-label") {
                    Spacer(Modifier.height(FocoSpace.section))
                    SectionLabel(stringResource(R.string.section_work))
                }
                if (loaded.workAccess == WorkCalendarAccess.UNREADABLE) {
                    item(key = "agenda-work-blocked") {
                        Column(Modifier.padding(horizontal = FocoSpace.page)) {
                            Text(
                                text = stringResource(R.string.agenda_work_unavailable),
                                style = MaterialTheme.typography.bodyLarge,
                                color = FocoPaperDim,
                            )
                            FocoTextButton(onClick = onOpenWorkCalendar) {
                                Text(stringResource(R.string.agenda_work_cta))
                            }
                        }
                    }
                } else if (loaded.work.isEmpty()) {
                    item(key = "agenda-work-empty") { EmptyLine() }
                } else {
                    items(loaded.work, key = { "w:${it.eventId}:${it.beginMillis}" }) { event ->
                        AgendaEventRow(event = event, zone = zone, onOpen = { onOpenEvent(event) })
                    }
                }
            }
        }
        item(key = "agenda-escape") {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .homeLongPress(onOpenSystemSettings),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = FocoSpace.page, vertical = FocoSpace.hair),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyLine() {
    Text(
        text = stringResource(R.string.agenda_empty),
        modifier = Modifier.padding(horizontal = FocoSpace.page, vertical = FocoSpace.gap),
        style = MaterialTheme.typography.bodyLarge,
        color = FocoPaperDim,
    )
}

@Composable
private fun AgendaEventRow(event: AgendaEvent, zone: ZoneId, onOpen: () -> Unit) {
    val whenLabel = if (event.allDay) {
        stringResource(R.string.agenda_all_day)
    } else {
        "${AgendaDay.clock(event.beginMillis, zone)}–${AgendaDay.clock(event.endMillis, zone)}"
    }
    val title = event.title.ifBlank { stringResource(R.string.agenda_untitled) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .semantics { role = Role.Button }
            .padding(horizontal = FocoSpace.page, vertical = FocoSpace.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (event.color != 0) {
            Box(
                modifier = Modifier
                    .padding(end = FocoSpace.gap)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(event.color)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = whenLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = FocoPaperDim,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaper,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.calendarName.isNotBlank()) {
                Text(
                    text = event.calendarName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocoPaperDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
