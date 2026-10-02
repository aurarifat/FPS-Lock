package com.example.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val hasDnd = permissionsState.hasDnd
    val hasOverlay = permissionsState.hasOverlay
    val hasWriteSettings = permissionsState.hasWriteSettings
    val hasUsageStats = permissionsState.hasUsageStats
    val hasNotifications = permissionsState.hasNotifications
    val isShizukuAuthorized = permissionsState.isShizukuAuthorized

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "App Settings & Hardware Profile",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Appearance, tactile haptics & permission checklist",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        // APPEARANCE & THEME CARD
        item {
            GamingCard(
                title = "Appearance & Interface",
                icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                accentColor = if (isDarkMode) CyberCyan else WarningAmber
            ) {
                // Dark / Light Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDarkMode) "Dark Stealth Theme" else "Light Solar Theme",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Synchronizes main app and floating FPS HUD overlay theme",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = NeonGreen
                        ),
                        modifier = Modifier.testTag("settings_theme_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Haptic Feedback Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactile Haptic Feedback",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sensory vibration on button taps, lag purges, and HUD dragging",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isHapticEnabled,
                        onCheckedChange = { viewModel.toggleHapticFeedback(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("settings_haptic_switch")
                    )
                }
            }
        }

        // Permissions Checklist Card
        item {
            GamingCard(
                title = "Android Permissions Checklist",
                icon = Icons.Default.Security,
                accentColor = NeonGreen
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Privileges & Guards",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    NeonOutlinedButton(
                        text = "Re-check All",
                        icon = Icons.Default.Refresh,
                        accentColor = NeonGreen,
                        onClick = { viewModel.refreshPermissions() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val perms = listOf(
                    Quad("Floating HUD Overlay", hasOverlay, "Required for floating telemetry bubble") {
                        viewModel.openOverlaySettings()
                    },
                    Quad("Do Not Disturb Policy", hasDnd, "Required to mute incoming game interruptions") {
                        viewModel.openNotificationPolicySettings()
                    },
                    Quad("System Write Settings", hasWriteSettings, "Required for custom per-game brightness") {
                        viewModel.openWriteSettings()
                    },
                    Quad("Usage Access (App Stats)", hasUsageStats, "Required for monitoring active games & memory") {
                        viewModel.openUsageAccessSettings()
                    },
                    Quad("System Notifications", hasNotifications, "Required for persistent GameBoost service") {
                        viewModel.openNotificationSettings()
                    },
                    Quad("Shizuku / Wireless ADB", isShizukuAuthorized, "Required for locking 90Hz & animation scales") {
                        if (permissionsState.shizukuStatus == ShizukuManager.ShizukuStatus.PERMISSION_REQUIRED) {
                            viewModel.shizukuManager.requestPermission()
                        } else if (permissionsState.shizukuStatus == ShizukuManager.ShizukuStatus.SERVICE_STOPPED ||
                            permissionsState.shizukuStatus == ShizukuManager.ShizukuStatus.NOT_INSTALLED
                        ) {
                            viewModel.shizukuManager.openShizukuApp()
                        } else {
                            viewModel.refreshPermissions()
                        }
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    perms.forEach { (name, granted, desc, onAction) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Dangerous,
                                    contentDescription = null,
                                    tint = if (granted) NeonGreen else WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(desc, color = TextMuted, fontSize = 10.sp)
                                }
                            }

                            if (!granted) {
                                NeonOutlinedButton(
                                    text = "Grant",
                                    accentColor = WarningAmber,
                                    onClick = onAction
                                )
                            } else {
                                StatusBadge(status = "ACTIVE", color = NeonGreen)
                            }
                        }
                    }
                }
            }
        }

        // Hardware Profile Disclosures (Infinix XPad 20)
        item {
            GamingCard(
                title = "Hardware Specifications & Architecture",
                icon = Icons.Default.Tune,
                accentColor = CyberCyan
            ) {
                val specs = listOf(
                    "Target Tablet" to "Infinix XPad 20 (MT6769 Helio G88)",
                    "Detected Device" to deviceInfo.deviceModel,
                    "Android Version" to deviceInfo.androidVersion,
                    "Total System RAM" to "${String.format("%.1f", deviceInfo.totalRamBytes / (1024f * 1024 * 1024))} GB (6GB Hardware)",
                    "Native Display" to "${deviceInfo.supportedRefreshRates.joinToString(" / ") { "${it.toInt()}Hz" }} (Genuine Hardware Modes)",
                    "Current Refresh Rate" to "${deviceInfo.currentRefreshRate.toInt()} Hz"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    specs.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = TextMuted, fontSize = 12.sp)
                            Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                HonestHardwareBanner(
                    title = "Hardware Limitations Transparency",
                    description = "GameBoost never claims unsupported 120Hz display frequencies or fictitious GPU overclocking. The Infinix XPad 20 features a 90Hz panel and MediaTek Helio G88. All metrics displayed are genuine Android OS telemetry.",
                    icon = Icons.Default.Info,
                    color = CyberCyan
                )
            }
        }

        // Restore Defaults
        item {
            GamingCard(
                title = "Restore Original System Settings",
                icon = Icons.Default.Restore,
                accentColor = WarningAmber
            ) {
                Text(
                    text = "Reverts any brightness overrides, resets animation scales to their original values, and turns off Do Not Disturb.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonOutlinedButton(
                    text = "Restore All System Defaults",
                    icon = Icons.Default.Restore,
                    accentColor = WarningAmber,
                    onClick = { showRestoreDialog = true }
                )

                if (restoreMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(restoreMessage!!, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // About & Version
        item {
            GamingCard(
                title = "About GameBoost",
                icon = Icons.Default.Info,
                accentColor = ElectricBlue
            ) {
                Text(
                    text = "GameBoost v1.0.0 (Native Android 15 & Infinix XPad 20 Edition)\nBuilt with Kotlin, Jetpack Compose, Material 3, Room, and Rikka Shizuku API.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore System Defaults?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will restore original UI animation scales (1.0x), reset brightness to auto/default, and disable gaming DND mode.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                NeonPrimaryButton(
                    text = "Confirm Restore",
                    onClick = {
                        viewModel.restoreAnimationScales()
                        viewModel.toggleDnd(false)
                        restoreMessage = "System defaults successfully restored!"
                        showRestoreDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
