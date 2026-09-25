package com.foco.launcher.core

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val FocoInk = Color(0xFF0E0E0E)
internal val FocoInkElevated = Color(0xFF171717)
internal val FocoPaper = Color(0xFFE6E6E6)
internal val FocoPaperDim = Color(0xFF8A8A8A)
internal val FocoLine = Color(0xFF2A2A2A)

private val FocoColors = darkColorScheme(
    primary = FocoPaper,
    onPrimary = FocoInk,
    primaryContainer = FocoLine,
    onPrimaryContainer = FocoPaper,
    inversePrimary = FocoPaperDim,
    secondary = FocoPaperDim,
    onSecondary = FocoInk,
    secondaryContainer = FocoLine,
    onSecondaryContainer = FocoPaper,
    tertiary = FocoPaperDim,
    onTertiary = FocoInk,
    tertiaryContainer = FocoLine,
    onTertiaryContainer = FocoPaper,
    background = FocoInk,
    onBackground = FocoPaper,
    surface = FocoInkElevated,
    onSurface = FocoPaper,
    surfaceVariant = FocoLine,
    onSurfaceVariant = FocoPaperDim,
    surfaceTint = Color.Transparent,
    inverseSurface = FocoPaper,
    inverseOnSurface = FocoInk,
    error = FocoPaperDim,
    onError = FocoInk,
    errorContainer = FocoLine,
    onErrorContainer = FocoPaper,
    outline = FocoLine,
    outlineVariant = FocoLine,
    scrim = FocoInk,
    surfaceBright = FocoInkElevated,
    surfaceDim = FocoInk,
    surfaceContainer = FocoInkElevated,
    surfaceContainerHigh = FocoInkElevated,
    surfaceContainerHighest = FocoLine,
    surfaceContainerLow = FocoInk,
    surfaceContainerLowest = FocoInk,
)

@Composable
fun FocoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FocoColors,
        typography = MaterialTheme.typography.copy(
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                color = FocoPaper,
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                color = FocoPaper,
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = FocoPaper,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = FocoPaperDim,
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = FocoPaper,
            ),
        ),
        content = content,
    )
}

/**
 * Single filled CTA: Paper fill, Ink label. Never Paper text on Paper fill.
 */
@Composable
fun FocoPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val label = if (enabled) FocoInk else FocoInk.copy(alpha = 0.38f)
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = FocoPaper,
            contentColor = label,
            disabledContainerColor = FocoPaper.copy(alpha = 0.38f),
            disabledContentColor = label,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides label) {
            ProvideTextStyle(TextStyle(color = label), content)
        }
    }
}

/** Secondary action: Paper text, no fill. */
@Composable
fun FocoTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val label = if (enabled) FocoPaper else FocoPaperDim
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = label,
            disabledContentColor = FocoPaperDim,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides label) {
            ProvideTextStyle(TextStyle(color = label), content)
        }
    }
}

@Composable
fun FocoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = FocoInk,
            checkedTrackColor = FocoPaper,
            checkedBorderColor = FocoPaper,
            uncheckedThumbColor = FocoPaperDim,
            uncheckedTrackColor = FocoInkElevated,
            uncheckedBorderColor = FocoPaperDim,
        ),
    )
}
