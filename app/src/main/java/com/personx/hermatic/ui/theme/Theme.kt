package com.personx.hermatic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NousDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = NousBlack,
    tertiary = DarkTertiary,
    onTertiary = NousWhite,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = NousDarkGrey,
    onSurfaceVariant = NousWhite,
    outline = NousTextGrey
)

@Composable
fun HermaticTheme(
    content: @Composable () -> Unit
) {
    // Hermatic uses a fixed high-contrast dark theme by default
    // to match the Nous Research portal aesthetic.
    MaterialTheme(
        colorScheme = NousDarkColorScheme,
        typography = Typography,
        content = content
    )
}
