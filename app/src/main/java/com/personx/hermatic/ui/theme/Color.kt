package com.personx.hermatic.ui.theme

import androidx.compose.ui.graphics.Color

// Nous Research Inspired Palette
val NousBlack = Color(0xFF000000)
val NousWhite = Color(0xFFFFFFFF)
val NousGreen = Color(0xFF00FF00) // High-contrast vibrant green
val NousGrey = Color(0xFF121212)
val NousDarkGrey = Color(0xFF1E1E1E)
val NousTextGrey = Color(0xFFA0A0A0)

// Ambient Blob Colors
val BlobGreen = Color(0xFF00FF00).copy(alpha = 0.15f)
val BlobBlue = Color(0xFF0066FF).copy(alpha = 0.1f)
val BlobPurple = Color(0xFFAA00FF).copy(alpha = 0.1f)

// Material 3 Dark Scheme Mapping
val DarkPrimary = NousGreen
val DarkSecondary = NousWhite
val DarkTertiary = NousTextGrey
val DarkBackground = NousBlack
val DarkSurface = NousGrey
val DarkOnBackground = NousWhite
val DarkOnSurface = NousWhite
val DarkOnPrimary = NousBlack
