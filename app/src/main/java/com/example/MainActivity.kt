package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.launcher.GameLauncherScreen
import com.example.ui.navigation.Screen
import com.example.ui.optimizer.OptimizerScreen
import com.example.ui.overlay.OverlayConfigScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.shizuku.ShizukuScreen
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GameBoostTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GameBoostTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                val telemetry by viewModel.telemetry.collectAsState()

                BackHandler(enabled = currentScreen != Screen.Dashboard) {
                    currentScreen = Screen.Dashboard
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_game_logo),
                                            contentDescription = "GameBoost Logo",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "GameBoost",
                                                color = TextPrimary,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StatusBadge(
                                                status = "${telemetry.displayRefreshRate.toInt()}Hz",
                                                color = CyberCyan
                                            )
                                        }
                                        Text(
                                            text = currentScreen.title,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentScreen = Screen.SettingsScreen },
                                    modifier = Modifier.testTag("top_bar_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = if (currentScreen == Screen.SettingsScreen) NeonGreen else TextSecondary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkSurface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(0.dp))
                        ) {
                            Screen.getBottomNavItems().forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonGreen,
                                        selectedTextColor = NeonGreen,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = DarkSurface
                                    ),
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(ObsidianBg)
                    ) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToLauncher = { currentScreen = Screen.Launcher },
                                onNavigateToOptimizer = { currentScreen = Screen.Optimizer },
                                onNavigateToShizuku = { currentScreen = Screen.Shizuku },
                                onNavigateToOverlay = { currentScreen = Screen.Overlay }
                            )
                            Screen.Launcher -> GameLauncherScreen(viewModel = viewModel)
                            Screen.Optimizer -> OptimizerScreen(
                                viewModel = viewModel,
                                onNavigateToShizuku = { currentScreen = Screen.Shizuku }
                            )
                            Screen.Shizuku -> ShizukuScreen(viewModel = viewModel)
                            Screen.Overlay -> OverlayConfigScreen(viewModel = viewModel)
                            Screen.SettingsScreen -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.refreshSettingsState()
    }
}
