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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foco.launcher.R
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant

private val DIET_DAYS = listOf(
    DayOfWeek.MONDAY to R.string.diet_dow_mon,
    DayOfWeek.TUESDAY to R.string.diet_dow_tue,
    DayOfWeek.WEDNESDAY to R.string.diet_dow_wed,
    DayOfWeek.THURSDAY to R.string.diet_dow_thu,
    DayOfWeek.FRIDAY to R.string.diet_dow_fri,
    DayOfWeek.SATURDAY to R.string.diet_dow_sat,
    DayOfWeek.SUNDAY to R.string.diet_dow_sun,
)

@Composable
fun DietPage(
    plan: DietCatalog?,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = System::currentTimeMillis,
) {
    if (plan == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .homeLongPress(onOpenSystemSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.diet_missing),
                style = dietDim(15),
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = nowMillis()
            now = current
            val remainder = Math.floorMod(current, 60_000L)
            val wait = if (remainder == 0L) 60_000L else 60_000L - remainder
            delay(wait.coerceIn(250L, 60_000L))
        }
    }
    val today = Instant.ofEpochMilli(now).atZone(DietPlan.ZONE).toLocalDate()
    var selectedName by rememberSaveable { mutableStateOf(today.dayOfWeek.name) }
    val selected = runCatching { DayOfWeek.valueOf(selectedName) }.getOrDefault(today.dayOfWeek)
    val shown = DietPlan.dateInWeek(today, selected)
    val day = DietPlan.resolve(plan, shown)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocoSpace.page),
    ) {
        Spacer(Modifier.height(FocoSpace.gap))
        DayStrip(
            today = today.dayOfWeek,
            selected = selected,
            onSelect = { selectedName = it.name },
        )
        Spacer(Modifier.height(FocoSpace.sheet))
        if (day == null) {
            Text(
                text = stringResource(R.string.diet_missing),
                style = dietDim(15),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            DietDetail(day)
        }
        Spacer(Modifier.height(FocoSpace.page))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .homeLongPress(onOpenSystemSettings),
        )
    }
}

@Composable
private fun DayStrip(
    today: DayOfWeek,
    selected: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit,
) {
    val todayWord = stringResource(R.string.diet_today_mark)
    Row(modifier = Modifier.fillMaxWidth()) {
        DIET_DAYS.forEach { (dow, labelRes) ->
            val label = stringResource(labelRes)
            val isToday = dow == today
            val isSelected = dow == selected
            val spoken = if (isToday) "$label. $todayWord" else label
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(dow) }
                    .clearAndSetSemantics {
                        contentDescription = spoken
                        role = Role.Button
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = if (isSelected || isToday) FocoPaper else FocoPaperDim,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> FocoPaper
                                isToday -> FocoPaperDim
                                else -> Color.Transparent
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun DietDetail(day: DietDay) {
    Text(
        text = DietPlan.homeTitle(day),
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            color = FocoPaper,
        ),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    if (day.weekend) {
        day.rules.forEach { rule ->
            Text(
                text = rule,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FocoSpace.gapLg),
                style = dietPaper(15),
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    DetailMeal(stringResource(R.string.diet_breakfast), day.breakfast, day.breakfastTip)
    DetailMeal(stringResource(R.string.diet_lunch), day.lunch, day.lunchTip)
    DetailMeal(stringResource(R.string.diet_dinner), day.dinner, day.dinnerTip)
    if (day.alternateTitles.isNotEmpty()) {
        Spacer(Modifier.height(FocoSpace.gap))
        Text(
            text = stringResource(R.string.diet_alts),
            modifier = Modifier.fillMaxWidth(),
            style = dietDim(13),
            textAlign = TextAlign.Center,
        )
        day.alternateTitles.forEach { title ->
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                style = dietPaper(15),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DetailMeal(label: String, value: String, tip: String) {
    if (value.isEmpty() && tip.isEmpty()) return
    Text(
        text = label,
        modifier = Modifier.fillMaxWidth(),
        style = dietDim(13),
        textAlign = TextAlign.Center,
    )
    if (value.isNotEmpty()) {
        Text(
            text = value,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            style = dietPaper(16),
            textAlign = TextAlign.Center,
        )
    }
    if (tip.isNotEmpty()) {
        Text(
            text = tip,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FocoSpace.hair, bottom = FocoSpace.section),
            style = dietDim(13),
            textAlign = TextAlign.Center,
        )
    } else {
        Spacer(Modifier.height(FocoSpace.section))
    }
}

private fun dietPaper(size: Int) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = (size + 6).sp,
    color = FocoPaper,
)

private fun dietDim(size: Int) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = (size + 4).sp,
    color = FocoPaperDim,
)
