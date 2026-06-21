package com.personx.hermatic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun HermaticTheme(
    isDark: Boolean = true,
    primaryColor: Color = NousWhite,
    accentColor: Color = Color(0xFF00FF00),
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = if (primaryColor == NousWhite) Color.Black else Color.White,
            secondary = accentColor,
            onSecondary = Color.Black,
            background = NousBlack,
            onBackground = NousWhite,
            surface = NousDarkGrey,
            onSurface = NousWhite,
            tertiary = NousTextGrey
        )
    } else {
        lightColorScheme(
            primary = if (primaryColor == NousWhite) Color.Black else primaryColor,
            onPrimary = Color.White,
            secondary = accentColor,
            onSecondary = Color.White,
            background = NousWhite,
            onBackground = NousBlack,
            surface = Color.LightGray,
            onSurface = NousBlack,
            tertiary = Color.Gray
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
