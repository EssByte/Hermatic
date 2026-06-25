package com.personx.hermatic.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    data object Chat : Screen("chat", Icons.AutoMirrored.Filled.Chat)
    data object Voice : Screen("voice", Icons.Default.Mic)
    data object Skills : Screen("skills", Icons.Default.Build)
    data object Settings : Screen("settings", Icons.Default.Settings)
}
