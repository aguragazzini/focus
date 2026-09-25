package com.foco.launcher.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foco.launcher.R
import kotlinx.coroutines.delay
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietGlance(
    plan: DietCatalog?,
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = System::currentTimeMillis,
) {
    if (plan == null) return
    var now by remember { mutableLongStateOf(nowMillis()) }
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = nowMillis()
            now = current
            val remainder = Math.floorMod(current, 60_000L)
            val wait = if (remainder == 0L) 60_000L else 60_000L - remainder
            delay(wait.coerceIn(250L, 60_000L))
        }
    }
    val date = Instant.ofEpochMilli(now).atZone(DietPlan.ZONE).toLocalDate()
    val day = DietPlan.resolve(plan, date) ?: return
    val title = DietPlan.homeTitle(day)
    val lunchLabel = stringResource(R.string.diet_lunch)
    val dinnerLabel = stringResource(R.string.diet_dinner)
    val breakfastLabel = stringResource(R.string.diet_breakfast)
    val spoken = buildString {
        append(title)
        if (!day.weekend) {
            if (day.lunch.isNotEmpty()) append(". ").append(lunchLabel).append(". ").append(day.lunch)
            if (day.dinner.isNotEmpty()) append(". ").append(dinnerLabel).append(". ").append(day.dinner)
            if (day.breakfast.isNotEmpty()) append(". ").append(breakfastLabel).append(". ").append(day.breakfast)
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true }
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick { open = true; true }
            }
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = dietTitleStyle(),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!day.weekend) {
            MealLine(lunchLabel, day.lunch)
            MealLine(dinnerLabel, day.dinner)
            MealLine(breakfastLabel, day.breakfast)
        }
    }
    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            containerColor = FocoInkElevated,
            contentColor = FocoPaper,
            tonalElevation = 0.dp,
            dragHandle = { BottomSheetDefaults.DragHandle(color = FocoPaperDim) },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            DietSheet(day)
        }
    }
}

@Composable
private fun MealLine(label: String, value: String) {
    if (value.isEmpty()) return
    Text(
        text = stringResource(R.string.diet_meal_line, label, value),
        modifier = Modifier.padding(top = 2.dp),
        style = dietBodyStyle(),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DietSheet(day: DietDay) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
    ) {
        Text(
            text = DietPlan.homeTitle(day),
            style = dietTitleStyle(),
        )
        Spacer(Modifier.height(12.dp))
        if (day.weekend) {
            day.rules.forEach { rule ->
                Text(
                    text = rule,
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = dietBodyStyle(paper = true),
                )
            }
            return@Column
        }
        SheetMeal(stringResource(R.string.diet_lunch), day.lunch, day.lunchTip)
        SheetMeal(stringResource(R.string.diet_dinner), day.dinner, day.dinnerTip)
        SheetMeal(stringResource(R.string.diet_breakfast), day.breakfast, day.breakfastTip)
        if (day.alternateTitles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.diet_alts),
                style = dietBodyStyle(),
            )
            day.alternateTitles.forEach { title ->
                Text(
                    text = title,
                    modifier = Modifier.padding(top = 4.dp),
                    style = dietBodyStyle(paper = true),
                )
            }
        }
    }
}

@Composable
private fun SheetMeal(label: String, value: String, tip: String) {
    if (value.isEmpty() && tip.isEmpty()) return
    Text(
        text = label,
        style = dietBodyStyle(),
    )
    if (value.isNotEmpty()) {
        Text(
            text = value,
            modifier = Modifier.padding(top = 2.dp),
            style = dietBodyStyle(paper = true),
        )
    }
    if (tip.isNotEmpty()) {
        Text(
            text = tip,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            style = dietBodyStyle(),
        )
    } else {
        Spacer(Modifier.height(10.dp))
    }
}

private fun dietTitleStyle() = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    color = FocoPaper,
)

private fun dietBodyStyle(paper: Boolean = false) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 17.sp,
    color = if (paper) FocoPaper else FocoPaperDim,
)
