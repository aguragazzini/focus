package com.foco.launcher.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.launcher.FocoApp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.launcher.R
import com.foco.launcher.core.FocoInk
import com.foco.launcher.core.FocoLine
import com.foco.launcher.core.FocoPaper
import com.foco.launcher.core.FocoPaperDim
import com.foco.launcher.core.FocoSpace
import com.foco.launcher.core.FocoTextButton

/**
 * Full-screen pad. Dismiss does not launch. A wrong PIN clears the digits and stays.
 */
@Composable
fun PinGate(
    error: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PinEntry(
        title = stringResource(R.string.pin_title),
        error = if (error) stringResource(R.string.pin_wrong) else null,
        onComplete = onSubmit,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxSize()
            .background(FocoInk)
            .statusBarsPadding()
            .navigationBarsPadding(),
    )
}

@Composable
fun PinEntry(
    title: String,
    error: String?,
    onComplete: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var digits by remember(title, error) { mutableStateOf("") }
    Column(
        modifier = modifier.padding(horizontal = FocoSpace.page, vertical = FocoSpace.section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FocoTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.group_cancel))
            }
        }
        Spacer(Modifier.height(FocoSpace.section))
        Text(text = title, color = FocoPaper, textAlign = TextAlign.Center)
        Spacer(Modifier.height(FocoSpace.gap))
        Text(
            text = error.orEmpty(),
            color = FocoPaperDim,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FocoSpace.section))
        Row(horizontalArrangement = Arrangement.spacedBy(FocoSpace.gapLg)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (index < digits.length) FocoPaper else FocoPaperDim),
                )
            }
        }
        Spacer(Modifier.height(FocoSpace.sheet))
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫"),
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(FocoSpace.touch))
                    } else {
                        val deleteLabel = stringResource(R.string.pin_delete)
                        Box(
                            modifier = Modifier
                                .size(FocoSpace.touch + 16.dp)
                                .clickable {
                                    val next = when (key) {
                                        "⌫" -> digits.dropLast(1)
                                        else -> if (digits.length < 4) digits + key else digits
                                    }
                                    digits = next
                                    if (next.length == 4 && key != "⌫") {
                                        digits = ""
                                        onComplete(next)
                                    }
                                }
                                .then(
                                    if (key == "⌫") {
                                        Modifier.semantics { contentDescription = deleteLabel }
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = key,
                                color = FocoPaper,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(FocoSpace.gap))
        }
    }
}

private sealed interface PinFlow {
    data object Enable : PinFlow
    data class EnableConfirm(val first: String) : PinFlow
    data object Disable : PinFlow
    data object ChangeCurrent : PinFlow
    data class ChangeNext(val current: String) : PinFlow
    data class ChangeConfirm(val current: String, val next: String) : PinFlow
}

/** Settings row plus the set / confirm / disable pads. Does not gate Ajustes itself. */
@Composable
fun PinSettingsRow() {
    val app = LocalContext.current.applicationContext as FocoApp
    val enabled by app.pinStore.enabled.collectAsStateWithLifecycle()
    var flow by remember { mutableStateOf<PinFlow?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val wrong = stringResource(R.string.pin_wrong)
    val mismatch = stringResource(R.string.pin_mismatch)
    Column {
        SettingsPinSwitch(
            checked = enabled,
            onCheckedChange = { want ->
                error = null
                flow = if (want) PinFlow.Enable else PinFlow.Disable
            },
        )
        if (enabled) {
            FocoTextButton(onClick = {
                error = null
                flow = PinFlow.ChangeCurrent
            }) {
                Text(stringResource(R.string.pin_change))
            }
        }
    }
    val current = flow
    if (current != null) {
        val title = when (current) {
            PinFlow.Enable -> stringResource(R.string.pin_set)
            is PinFlow.EnableConfirm -> stringResource(R.string.pin_confirm)
            PinFlow.Disable -> stringResource(R.string.pin_disable)
            PinFlow.ChangeCurrent -> stringResource(R.string.pin_current)
            is PinFlow.ChangeNext -> stringResource(R.string.pin_next)
            is PinFlow.ChangeConfirm -> stringResource(R.string.pin_confirm)
        }
        Dialog(
            onDismissRequest = {
                error = null
                flow = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            PinEntry(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FocoInk),
                title = title,
                error = error,
                onDismiss = {
                    error = null
                    flow = null
                },
                onComplete = { pin ->
                    val store = app.pinStore
                    when (current) {
                        PinFlow.Enable -> {
                            error = null
                            flow = PinFlow.EnableConfirm(pin)
                        }
                        is PinFlow.EnableConfirm -> {
                            if (store.enable(current.first, pin)) {
                                error = null
                                flow = null
                            } else {
                                error = mismatch
                                flow = PinFlow.Enable
                            }
                        }
                        PinFlow.Disable -> {
                            if (store.disable(pin)) {
                                error = null
                                flow = null
                            } else {
                                error = wrong
                            }
                        }
                        PinFlow.ChangeCurrent -> {
                            if (store.verify(pin)) {
                                error = null
                                flow = PinFlow.ChangeNext(pin)
                            } else {
                                error = wrong
                            }
                        }
                        is PinFlow.ChangeNext -> {
                            error = null
                            flow = PinFlow.ChangeConfirm(current.current, pin)
                        }
                        is PinFlow.ChangeConfirm -> {
                            if (store.change(current.current, current.next, pin)) {
                                error = null
                                flow = null
                            } else {
                                error = mismatch
                                flow = PinFlow.ChangeNext(current.current)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsPinSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = FocoSpace.page, vertical = FocoSpace.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.pin_enable), color = FocoPaper)
            Spacer(Modifier.height(FocoSpace.hair))
            Text(text = stringResource(R.string.pin_enable_sub), color = FocoPaperDim)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocoInk,
                checkedTrackColor = FocoPaper,
                uncheckedThumbColor = FocoPaperDim,
                uncheckedTrackColor = FocoLine,
            ),
        )
    }
}
