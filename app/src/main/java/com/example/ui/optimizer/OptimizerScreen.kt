package com.example.ui.optimizer

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun OptimizerScreen(
    viewModel: MainViewModel,
    onNavigateToShizuku: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val animationScales by viewModel.animationScales.collectAsState()
    val systemBrightness by viewModel.systemBrightness.collectAsState()
    val isDndActive by viewModel.isDndActive.collectAsState()
    val isBatterySaverActive by viewModel.isBatterySaverActive.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val pingResult by viewModel.pingResult.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()
    val lagKillResult by viewModel.lagKillResult.collectAsState()
    val isKillingLag by viewModel.isKillingLag.collectAsState()

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

    var pingTargetInput by remember { mutableStateOf(viewModel.prefs.pingHost) }
    var brightnessSliderValue by remember { mutableFloatStateOf(systemBrightness.toFloat()) }
    var trimmedMemoryMessage by remember { mutableStateOf<String?>(null) }

    val hasDndPermission = permissionsState.hasDnd
    val hasWriteSettings = permissionsState.hasWriteSettings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("optimizer_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "System Optimization & Controls",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Hardware-safe telemetry, display tuning & background managers",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // FORCE TO KILL LAG SECTION
        item {
            GamingCard(
                title = "⚡ FORCE TO KILL LAG & FRAME DROPS",
                icon = Icons.Default.Refresh,
                accentColor = AlertRed
            ) {
                Text(
                    text = "Extreme Anti-Lag Protocol: Purges non-game background tasks, cleans cache, removes CPU throttling, and locks gaming DND shield.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonPrimaryButton(
                    text = if (isKillingLag) "Purging Background Stutter..." else "FORCE KILL LAG NOW",
                    icon = Icons.Default.Refresh,
                    accentColor = AlertRed,
                    enabled = !isKillingLag,
                    modifier = Modifier.fillMaxWidth().testTag("force_kill_lag_button"),
                    onClick = { viewModel.forceKillLag() }
                )

                if (lagKillResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Anti-Lag Report",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+${lagKillResult!!.ramFreedMb}MB Purged",
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            lagKillResult!!.details.forEach { detail ->
                                Text(
                                    text = "• $detail",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // FUNCTION 1: ANIMATION OPTIMIZER
        item {
            GamingCard(
                title = "UI Animation Optimizer",
                icon = Icons.Default.Animation,
                accentColor = NeonGreen
            ) {
                Text(
                    text = "Current Scales: Window ${animationScales.windowScale}x • Transition ${animationScales.transitionScale}x • Animator ${animationScales.animatorScale}x",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val scales = listOf(0.0f, 0.5f, 1.0f, 1.5f)
                    scales.forEach { s ->
                        val isSelected = animationScales.windowScale == s
                        NeonOutlinedButton(
                            modifier = Modifier.weight(1f),
                            text = if (s == 0f) "Off" else "${s}x",
                            accentColor = if (isSelected) NeonGreen else TextMuted,
                            onClick = {
                                if (shizukuStatus == ShizukuManager.ShizukuStatus.AUTHORIZED) {
                                    viewModel.applyAnimationScale(s)
                                } else {
                                    viewModel.openDeveloperOptions()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonOutlinedButton(
                        text = "Restore Defaults (1.0x)",
                        accentColor = CyberCyan,
                        onClick = {
                            if (shizukuStatus == ShizukuManager.ShizukuStatus.AUTHORIZED) {
                                viewModel.restoreAnimationScales()
                            } else {
                                viewModel.openDeveloperOptions()
                            }
                        }
                    )

                    if (shizukuStatus != ShizukuManager.ShizukuStatus.AUTHORIZED) {
                        NeonOutlinedButton(
                            text = "Developer Options",
                            accentColor = WarningAmber,
                            onClick = { viewModel.openDeveloperOptions() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                HonestHardwareBanner(
                    title = "Animation Scale Fact",
                    description = "Reducing animation scales accelerates Android window transitions and makes UI navigation feel snappier. It does NOT increase in-game 3D rendering frames per second.",
                    icon = Icons.Default.Info,
                    color = CyberCyan
                )
            }
        }

        // FUNCTION 3: REFRESH RATE CHECKER & SWITCHER
        item {
            GamingCard(
                title = "Refresh Rate & Display Modes",
                icon = Icons.Default.Refresh,
                accentColor = CyberCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Hardware Rate",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${deviceInfo.currentRefreshRate.toInt()} Hz",
                            color = NeonGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Supported Modes",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = deviceInfo.supportedRefreshRates.joinToString(", ") { "${it.toInt()}Hz" },
                            color = CyberCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (shizukuStatus == ShizukuManager.ShizukuStatus.AUTHORIZED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deviceInfo.supportedRefreshRates.forEach { rate ->
                            NeonOutlinedButton(
                                modifier = Modifier.weight(1f),
                                text = "Lock ${rate.toInt()}Hz",
                                accentColor = if (deviceInfo.currentRefreshRate == rate) NeonGreen else CyberCyan,
                                onClick = {
                                    scope.launch {
                                        viewModel.shizukuManager.setRefreshRate(rate)
                                        viewModel.refreshSettingsState()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Connect Shizuku to lock display refresh rate directly without developer menu.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HonestHardwareBanner(
                    title = "Hardware Limitation Guarantee",
                    description = "Infinix XPad 20 features a maximum 90Hz display. Apps that claim to unlock 120Hz or 144Hz on this hardware are misleading. GameBoost strictly enforces genuine hardware limits.",
                    icon = Icons.Default.Info,
                    color = CyberCyan
                )
            }
        }

        // FUNCTION 4: BACKGROUND APP & MEMORY MANAGER
        item {
            GamingCard(
                title = "Background Memory Manager",
                icon = Icons.Default.CleaningServices,
                accentColor = NeonGreen
            ) {
                val usedMb = telemetry.ramUsedBytes / (1024 * 1024)
                val totalMb = telemetry.ramTotalBytes / (1024 * 1024)
                val pct = if (totalMb > 0) (usedMb * 100 / totalMb) else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RAM Usage: ${String.format("%.1f", usedMb / 1024f)}GB / ${String.format("%.1f", totalMb / 1024f)}GB ($pct%)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (usedMb.toFloat() / totalMb.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonGreen,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonPrimaryButton(
                        text = "Safe Memory Trim",
                        icon = Icons.Default.CleaningServices,
                        onClick = {
                            val freed = viewModel.systemMonitor.trimBackgroundMemory()
                            trimmedMemoryMessage = "Safely recovered ~${freed}MB from background tasks"
                        }
                    )

                    Text(
                        text = "${installedApps.size} apps scanned",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                if (trimmedMemoryMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = trimmedMemoryMessage!!,
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HonestHardwareBanner(
                    title = "System Protection Rule",
                    description = "Essential Android OS services and critical framework processes are never killed. Android manages memory dynamically; clearing cache frees temporary memory without compromising stability.",
                    icon = Icons.Default.Info,
                    color = CyberCyan
                )
            }
        }

        // FUNCTION 6: NETWORK MONITOR & REAL PING TEST
        item {
            GamingCard(
                title = "Network Latency & Ping Monitor",
                icon = Icons.Default.Wifi,
                accentColor = ElectricBlue
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Connection", color = TextMuted, fontSize = 11.sp)
                        Text(telemetry.networkType, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Link Speed", color = TextMuted, fontSize = 11.sp)
                        Text("${telemetry.networkSpeedMbps} Mbps", color = CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pingTargetInput,
                    onValueChange = { pingTargetInput = it },
                    label = { Text("Ping Target Host (e.g. 8.8.8.8, 1.1.1.1)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonPrimaryButton(
                        text = if (isPinging) "Testing Ping..." else "Start Ping Test",
                        icon = Icons.Default.NetworkCheck,
                        enabled = !isPinging,
                        onClick = { viewModel.runPingTest(pingTargetInput) }
                    )

                    if (isPinging) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyberCyan)
                    }
                }

                if (pingResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val (ms, desc) = pingResult!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(
                                    status = if (ms > 0) "${ms}ms" else "ERR",
                                    color = if (ms in 1..60) NeonGreen else if (ms > 60) WarningAmber else AlertRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (ms in 1..60) "Low Latency (Optimal)" else if (ms > 60) "High Latency" else "Packet Loss",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = desc, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HonestHardwareBanner(
                    title = "Real-World Network Truth",
                    description = "Game ping is determined by physical routing and your internet service provider. No app can artificially bypass distance or physical network latency.",
                    icon = Icons.Default.Info,
                    color = CyberCyan
                )
            }
        }

        // FUNCTION 7: DO NOT DISTURB MANAGER
        item {
            GamingCard(
                title = "Do Not Disturb (DND) Interruption Guard",
                icon = Icons.Default.NotificationsOff,
                accentColor = WarningAmber
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDndActive) "Gaming DND is ACTIVE" else "DND is OFF",
                            color = if (isDndActive) WarningAmber else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasDndPermission) "Notification policy access granted" else "Permission required to mute incoming notifications",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    if (hasDndPermission) {
                        Switch(
                            checked = isDndActive,
                            onCheckedChange = { viewModel.toggleDnd(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = WarningAmber
                            )
                        )
                    } else {
                        NeonOutlinedButton(
                            text = "Grant Access",
                            accentColor = WarningAmber,
                            onClick = { viewModel.openNotificationPolicySettings() }
                        )
                    }
                }
            }
        }

        // FUNCTION 9: BRIGHTNESS CONTROLLER
        item {
            GamingCard(
                title = "Display Brightness Controller",
                icon = Icons.Default.WbSunny,
                accentColor = CyberCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brightness Level: ${(brightnessSliderValue * 100 / 255).toInt()}%",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!hasWriteSettings) {
                        NeonOutlinedButton(
                            text = "Enable Write",
                            accentColor = WarningAmber,
                            onClick = { viewModel.openWriteSettings() }
                        )
                    }
                }

                Slider(
                    value = brightnessSliderValue,
                    onValueChange = {
                        brightnessSliderValue = it
                        if (hasWriteSettings) {
                            viewModel.setSystemBrightness(it.toInt())
                        }
                    },
                    valueRange = 10f..255f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )

                Text(
                    text = "You can also define per-game brightness levels in the Game Launcher tab.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // FUNCTION 2 & 10: BATTERY SAVER & BACKGROUND BATTERY MANAGER
        item {
            GamingCard(
                title = "Battery Saver & Throttling Monitor",
                icon = Icons.Default.BatteryAlert,
                accentColor = WarningAmber
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatterySaverActive) "Battery Saver: ON (Throttling Active)" else "Battery Saver: OFF (Maximum Performance)",
                            color = if (isBatterySaverActive) WarningAmber else NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Battery Saver lowers CPU/GPU clock speeds and forces 60Hz. Keep it OFF for optimal gaming.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    NeonOutlinedButton(
                        text = "Battery Settings",
                        accentColor = CyberCyan,
                        onClick = { viewModel.openBatterySaverSettings() }
                    )
                }
            }
        }

        // FUNCTION 5: STORAGE CLEANER
        item {
            GamingCard(
                title = "Storage Usage & Cleanup",
                icon = Icons.Default.Storage,
                accentColor = ElectricBlue
            ) {
                val totalGb = deviceInfo.totalStorageBytes / (1024 * 1024 * 1024)
                val freeGb = deviceInfo.freeStorageBytes / (1024 * 1024 * 1024)
                val usedGb = totalGb - freeGb
                val storagePct = if (totalGb > 0) (usedGb * 100 / totalGb).toInt() else 0

                Text(
                    text = "Storage: ${usedGb}GB used / ${totalGb}GB total ($storagePct%)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (storagePct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ElectricBlue,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${freeGb}GB Free Space",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    NeonOutlinedButton(
                        text = "Open Storage Cleaner",
                        icon = Icons.Default.Storage,
                        accentColor = ElectricBlue,
                        onClick = { viewModel.openStorageSettings() }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
