package com.foco.launcher.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.launcher.R
import com.foco.launcher.notification.Silence
import com.foco.launcher.notification.SilenceLevel

/**
 * One track. Neither half lit is off. Avisos is the in-scope cancel.
 * Todo is the listener-wide cancel. Tap the lit half to clear.
 */
@Composable
fun SilenceControl(
    notificationsPaused: Boolean,
    phonePaused: Boolean,
    onChange: (SilenceLevel) -> Unit,
    avisosIdle: String,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    val level = Silence.level(notificationsPaused, phonePaused)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.silencio_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FocoSpace.hair))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FocoSpace.touch)
                .clip(RoundedCornerShape(50))
                .border(1.dp, FocoLine, RoundedCornerShape(50)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SilenceSegment(
                label = stringResource(R.string.silencio_avisos),
                selected = level == SilenceLevel.AVISOS,
                description = if (level == SilenceLevel.AVISOS) {
                    stringResource(R.string.nls_paused_chip)
                } else {
                    avisosIdle
                },
                onClick = {
                    onChange(if (level == SilenceLevel.AVISOS) SilenceLevel.OFF else SilenceLevel.AVISOS)
                },
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(FocoLine),
            )
            SilenceSegment(
                label = stringResource(R.string.silencio_todo),
                selected = level == SilenceLevel.TODO,
                description = stringResource(
                    if (level == SilenceLevel.TODO) R.string.silencio_todo_on else R.string.block_pausar,
                ),
                onClick = {
                    onChange(if (level == SilenceLevel.TODO) SilenceLevel.OFF else SilenceLevel.TODO)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SilenceSegment(
    label: String,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) FocoInkElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                this.selected = selected
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) FocoPaper else FocoPaperDim,
            textAlign = TextAlign.Center,
        )
    }
}
