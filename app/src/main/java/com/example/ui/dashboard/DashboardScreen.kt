package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.components.GamingCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.telemetry.FpsMode
import com.example.telemetry.StabilityStatus
import com.example.telemetry.RefreshControlMethod

/**
 * Simplified, high-performance Game Booster Dashboard.
 * Focuses on prominent 1-tap Force 90 FPS Global Lock and instant lag elimination.
 */
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToLauncher: () -> Unit,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToShizuku: () -> Unit,
    onNavigateToOverlay: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val liveFps by viewModel.liveFps.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val isForce90FpsLocked by viewModel.isForce90FpsLocked.collectAsState()
    val isDndActive by viewModel.isDndActive.collectAsState()
    val isKillingLag by viewModel.isKillingLag.collectAsState()
    val lagKillResult by viewModel.lagKillResult.collectAsState()
    val gameProfiles by viewModel.gameProfiles.collectAsState()
    val capabilities by viewModel.capabilities.collectAsState()
    val stabilityMetrics by viewModel.stabilityMetrics.collectAsState()
    val selectedFpsMode by viewModel.selectedFpsMode.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.refreshSettingsState()
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

    val infiniteTransition = rememberInfiniteTransition(label = "pulse90fps")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // COMPACT HERO HEADER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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
                                colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.92f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(
                            status = if (deviceInfo.isXPadDetected) "INFINIX XPAD 20" else deviceInfo.deviceModel,
                            color = NeonGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            status = "${telemetry.displayRefreshRate.toInt()}Hz Display",
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "90 FPS Game Accelerator",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 🔥 90 FPS STABILITY ENGINE & MODE SELECTOR
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        if (selectedFpsMode != FpsMode.OFF) NeonGreen else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedFpsMode != FpsMode.OFF) DarkSurfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedFpsMode != FpsMode.OFF) NeonGreen.copy(alpha = 0.2f) else DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "90 FPS Stability Engine",
                                    tint = if (selectedFpsMode != FpsMode.OFF) NeonGreen else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "90 FPS STABILITY ENGINE",
                                    color = if (selectedFpsMode != FpsMode.OFF) NeonGreen else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = stabilityMetrics.status.label,
                                    color = when (stabilityMetrics.status) {
                                        StabilityStatus.STABLE, StabilityStatus.STABLE_60 -> NeonGreen
                                        StabilityStatus.FPS_DROP, StabilityStatus.THERMAL_WARNING -> WarningAmber
                                        StabilityStatus.FALLBACK_60HZ, StabilityStatus.SHIZUKU_DISCONNECTED -> AlertRed
                                        StabilityStatus.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Switch(
                            checked = selectedFpsMode != FpsMode.OFF,
                            onCheckedChange = { viewModel.toggleForce90FpsLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = NeonGreen
                            ),
                            modifier = Modifier.testTag("force_90fps_lock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 13. CAPABILITY-BASED UI INDICATOR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DEVICE 90Hz STATUS",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val (capLabel, capColor) = when {
                            capabilities.is90HzSupported && capabilities.controlMethod != RefreshControlMethod.MONITORING_ONLY ->
                                "[ 90 Hz ] ✓ Supported" to NeonGreen
                            capabilities.is90HzSupported ->
                                "[ 90 Hz ] ⚠ Monitoring only" to WarningAmber
                            else ->
                                "[ 90 Hz ] ✕ Not supported (Max: ${capabilities.maxSupportedRefreshRate.toInt()}Hz)" to AlertRed
                        }
                        Text(
                            text = capLabel,
                            color = capColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. 90 FPS MODE SELECTOR (OFF, 60 FPS / 60 Hz, 90 FPS / 90 Hz, AUTO / HIGHEST)
                    Text(
                        text = "SELECT STABILITY TARGET",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (mode in FpsMode.entries) {
                            val isSelected = selectedFpsMode == mode
                            val isSupported = mode != FpsMode.MODE_90HZ || capabilities.is90HzSupported

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSupported || mode == FpsMode.OFF) {
                                        viewModel.selectFpsMode(mode)
                                    }
                                },
                                enabled = isSupported || mode == FpsMode.OFF,
                                label = {
                                    Text(
                                        text = when (mode) {
                                            FpsMode.OFF -> "OFF"
                                            FpsMode.MODE_60HZ -> "60 Hz"
                                            FpsMode.MODE_90HZ -> if (capabilities.is90HzSupported) "90 Hz" else "90 Hz ✕"
                                            FpsMode.AUTO_HIGHEST -> "AUTO"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonGreen,
                                    containerColor = MaterialTheme.colorScheme.background,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fps_mode_chip_${mode.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hardware Telemetry & Frame Timing Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "HARDWARE RATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${stabilityMetrics.currentRefreshRate.toInt()} Hz",
                                color = if (stabilityMetrics.currentRefreshRate >= 85f) NeonGreen else CyberCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "FRAME TIMING", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("%.1f ms", stabilityMetrics.frameTimeMs),
                                color = CyberCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "THERMAL STATUS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${stabilityMetrics.temperatureC.toInt()}°C (${stabilityMetrics.thermalTier.name})",
                                color = when (stabilityMetrics.thermalTier) {
                                    com.example.telemetry.ThermalTier.NORMAL -> NeonGreen
                                    com.example.telemetry.ThermalTier.WARM -> WarningAmber
                                    else -> AlertRed
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Diagnostic status message
                    if (stabilityMetrics.statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stabilityMetrics.statusMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Diagnostic Link Button
                    Button(
                        onClick = onNavigateToDiagnostics,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_view_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Hardware & Refresh Rate Diagnostics",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ⚡ ONE-TAP MAX BOOST
        item {
            GamingCard(
                title = "One-Tap Ultra Boost",
                icon = Icons.Default.FlashOn,
                accentColor = AlertRed
            ) {
                Text(
                    text = "Instantly clears RAM, force-kills background lag processes, prevents CPU throttling, and locks peak 90Hz.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                NeonPrimaryButton(
                    text = if (isKillingLag) "Purging System Stutter..." else "⚡ ONE-TAP ULTRA BOOST NOW",
                    icon = Icons.Default.FlashOn,
                    accentColor = AlertRed,
                    enabled = !isKillingLag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_force_kill_lag_button"),
                    onClick = {
                        viewModel.forceKillLag()
                        if (!isForce90FpsLocked) {
                            viewModel.toggleForce90FpsLock(true)
                        }
                    }
                )

                AnimatedVisibility(
                    visible = lagKillResult != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    lagKillResult?.let { res ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ +${res.ramFreedMb}MB RAM Freed • 90Hz Guard Active",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "LAG PURGED",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // 🪟 QUICK GAMING CONTROLS (Floating FPS & DND)
        item {
            GamingCard(
                title = "Quick Gaming Toggles",
                icon = Icons.Default.Bolt,
                accentColor = CyberCyan
            ) {
                // Floating FPS HUD switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Floating FPS Counter",
                            tint = if (isOverlayActive) CyberCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Floating FPS Counter",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Displays live frame rate on top of other games",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isOverlayActive,
                        onCheckedChange = { viewModel.toggleOverlay(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("dashboard_quick_overlay_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gaming DND Shield switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoNotDisturbOn,
                            contentDescription = "Gaming DND Shield",
                            tint = if (isDndActive) AlertRed else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gaming DND Shield",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Blocks frame-dropping notification popups",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isDndActive,
                        onCheckedChange = { viewModel.toggleDnd(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = AlertRed
                        ),
                        modifier = Modifier.testTag("dashboard_quick_dnd_toggle")
                    )
                }
            }
        }

        // 📊 SIMPLE LIVE TELEMETRY
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LIVE SYSTEM TELEMETRY",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Render FPS",
                        value = "$liveFps",
                        subValue = "${telemetry.displayRefreshRate.toInt()}Hz",
                        icon = Icons.Default.Speed,
                        accentColor = NeonGreen
                    )
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "RAM Usage",
                        value = "${String.format("%.1f", usedRamMb / 1024f)}GB",
                        subValue = "${(ramProgress * 100).toInt()}% used",
                        icon = Icons.Default.Memory,
                        accentColor = CyberCyan,
                        progress = ramProgress
                    )
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Thermal Temp",
                        value = String.format("%.0f°C", telemetry.batteryTemperatureC),
                        subValue = "${telemetry.batteryPercent}%${if (telemetry.isCharging) " ⚡" else ""}",
                        icon = Icons.Default.BatteryChargingFull,
                        accentColor = if (telemetry.batteryTemperatureC > 40f) WarningAmber else StatusSuccess
                    )
                }
            }
        }

        // 🚀 QUICK GAME LAUNCHER
        item {
            GamingCard(
                title = "Launch Games at 90 FPS",
                icon = Icons.Default.SportsEsports,
                accentColor = NeonGreen
            ) {
                if (gameProfiles.isEmpty()) {
                    Text(
                        text = "Tap below to view installed games and launch them with 90 FPS lock:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gameProfiles.take(2).forEach { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable {
                                        if (!isForce90FpsLocked) {
                                            viewModel.toggleForce90FpsLock(true)
                                        }
                                        viewModel.launchGameWithProfile(profile)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = profile.gameName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "90 FPS Target • DND Ready",
                                        color = NeonGreen,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Launch ${profile.gameName}",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "PLAY",
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                NeonPrimaryButton(
                    text = "OPEN ALL GAMES",
                    icon = Icons.Default.SportsEsports,
                    accentColor = NeonGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToLauncher
                )
            }
        }
    }
}
