package com.foco.launcher.core

import android.view.HapticFeedbackConstants
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.launcher.R
import com.foco.launcher.notification.Silence
import com.foco.launcher.notification.SilenceLevel

/**
 * Off says Avisos: outline bell, hairline, no cancel.
 * On says Foco: paper fill and a filled bell, and the listener cancels everything it sees.
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
    val view = LocalView.current
    val on = listenerReady && level == SilenceLevel.FOCO
    val visible = when {
        !listenerReady -> stringResource(R.string.silence_activate)
        on -> stringResource(R.string.silence_foco)
        else -> stringResource(R.string.silence_avisos)
    }
    val spoken = when {
        !listenerReady -> stringResource(R.string.silence_activate)
        on -> stringResource(R.string.silence_a11y_on)
        else -> stringResource(R.string.silence_a11y_off)
    }
    val shape = RoundedCornerShape(50)
    val act = {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        if (listenerReady) onChange(Silence.toggle(level)) else onActivate()
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(FocoSpace.touch)
                .clip(shape)
                .background(if (on) FocoPaper else Color.Transparent)
                .border(
                    width = if (on) 2.dp else 1.dp,
                    color = if (on) FocoPaper else FocoLine,
                    shape = shape,
                )
                .clickable(onClick = act)
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = spoken
                    onClick { act(); true }
                }
                .padding(horizontal = FocoSpace.gapLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (on) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (on) FocoInk else FocoPaperDim.copy(alpha = 0.55f),
            )
            Text(
                text = visible,
                color = if (on) FocoInk else FocoPaperDim,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
