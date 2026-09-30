package com.example.ui.dashboard

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StepStatus
import com.example.service.ShizukuManager
import com.example.ui.MainViewModel
import com.example.ui.components.GamingCard
import com.example.ui.components.HonestHardwareBanner
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
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
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToLauncher: () -> Unit,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToShizuku: () -> Unit,
    onNavigateToOverlay: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val liveFps by viewModel.liveFps.collectAsState()
    val stability by viewModel.stabilityState.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val isOptimizing by viewModel.isOptimizing.collectAsState()
    val optimizationResult by viewModel.optimizationState.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val gameProfiles by viewModel.gameProfiles.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()
    val isKillingLag by viewModel.isKillingLag.collectAsState()
    val lagKillResult by viewModel.lagKillResult.collectAsState()

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

    val usedRamMb = telemetry.ramUsedBytes / (1024 * 1024)
    val totalRamMb = telemetry.ramTotalBytes / (1024 * 1024)
    val ramProgress = if (totalRamMb > 0) usedRamMb.toFloat() / totalRamMb.toFloat() else 0.5f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("dashboard_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Banner & Hardware Tag
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_game_hero),
                    contentDescription = "GameBoost Cockpit Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.95f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(
                            status = if (deviceInfo.isXPadDetected) "INFINIX XPAD 20 DETECTED" else "TARGET DEVICE",
                            color = NeonGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            status = "60Hz/90Hz Display",
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "GameBoost Gaming Control",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${deviceInfo.chipset} • Android 15 • 6GB RAM",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Live Telemetry 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE HARDWARE TELEMETRY",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (stability.isStable) NeonGreen else WarningAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stability.message,
                            color = if (stability.isStable) NeonGreen else WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Render FPS",
                        value = "$liveFps",
                        subValue = "Display: ${telemetry.displayRefreshRate.toInt()}Hz",
                        icon = Icons.Default.Speed,
                        accentColor = NeonGreen
                    )
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "RAM Usage",
                        value = "${String.format("%.1f", usedRamMb / 1024f)}GB",
                        subValue = "${(ramProgress * 100).toInt()}% of 6GB",
                        icon = Icons.Default.Memory,
                        accentColor = CyberCyan,
                        progress = ramProgress
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Battery & Temp",
                        value = "${telemetry.batteryPercent}%",
                        subValue = String.format("%.1f°C %s", telemetry.batteryTemperatureC, if (telemetry.isCharging) "⚡" else ""),
                        icon = Icons.Default.BatteryChargingFull,
                        accentColor = if (telemetry.batteryTemperatureC > 40f) WarningAmber else StatusSuccess
                    )
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Network",
                        value = telemetry.networkType,
                        subValue = if (telemetry.pingMs > 0) "${telemetry.pingMs}ms" else "Tap for Ping",
                        icon = Icons.Default.Wifi,
                        accentColor = ElectricBlue,
                        onClick = { onNavigateToOptimizer() }
                    )
                }
            }
        }

        // FORCE TO KILL LAG CTA
        item {
            GamingCard(
                title = "⚡ FORCE TO KILL LAG & PURGE STUTTER",
                icon = Icons.Default.FlashOn,
                accentColor = AlertRed
            ) {
                Text(
                    text = "Aggressive lag terminator for intense games like Free Fire MAX: force-terminates non-essential background tasks, frees physical RAM, prevents CPU throttling, and enables Do Not Disturb shield.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonPrimaryButton(
                    text = if (isKillingLag) "Purging System Stutter..." else "FORCE KILL LAG NOW",
                    icon = Icons.Default.FlashOn,
                    accentColor = AlertRed,
                    enabled = !isKillingLag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_force_kill_lag_button"),
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
                                    text = "Lag Termination Successful",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+${lagKillResult!!.ramFreedMb}MB Freed",
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

        // ONE-TAP OPTIMIZE CTA
        item {
            GamingCard(
                title = "One-Tap Gaming Boost",
                icon = Icons.Default.Bolt,
                accentColor = NeonGreen
            ) {
                Text(
                    text = "Applies verified, permission-authorized optimizations: safely trims background memory, locks peak refresh rate, and checks gaming DND guard.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonPrimaryButton(
                    text = if (isOptimizing) "Optimizing System..." else "Run Safe One-Tap Boost",
                    icon = Icons.Default.Bolt,
                    enabled = !isOptimizing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("one_tap_optimize_button"),
                    onClick = { viewModel.runOneTapOptimize() }
                )

                // Optimization Live Progress / Results Card
                if (optimizationResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Optimization Results",
                                    color = NeonGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+${optimizationResult!!.ramFreedMb}MB Freed",
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            optimizationResult!!.steps.forEach { step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (badgeText, badgeColor) = when (step.status) {
                                        StepStatus.SUCCESS -> "DONE" to NeonGreen
                                        StepStatus.RUNNING -> "..." to CyberCyan
                                        StepStatus.PERMISSION_NEEDED -> "PERMISSION" to WarningAmber
                                        StepStatus.SKIPPED -> "SKIPPED" to TextMuted
                                        StepStatus.FAILED -> "ERROR" to AlertRed
                                        StepStatus.PENDING -> "WAIT" to TextMuted
                                    }
                                    StatusBadge(status = badgeText, color = badgeColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = step.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = step.details,
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Launch Featured Game (Free Fire MAX)
        item {
            val featuredGame = gameProfiles.firstOrNull { it.isFavorite } ?: gameProfiles.firstOrNull()
            GamingCard(
                title = "Quick Game Launcher",
                icon = Icons.Default.PlayArrow,
                accentColor = CyberCyan
            ) {
                if (featuredGame != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = featuredGame.gameName,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Profile: 90Hz Target • DND On • Floating HUD",
                                color = CyberCyan,
                                fontSize = 11.sp
                            )
                        }

                        NeonPrimaryButton(
                            text = "Launch",
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.testTag("quick_launch_button"),
                            onClick = { viewModel.launchGameWithProfile(featuredGame) }
                        )
                    }
                } else {
                    Text(
                        text = "No games added yet. Open Game Launcher to add Free Fire MAX or your favorite games.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NeonOutlinedButton(
                        text = "Browse Games",
                        icon = Icons.Default.PlayArrow,
                        onClick = onNavigateToLauncher
                    )
                }
            }
        }

        // Floating HUD Quick Controller
        item {
            GamingCard(
                title = "Floating Gaming HUD Bubble",
                icon = Icons.Default.Speed,
                accentColor = ElectricBlue
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOverlayActive) "Overlay is Active" else "Overlay Disabled",
                            color = if (isOverlayActive) NeonGreen else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time draggable FPS, Hz, RAM and Temp bubble over games",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isOverlayActive,
                        onCheckedChange = { active ->
                            viewModel.toggleOverlay(active)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }

        // Shizuku Status Bar
        item {
            GamingCard(
                title = "Shizuku & Wireless ADB Status",
                icon = Icons.Default.Terminal,
                accentColor = CyberCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val statusDesc = when (shizukuStatus) {
                            ShizukuManager.ShizukuStatus.AUTHORIZED -> "Authorized: System refresh rates and animation scales accessible"
                            ShizukuManager.ShizukuStatus.PERMISSION_REQUIRED -> "Shizuku running: Permission grant required"
                            ShizukuManager.ShizukuStatus.SERVICE_STOPPED -> "Shizuku installed but service not running"
                            ShizukuManager.ShizukuStatus.NOT_INSTALLED -> "Shizuku not installed on device"
                            ShizukuManager.ShizukuStatus.ERROR -> "Error communicating with Shizuku binder"
                        }
                        val statusColor = when (shizukuStatus) {
                            ShizukuManager.ShizukuStatus.AUTHORIZED -> NeonGreen
                            ShizukuManager.ShizukuStatus.PERMISSION_REQUIRED -> WarningAmber
                            else -> TextMuted
                        }
                        Text(
                            text = shizukuStatus.name.replace("_", " "),
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = statusDesc,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonOutlinedButton(
                        text = "Setup",
                        accentColor = CyberCyan,
                        onClick = onNavigateToShizuku
                    )
                }
            }
        }

        // Device Truth Disclaimer
        item {
            HonestHardwareBanner(
                title = "Infinix XPad 20 Hardware Facts",
                description = "Equipped with a genuine 90Hz display and MediaTek Helio G88 processor. GameBoost never claims unsupported 120Hz modes or fake GPU clock increases. Clearing RAM frees background task memory safely.",
                icon = Icons.Default.Tune,
                color = CyberCyan
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
