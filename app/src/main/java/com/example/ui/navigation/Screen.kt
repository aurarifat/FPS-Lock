package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Booster", Icons.Default.Bolt)
    data object Launcher : Screen("launcher", "Games", Icons.Default.SportsEsports)
    data object Optimizer : Screen("optimizer", "Optimize", Icons.Default.Build)
    data object Shizuku : Screen("shizuku", "Shizuku", Icons.Default.Terminal)
    data object Overlay : Screen("overlay", "HUD Overlay", Icons.Default.Layers)
    data object SettingsScreen : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        fun getBottomNavItems(): List<Screen> = listOf(
            Dashboard,
            Launcher,
            SettingsScreen
        )
    }
}
