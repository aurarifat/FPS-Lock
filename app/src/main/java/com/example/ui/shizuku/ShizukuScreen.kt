package com.example.ui.shizuku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Launch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.service.ShizukuManager
import com.example.ui.MainViewModel
import com.example.ui.components.GamingCard
import com.example.ui.components.HonestHardwareBanner
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun ShizukuScreen(viewModel: MainViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val shizukuOutput by viewModel.shizukuOutput.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.shizukuManager.checkStatus()
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isAuthorized = shizukuStatus == ShizukuManager.ShizukuStatus.AUTHORIZED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("shizuku_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Shizuku & Wireless ADB Manager",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Elevated system privileges without root access",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Status Card
        item {
            GamingCard(
                title = "Connection Status",
                icon = Icons.Default.Terminal,
                accentColor = if (isAuthorized) NeonGreen else WarningAmber
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val (title, color) = when (shizukuStatus) {
                            ShizukuManager.ShizukuStatus.AUTHORIZED -> "CONNECTED & AUTHORIZED" to NeonGreen
                            ShizukuManager.ShizukuStatus.PERMISSION_REQUIRED -> "PERMISSION REQUIRED" to WarningAmber
                            ShizukuManager.ShizukuStatus.SERVICE_STOPPED -> "SERVICE NOT RUNNING" to AlertRed
                            ShizukuManager.ShizukuStatus.NOT_INSTALLED -> "NOT INSTALLED" to TextMuted
                            ShizukuManager.ShizukuStatus.ERROR -> "COMMUNICATION ERROR" to AlertRed
                        }
                        StatusBadge(status = title, color = color)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (shizukuStatus) {
                                ShizukuManager.ShizukuStatus.AUTHORIZED -> "GameBoost is authorized to modify refresh rates and animation scales via system APIs."
                                ShizukuManager.ShizukuStatus.PERMISSION_REQUIRED -> "Shizuku service is running. Tap 'Request Permission' below to grant access."
                                ShizukuManager.ShizukuStatus.SERVICE_STOPPED -> "Shizuku app is present, but the background service is stopped. Start it via Wireless Debugging."
                                ShizukuManager.ShizukuStatus.NOT_INSTALLED -> "Shizuku is not installed. You can install it from GitHub or Google Play for ADB level controls."
                                ShizukuManager.ShizukuStatus.ERROR -> "Binder connection failed. Ensure Shizuku is updated to the latest version."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonPrimaryButton(
                        text = if (isAuthorized) "Re-check Binder" else "Request Permission",
                        icon = Icons.Default.Security,
                        onClick = {
                            if (shizukuStatus == ShizukuManager.ShizukuStatus.PERMISSION_REQUIRED) {
                                viewModel.shizukuManager.requestPermission()
                            } else {
                                viewModel.shizukuManager.checkStatus()
                            }
                        }
                    )

                    if (!isAuthorized) {
                        NeonOutlinedButton(
                            text = "Open Shizuku App",
                            icon = Icons.Default.Launch,
                            accentColor = WarningAmber,
                            onClick = { viewModel.shizukuManager.openShizukuApp() }
                        )
                    }

                    NeonOutlinedButton(
                        text = "Dev Options",
                        accentColor = CyberCyan,
                        onClick = { viewModel.openDeveloperOptions() }
                    )
                }
            }
        }

        // Shizuku FPS & Refresh Rate Controller
        item {
            GamingCard(
                title = "Hardware Refresh Rate Locker",
                icon = Icons.Default.Speed,
                accentColor = CyberCyan
            ) {
                Text(
                    text = "Infinix XPad 20 genuine display modes: ${deviceInfo.supportedRefreshRates.joinToString(", ") { "${it.toInt()}Hz" }}. Current: ${deviceInfo.currentRefreshRate.toInt()}Hz",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonOutlinedButton(
                        modifier = Modifier.weight(1f),
                        text = "Lock 60Hz",
                        accentColor = if (deviceInfo.currentRefreshRate == 60f) NeonGreen else CyberCyan,
                        enabled = isAuthorized,
                        onClick = {
                            scope.launch {
                                viewModel.shizukuManager.setRefreshRate(60f)
                                viewModel.refreshSettingsState()
                            }
                        }
                    )

                    NeonOutlinedButton(
                        modifier = Modifier.weight(1f),
                        text = "Lock 90Hz (XPad Peak)",
                        accentColor = if (deviceInfo.currentRefreshRate == 90f) NeonGreen else NeonGreen,
                        enabled = isAuthorized,
                        onClick = {
                            scope.launch {
                                viewModel.shizukuManager.setRefreshRate(90f)
                                viewModel.refreshSettingsState()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                NeonOutlinedButton(
                    text = "Restore System Auto Refresh Rate",
                    accentColor = ElectricBlue,
                    enabled = isAuthorized,
                    onClick = {
                        scope.launch {
                            viewModel.shizukuManager.executeCommand("settings delete system peak_refresh_rate && settings delete system min_refresh_rate")
                            viewModel.refreshSettingsState()
                        }
                    }
                )

                if (!isAuthorized) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect Shizuku to lock 90Hz or 60Hz without opening system menus.",
                        color = WarningAmber,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Wireless ADB Setup Instructions
        item {
            GamingCard(
                title = "Wireless Debugging Setup Guide (Android 15)",
                icon = Icons.Default.Wifi,
                accentColor = ElectricBlue
            ) {
                val steps = listOf(
                    "1. Open Settings -> About Tablet -> Tap 'Build Number' 7 times to enable Developer Options.",
                    "2. Go to Settings -> System -> Developer Options.",
                    "3. Turn on 'Wireless Debugging'. Tap on 'Wireless Debugging' to open details.",
                    "4. Tap 'Pair device with pairing code'. Note the 6-digit code and port number.",
                    "5. Open Shizuku notification or app -> Enter the pairing code.",
                    "6. Return to Shizuku and tap 'Start'. Shizuku is now running!",
                    "7. Return to GameBoost and tap 'Request Permission'."
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.forEach { step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Command Console Log
        item {
            GamingCard(
                title = "Authorized System Command Output",
                icon = Icons.Default.Code,
                accentColor = CyberCyan
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = shizukuOutput ?: "No commands executed yet. Output will appear here.",
                        color = if (shizukuOutput?.contains("ERR") == true) AlertRed else NeonGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonOutlinedButton(
                        text = "Test Read Refresh Rate",
                        enabled = isAuthorized,
                        onClick = {
                            scope.launch {
                                viewModel.shizukuManager.executeCommand("settings get system peak_refresh_rate")
                            }
                        }
                    )

                    NeonOutlinedButton(
                        text = "Test Read Animation Scale",
                        enabled = isAuthorized,
                        onClick = {
                            scope.launch {
                                viewModel.shizukuManager.executeCommand("settings get global window_animation_scale")
                            }
                        }
                    )
                }
            }
        }

        // Permissions Matrix Explanation
        item {
            GamingCard(
                title = "Permissions & Security Architecture",
                icon = Icons.Default.Security,
                accentColor = NeonGreen
            ) {
                Text(
                    text = "GameBoost strictly obeys Android security boundaries:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val matrix = listOf(
                    "Floating HUD Overlay" to "SYSTEM_ALERT_WINDOW (Android User Setting)",
                    "Do Not Disturb (DND)" to "ACCESS_NOTIFICATION_POLICY (System Setting)",
                    "Display Brightness" to "WRITE_SETTINGS (System Setting)",
                    "Lock 90Hz / 60Hz" to "Shizuku / Wireless ADB (System privilege)",
                    "UI Animation Scales" to "Shizuku / Wireless ADB (Global setting)"
                )

                matrix.forEach { (feat, perm) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(feat, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(perm, color = CyberCyan, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
