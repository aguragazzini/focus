package com.foco.launcher.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.launcher.R
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.PageBlock
import com.foco.launcher.registry.PageBlocks
import com.foco.launcher.work.WorkApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId

@Composable
fun PageBlockColumn(
    blocks: List<PageBlock>,
    apps: List<LaunchableApp>,
    workApps: List<WorkApp>,
    hasWorkProfile: Boolean,
    notificationsPaused: Boolean,
    phonePaused: Boolean,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onPhonePaused: (Boolean) -> Unit,
    onLaunch: (String) -> Unit,
    onUpdateBlock: (PageBlock) -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = HomeClockFormat.CIVIL_ZONE,
    showEmpty: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(30_000L)
            }
        }
    }
    val day = remember(now, zone) { Instant.ofEpochMilli(now).atZone(zone).toLocalDate() }
    val needsAgenda = blocks.any {
        it.type == PageBlocks.AGENDA || it.type == PageBlocks.PROXIMO ||
            it.type == PageBlocks.RECORDATORIO || it.type == PageBlocks.CALENDARIO
    }
    var agenda by remember { mutableStateOf<AgendaSnapshot?>(null) }
    LaunchedEffect(needsAgenda, hasWorkProfile, zone) {
        if (!needsAgenda) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            agenda = withContext(Dispatchers.IO) {
                AgendaReader.load(context, System.currentTimeMillis(), zone, hasWorkProfile)
            }
        }
    }
    var celsius by remember { mutableIntStateOf(Int.MIN_VALUE) }
    val wantsWeather = blocks.any { it.type == PageBlocks.CLIMA }
    LaunchedEffect(wantsWeather) {
        if (!wantsWeather) return@LaunchedEffect
        val value = withContext(Dispatchers.IO) { CordobaWeather.fetchCelsius() }
        celsius = value ?: Int.MIN_VALUE
    }
    val vetus = remember(context) { loadVetus(context) }
    val novus = remember(context) { loadNovus(context) }
    val meals = remember(context) { loadMeals(context) }
    val phrases = remember(context) { loadPhrases(context) }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (blocks.isEmpty() && showEmpty) {
            Text(
                text = stringResource(R.string.home_blocks_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocoSpace.page, vertical = FocoSpace.gap),
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaperDim,
                textAlign = TextAlign.Center,
            )
        }
        blocks.forEach { block ->
            BlockFrame(label = frameLabel(block.type)) {
                BlockBody(
                    block = block,
                    now = now,
                    zone = zone,
                    day = day,
                    agenda = agenda,
                    celsius = celsius,
                    vetus = vetus,
                    novus = novus,
                    meals = meals,
                    phrases = phrases,
                    apps = apps,
                    workApps = workApps,
                    hasWorkProfile = hasWorkProfile,
                    notificationsPaused = notificationsPaused,
                    phonePaused = phonePaused,
                    onOpenClock = onOpenClock,
                    onOpenCalendar = onOpenCalendar,
                    onNotificationsPaused = onNotificationsPaused,
                    onPhonePaused = onPhonePaused,
                    onLaunch = onLaunch,
                    onUpdateBlock = onUpdateBlock,
                )
            }
        }
    }
}

/**
 * One centered caption. Reloj skips it: the page name is already Reloj and the digits are the block.
 * Vetus and Novus use this caption and hide the title inside [SantoralLine].
 */
@Composable
private fun frameLabel(type: String): String? {
    if (type == PageBlocks.RELOJ || type == PageBlocks.VACIO) return null
    return blockLabel(type)
}

@Composable
private fun BlockFrame(label: String?, body: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocoSpace.page, vertical = FocoSpace.hair),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        body()
    }
}

