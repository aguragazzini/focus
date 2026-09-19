package com.foco.launcher.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.launcher.R
import com.foco.launcher.registry.LaunchableApp
import com.foco.launcher.registry.SuggestedApp
import com.foco.launcher.registry.SuggestedKind

@Composable
fun SetupScreen(
    state: SetupUiState,
    onStart: () -> Unit,
    onToggle: (String) -> Unit,
    onContinueApps: () -> Unit,
    onChooseDefault: () -> Unit,
    onSkipDefault: () -> Unit,
    onBackToApps: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (state.step) {
            SetupStep.Welcome -> WelcomeStep(onStart)
            SetupStep.PickApps -> PickAppsStep(state, onToggle, onContinueApps)
            SetupStep.SetDefault -> DefaultStep(state, onChooseDefault, onSkipDefault, onBackToApps)
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.welcome_cta))
        }
    }
}

@Composable
private fun PickAppsStep(
    state: SetupUiState,
    onToggle: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_sub),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(state.suggested, key = { it.packageName }) { app ->
                SuggestedRow(
                    app = app,
                    checked = app.packageName in state.selected,
                    onToggle = { onToggle(app.packageName) },
                )
            }
            if (state.others.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = stringResource(R.string.setup_other_apps),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(state.others, key = { it.packageName }) { app ->
                    LaunchableRow(
                        app = app,
                        checked = app.packageName in state.selected,
                        onToggle = { onToggle(app.packageName) },
                    )
                }
            }
        }
        Button(
            onClick = onContinue,
            enabled = state.canContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.setup_continue))
        }
    }
}

@Composable
private fun DefaultStep(
    state: SetupUiState,
    onChooseDefault: () -> Unit,
    onSkipDefault: () -> Unit,
    onBackToApps: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.default_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.default_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!state.canSetDefault) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.default_blocked),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onBackToApps) {
                Text(stringResource(R.string.setup_need_settings))
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onChooseDefault,
            enabled = state.canSetDefault,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.default_cta))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkipDefault) {
            Text(stringResource(R.string.default_skip))
        }
    }
}

@Composable
private fun SuggestedRow(app: SuggestedApp, checked: Boolean, onToggle: () -> Unit) {
    val title = when (app.kind) {
        SuggestedKind.PHONE -> stringResource(R.string.setup_suggested_phone)
        SuggestedKind.SETTINGS -> stringResource(R.string.setup_suggested_settings)
        SuggestedKind.MESSAGES -> stringResource(R.string.setup_suggested_messages)
        SuggestedKind.CAMERA -> stringResource(R.string.setup_suggested_camera)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (app.label != title) {
                Text(text = app.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LaunchableRow(app: LaunchableApp, checked: Boolean, onToggle: () -> Unit) {
    val bitmap = remember(app.packageName, app.icon) { app.icon.asImageBitmap() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
