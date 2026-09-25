package com.foco.launcher.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foco.launcher.R
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeClock(
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = System::currentTimeMillis,
    zone: ZoneId = ZoneId.systemDefault(),
    readGlance: () -> String? = { null },
    readExtra: () -> String? = { null },
    underDate: @Composable (LocalDate) -> Unit = {},
) {
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(zone) {
        while (true) {
            val current = nowMillis()
            now = current
            val remainder = Math.floorMod(current, 60_000L)
            val wait = if (remainder == 0L) 60_000L else 60_000L - remainder
            delay(wait.coerceIn(250L, 60_000L))
        }
    }
    val time = HomeClockFormat.time(now, zone)
    val date = HomeClockFormat.date(now, zone)
    val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val glance = listOf(readGlance(), readExtra())
        .mapNotNull { it?.trim()?.takeIf { line -> line.isNotEmpty() } }
        .joinToString(" · ")
        .ifBlank { null }
    val timeCd = stringResource(R.string.clock_time_cd, time)
    val dateCd = stringResource(R.string.clock_date_cd, date)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = time,
            modifier = Modifier
                .clickable(onClick = onOpenClock)
                .clearAndSetSemantics {
                    contentDescription = timeCd
                    role = Role.Button
                    onClick { onOpenClock(); true }
                }
                .padding(horizontal = 12.dp, vertical = 2.dp),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = HomeClockFormat.TIME_SP.sp,
                lineHeight = 56.sp,
                color = FocoPaper,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = date,
            modifier = Modifier
                .clickable(onClick = onOpenCalendar)
                .clearAndSetSemantics {
                    contentDescription = dateCd
                    role = Role.Button
                    onClick { onOpenCalendar(); true }
                }
                .padding(horizontal = 12.dp, vertical = 2.dp),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = HomeClockFormat.DATE_SP.sp,
                lineHeight = 18.sp,
                color = FocoPaperDim,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        underDate(day)
        if (!glance.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = glance,
                modifier = Modifier.padding(horizontal = 12.dp),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = HomeGlanceFormat.GLANCE_SP.sp,
                    lineHeight = 16.sp,
                    color = FocoPaperDim,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}
