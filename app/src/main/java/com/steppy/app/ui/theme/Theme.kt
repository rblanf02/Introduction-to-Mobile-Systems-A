package com.steppy.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SteppyColorScheme = lightColorScheme(
    primary = SteppyDarkNavy,
    onPrimary = SteppyCardWhite,
    secondary = SteppyTeal,
    onSecondary = SteppyCardWhite,
    background = SteppyBackground,
    onBackground = SteppyDarkNavy,
    surface = SteppyCardWhite,
    onSurface = SteppyDarkNavy,
    onSurfaceVariant = SteppyTextGray,
    error = SteppyErrorRed
)

@Composable
fun SteppyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SteppyColorScheme,
        typography = Typography,
        content = content
    )
}