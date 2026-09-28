package com.foco.launcher.core

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foco.launcher.R
import com.foco.launcher.notification.Silence
import com.foco.launcher.notification.SilenceLevel

/**
 * Two states. Avisos does not cancel. Foco cancels everything the listener sees.
 * On is a struck bell, Paper at 11%, and a 1.5dp Paper border.
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
    val name = stringResource(R.string.silence_a11y_name)
    val stateLabel = stringResource(if (on) R.string.silence_a11y_on else R.string.silence_a11y_off)
    val hint = stringResource(if (on) R.string.silence_a11y_hint_off else R.string.silence_a11y_hint_on)
    val activateLabel = stringResource(R.string.silence_activate)
    val shape = RoundedCornerShape(50)
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val act = {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        if (listenerReady) onChange(Silence.toggle(level)) else onActivate()
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    val scale = if (pressed) 0.98f else 1f
                    scaleX = scale
                    scaleY = scale
                }
                .height(FocoSpace.touch)
                .clip(shape)
                .background(if (on) FocoPaper.copy(alpha = 0.11f) else FocoInkElevated)
                .border(
                    width = if (on) 1.5.dp else 1.dp,
                    color = if (on) FocoPaper else FocoLine,
                    shape = shape,
                )
                .clickable(
                    interactionSource = press,
                    indication = null,
                    onClick = act,
                )
                .clearAndSetSemantics {
                    if (listenerReady) {
                        role = Role.Switch
                        contentDescription = name
                        stateDescription = stateLabel
                        toggleableState = if (on) ToggleableState.On else ToggleableState.Off
                        onClick(label = hint) { act(); true }
                    } else {
                        role = Role.Button
                        contentDescription = activateLabel
                        onClick { act(); true }
                    }
                }
                .padding(horizontal = FocoSpace.gapLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (on) FocoPaper else FocoPaperDim,
                )
                if (on) {
                    Box(
                        Modifier
                            .width(16.dp)
                            .height(1.5.dp)
                            .rotate(-42f)
                            .background(FocoPaper),
                    )
                }
            }
            Text(
                text = visible,
                color = if (on) FocoPaper else FocoPaperDim,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
