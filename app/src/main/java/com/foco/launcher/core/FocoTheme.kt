package com.foco.launcher.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Paper = Color(0xFFE8E4DC)
private val PaperMuted = Color(0xFF9A958C)
private val Ink = Color(0xFF121212)
private val Surface = Color(0xFF1C1C1C)
private val Line = Color(0xFF2A2A2A)

private val FocoColors = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    secondary = PaperMuted,
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Surface,
    onSurface = Paper,
    surfaceVariant = Line,
    onSurfaceVariant = PaperMuted,
    outline = Line,
    error = Color(0xFFC98980),
    onError = Ink,
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
                color = Paper,
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = Paper,
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Paper,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = PaperMuted,
            ),
            labelSmall = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = PaperMuted,
            ),
        ),
        content = content,
    )
}