@Composable
private fun BlockBody(
    block: PageBlock,
    now: Long,
    zone: ZoneId,
    day: java.time.LocalDate,
    agenda: AgendaSnapshot?,
    celsius: Int,
    vetus: SantoralCatalog?,
    novus: SantoralCatalog?,
    meals: DietCatalog?,
    phrases: List<String>,
    apps: List<LaunchableApp>,
    workApps: List<WorkApp>,
    hasWorkProfile: Boolean,
    notificationsPaused: Boolean,
    phonePaused: Boolean,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onNotificationsPaused: (Boolean) -> Unit,
    onPhonePaused: (Boolean) -> Unit,
    onLaunch: (String) -> Unit,
    onUpdateBlock: (PageBlock) -> Unit,
) {
    val context = LocalContext.current
    when (block.type) {
        PageBlocks.RELOJ -> HomeClock(
            onOpenClock = onOpenClock,
            onOpenCalendar = onOpenCalendar,
            zone = zone,
            readGlance = { null },
            showDate = false,
            showGlance = false,
        )
        PageBlocks.FECHA -> BodyText(HomeClockFormat.date(now, zone))
        PageBlocks.BATERIA -> {
            val percent = remember(now) { HomeGlance.battery(context) }
            BodyText(percent?.let { "$it%" } ?: stringResource(R.string.block_unavailable))
        }
        PageBlocks.ALARMA -> {
            val alarm = remember(now) { HomeGlance.nextAlarm(context, zone) }
            BodyText(alarm ?: stringResource(R.string.block_no_alarm))
        }
        PageBlocks.VETUS -> {
            val line = vetus?.let { Santoral1962.resolve(it, day) }
            if (line != null) {
                SantoralLine(
                    day = line,
                    label = stringResource(R.string.santoral_vetus),
                    showLabel = false,
                )
            } else {
                BodyText(stringResource(R.string.block_unavailable))
            }
        }
        PageBlocks.NOVUS -> {
            val line = novus?.let { SantoralNovus.resolve(it, day) }
            if (line != null) {
                SantoralLine(
                    day = line,
                    label = stringResource(R.string.santoral_novus),
                    novus = true,
                    showLabel = false,
                )
            } else {
                BodyText(stringResource(R.string.block_unavailable))
            }
        }
        PageBlocks.AGENDA -> AgendaLines(agenda, limit = 4)
        PageBlocks.PROXIMO -> {
            val events = agendaEvents(agenda)
            val next = PageBlockLogic.nextUpcoming(events, now)
            BodyText(next?.title?.ifBlank { stringResource(R.string.agenda_untitled) } ?: stringResource(R.string.agenda_empty))
        }
        PageBlocks.COMIDA -> MealToday(meals, day, compact = false)
        PageBlocks.CLIMA -> BodyText(
            if (celsius == Int.MIN_VALUE) stringResource(R.string.block_weather_fail) else "$celsius°",
        )
        PageBlocks.SEMANA -> BodyText("${PageBlockLogic.weekNumber(day)} · ${PageBlockLogic.weekdayShort(day)}")
        PageBlocks.LUNA -> BodyText(PageBlockLogic.moonLabel(PageBlockLogic.moonPhase(now)))
        PageBlocks.SILENCIO -> FocoTextButton(onClick = { onNotificationsPaused(!notificationsPaused) }) {
            Text(
                stringResource(
                    if (notificationsPaused) R.string.nls_paused_chip else R.string.nls_pause,
                ),
            )
        }
        PageBlocks.PAUSAR -> FocoTextButton(onClick = { onPhonePaused(!phonePaused) }) {
            Text(stringResource(if (phonePaused) R.string.phone_paused_chip else R.string.phone_pause))
        }
        PageBlocks.ATAJOS -> AppNames(apps.take(4), onLaunch)
        PageBlocks.TRABAJO -> {
            if (!hasWorkProfile) {
                BodyText(stringResource(R.string.block_no_work))
            } else {
                BodyText(stringResource(R.string.block_work_count, workApps.size))
                AppNames(workApps.take(4).map { it.label }, onClick = null)
            }
        }
        PageBlocks.FAVORITOS -> {
            val pinned = block.pins.mapNotNull { id -> apps.find { it.packageName == id } }
            if (pinned.isEmpty()) BodyText(stringResource(R.string.block_no_pins))
            else AppNames(pinned, onLaunch)
        }
        PageBlocks.RECORDATORIO -> {
            val hit = PageBlockLogic.reminder(agendaEvents(agenda))
            BodyText(hit?.title?.ifBlank { stringResource(R.string.agenda_untitled) } ?: stringResource(R.string.block_no_reminder))
        }
        PageBlocks.NOTA -> OutlinedTextField(
            value = block.note,
            onValueChange = { raw -> onUpdateBlock(block.copy(note = raw.take(PageBlocks.NOTE_MAX))) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = FocoPaper,
                textAlign = TextAlign.Center,
            ),
            placeholder = {
                Text(
                    text = stringResource(R.string.block_note_hint),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = FocoPaper,
                unfocusedTextColor = FocoPaper,
                focusedBorderColor = FocoPaperDim,
                unfocusedBorderColor = FocoLine,
                cursorColor = FocoPaper,
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
        PageBlocks.CONTADOR -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocoTextButton(onClick = { onUpdateBlock(block.copy(count = (block.count - 1).coerceAtLeast(0))) }) {
                Text("−")
            }
            Text(
                text = block.count.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = FocoPaper,
            )
            FocoTextButton(onClick = {
                onUpdateBlock(block.copy(count = (block.count + 1).coerceAtMost(PageBlocks.COUNT_MAX)))
            }) { Text("+") }
        }
        PageBlocks.DATOS -> BodyText(remember(now) { DeviceSignals.dataLabel(context) })
        PageBlocks.WIFI -> BodyText(remember(now) { DeviceSignals.wifiLabel(context) })
        PageBlocks.ESPACIO -> BodyText(
            PageBlockLogic.storageLabel(remember(now) { DeviceSignals.freeBytes() })
                ?: stringResource(R.string.block_unavailable),
        )
        PageBlocks.ONOMASTICO -> {
            val name = vetus?.let { Santoral1962.resolve(it, day) }?.name
                ?: novus?.let { SantoralNovus.resolve(it, day) }?.name
            BodyText(name?.ifBlank { null } ?: stringResource(R.string.block_unavailable))
        }
        PageBlocks.FRASE -> BodyText(
            PageBlockLogic.pickPhrase(phrases, day) ?: stringResource(R.string.block_unavailable),
        )
        PageBlocks.PASOS -> MealToday(meals, day, compact = true)
        PageBlocks.CALENDARIO -> {
            val count = agendaEvents(agenda).size
            BodyText(stringResource(R.string.block_calendar_count, count))
            FocoTextButton(onClick = onOpenCalendar) {
                Text(stringResource(R.string.block_open_calendar))
            }
        }
        PageBlocks.VACIO -> Spacer(Modifier.height(28.dp))
        else -> BodyText(stringResource(R.string.block_unavailable))
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        color = FocoPaper,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AgendaLines(snapshot: AgendaSnapshot?, limit: Int) {
    if (snapshot == null) {
        BodyText(stringResource(R.string.block_unavailable))
        return
    }
    if (!snapshot.granted) {
        BodyText(stringResource(R.string.agenda_permission))
        return
    }
    val events = agendaEvents(snapshot)
    if (events.isEmpty()) {
        BodyText(stringResource(R.string.agenda_empty))
        return
    }
    events.take(limit).forEach { event ->
        Text(
            text = event.title.ifBlank { stringResource(R.string.agenda_untitled) },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = FocoPaper,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppNames(apps: List<LaunchableApp>, onLaunch: (String) -> Unit) {
    if (apps.isEmpty()) {
        BodyText(stringResource(R.string.home_empty))
        return
    }
    apps.forEach { app ->
        Text(
            text = app.label,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLaunch(app.packageName) }
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = FocoPaper,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppNames(labels: List<String>, onClick: (() -> Unit)?) {
    if (labels.isEmpty()) return
    labels.forEach { label ->
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = FocoPaper,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MealToday(plan: DietCatalog?, day: java.time.LocalDate, compact: Boolean) {
    val resolved = plan?.let { DietPlan.resolve(it, day) }
    if (resolved == null) {
        BodyText(stringResource(R.string.diet_missing))
        return
    }
        if (compact) {
        val hour = java.time.LocalTime.now(HomeClockFormat.CIVIL_ZONE).hour
        val slot = PageBlockLogic.mealSlot(hour, resolved.weekend)
        val line = when (slot) {
            "desayuno" -> resolved.breakfast
            "almuerzo" -> resolved.lunch
            "cena" -> resolved.dinner
            else -> resolved.theme
        }
        BodyText(line.ifBlank { resolved.theme }.ifBlank { stringResource(R.string.diet_missing) })
        return
    }
    BodyText(DietPlan.homeTitle(resolved))
    if (!resolved.weekend) {
        if (resolved.breakfast.isNotBlank()) BodyText(resolved.breakfast)
        if (resolved.lunch.isNotBlank()) BodyText(resolved.lunch)
        if (resolved.dinner.isNotBlank()) BodyText(resolved.dinner)
    }
}

@Composable
fun blockLabel(type: String): String {
    val res = when (type) {
        PageBlocks.RELOJ -> R.string.block_reloj
        PageBlocks.FECHA -> R.string.block_fecha
        PageBlocks.BATERIA -> R.string.block_bateria
        PageBlocks.ALARMA -> R.string.block_alarma
        PageBlocks.VETUS -> R.string.santoral_vetus
        PageBlocks.NOVUS -> R.string.santoral_novus
        PageBlocks.AGENDA -> R.string.block_agenda
        PageBlocks.PROXIMO -> R.string.block_proximo
        PageBlocks.COMIDA -> R.string.page_diet
        PageBlocks.CLIMA -> R.string.block_clima
        PageBlocks.SEMANA -> R.string.block_semana
        PageBlocks.LUNA -> R.string.block_luna
        PageBlocks.SILENCIO -> R.string.block_silencio
        PageBlocks.PAUSAR -> R.string.block_pausar
        PageBlocks.ATAJOS -> R.string.block_atajos
        PageBlocks.TRABAJO -> R.string.section_work
        PageBlocks.FAVORITOS -> R.string.block_favoritos
        PageBlocks.RECORDATORIO -> R.string.block_recordatorio
        PageBlocks.NOTA -> R.string.block_nota
        PageBlocks.CONTADOR -> R.string.block_contador
        PageBlocks.DATOS -> R.string.block_datos
        PageBlocks.WIFI -> R.string.block_wifi
        PageBlocks.ESPACIO -> R.string.block_espacio
        PageBlocks.ONOMASTICO -> R.string.block_onomastico
        PageBlocks.FRASE -> R.string.block_frase
        PageBlocks.PASOS -> R.string.block_pasos
        PageBlocks.CALENDARIO -> R.string.block_calendario
        PageBlocks.VACIO -> R.string.block_vacio
        else -> R.string.block_unavailable
    }
    return stringResource(res)
}

private fun agendaEvents(snapshot: AgendaSnapshot?): List<AgendaEvent> {
    if (snapshot == null || !snapshot.granted) return emptyList()
    return snapshot.personal + snapshot.work
}

private fun loadVetus(context: android.content.Context): SantoralCatalog? {
    return Santoral1962.peek() ?: runCatching {
        context.assets.open("santoral_1962.json").bufferedReader().use { reader ->
            Santoral1962.parse(reader.readText())
        }
    }.getOrNull()?.also { Santoral1962.store(it) }
}

private fun loadNovus(context: android.content.Context): SantoralCatalog? {
    return SantoralNovus.peek() ?: runCatching {
        context.assets.open("santoral_novus.json").bufferedReader().use { reader ->
            SantoralNovus.parse(reader.readText())
        }
    }.getOrNull()?.also { SantoralNovus.store(it) }
}

private fun loadMeals(context: android.content.Context): DietCatalog? {
    return DietPlan.peek() ?: runCatching {
        context.assets.open("plan_ragazzini.json").bufferedReader().use { reader ->
            DietPlan.parse(reader.readText())
        }
    }.getOrNull()?.also { DietPlan.store(it) }
}

private fun loadPhrases(context: android.content.Context): List<String> {
    return runCatching {
        context.assets.open("frases.json").bufferedReader().use { reader ->
            Json { ignoreUnknownKeys = true }.decodeFromString<List<String>>(reader.readText())
        }
    }.getOrDefault(emptyList())
}
