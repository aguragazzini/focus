package com.foco.launcher.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.launcher.R
import com.foco.launcher.notification.Silence
import com.foco.launcher.notification.SilenceLevel

/**
 * One pill. The label is the mode that is on.
 * Sin pausa → Foco → Todo en pausa. Without a listener it asks to activate avisos.
 */
@Composable
fun SilenceControl(
    notificationsPaused: Boolean,
    phonePaused: Boolean,
    listenerReady: Boolean,
    onChange: (SilenceLevel) -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val level = Silence.level(notificationsPaused, phonePaused)
    val label = when {
        !listenerReady -> stringResource(R.string.silence_activate)
        level == SilenceLevel.TODO -> stringResource(R.string.silence_all)
        level == SilenceLevel.AVISOS -> stringResource(R.string.silence_foco)
        else -> stringResource(R.string.silence_off)
    }
    val active = listenerReady && level != SilenceLevel.OFF
    val hardest = listenerReady && level == SilenceLevel.TODO
    val textColor = if (active) FocoPaper else FocoPaperDim
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(FocoSpace.touch)
                .clip(shape)
                .background(if (active) FocoInkElevated else Color.Transparent)
                .border(
                    width = if (hardest) 1.5.dp else 1.dp,
                    color = if (hardest) FocoPaper else FocoLine,
                    shape = shape,
                )
                .clickable {
                    if (listenerReady) onChange(Silence.next(level)) else onActivate()
                }
                .semantics {
                    role = Role.Button
                    selected = active
                }
                .padding(horizontal = FocoSpace.gapLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = textColor,
            )
            Text(
                text = label,
                color = textColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
