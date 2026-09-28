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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.rotate
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
 * One pill. The visible word stays «Foco».
 * Outline bell is off, a filled bell is in-scope, a struck bell and a Paper border cancel everything.
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
    val activateLabel = stringResource(R.string.silence_activate)
    val focoLabel = stringResource(R.string.silence_foco)
    val spoken = when {
        !listenerReady -> activateLabel
        level == SilenceLevel.TODO -> stringResource(R.string.silence_all)
        level == SilenceLevel.AVISOS -> focoLabel
        else -> stringResource(R.string.silence_off)
    }
    val visible = if (listenerReady) focoLabel else activateLabel
    val inScope = listenerReady && level == SilenceLevel.AVISOS
    val cancelAll = listenerReady && level == SilenceLevel.TODO
    val active = inScope || cancelAll
    val textColor = if (active) FocoPaper else FocoPaperDim
    val shape = RoundedCornerShape(50)
    val act = {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        if (listenerReady) onChange(Silence.next(level)) else onActivate()
    }
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
                    width = if (cancelAll) 2.dp else 1.dp,
                    color = if (active) FocoPaper else FocoLine,
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
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (active) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textColor.copy(alpha = if (active) 1f else 0.55f),
                )
                if (cancelAll) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(1.5.dp)
                            .rotate(-42f)
                            .background(FocoInk),
                    )
                }
            }
            Text(
                text = visible,
                color = textColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
