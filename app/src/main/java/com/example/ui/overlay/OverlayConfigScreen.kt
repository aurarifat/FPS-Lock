package com.example.ui.overlay

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.MainViewModel
import com.example.ui.components.GamingCard
import com.example.ui.components.HonestHardwareBanner
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LightBg
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun OverlayConfigScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()
    val hasOverlayPermission = permissionsState.hasOverlay
    val isDarkMode by viewModel.isDarkMode.collectAsState()

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

    var opacity by remember { mutableFloatStateOf(viewModel.prefs.overlayOpacity) }
    var showFps by remember { mutableStateOf(viewModel.prefs.overlayShowFps) }
    var showHz by remember { mutableStateOf(viewModel.prefs.overlayShowRefreshRate) }
    var showRam by remember { mutableStateOf(viewModel.prefs.overlayShowRam) }
    var showTemp by remember { mutableStateOf(viewModel.prefs.overlayShowTemp) }
    var showBattery by remember { mutableStateOf(viewModel.prefs.overlayShowBattery) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("overlay_config_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Floating Game Booster HUD",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Real-time telemetry overlay displayed on top of games",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Overlay Activation Card
        item {
            GamingCard(
                title = "Overlay Service Control",
                icon = Icons.Default.Layers,
                accentColor = NeonGreen
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOverlayActive) "HUD Overlay is ACTIVE" else "HUD Overlay is OFF",
                            color = if (isOverlayActive) NeonGreen else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasOverlayPermission) "Overlay permission granted" else "Special overlay permission required",
                            color = if (hasOverlayPermission) TextMuted else WarningAmber,
                            fontSize = 11.sp
                        )
                    }

                    if (hasOverlayPermission) {
                        Switch(
                            checked = isOverlayActive,
                            onCheckedChange = { active ->
                                viewModel.toggleOverlay(active)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = NeonGreen
                            )
                        )
                    } else {
                        NeonOutlinedButton(
                            text = "Grant Permission",
                            accentColor = WarningAmber,
                            onClick = { viewModel.openOverlaySettings() }
                        )
                    }
                }
            }
        }

        // Live Preview Box
        item {
            GamingCard(
                title = "HUD Bubble Preview (${if (isDarkMode) "Dark" else "Light"} Theme)",
                icon = Icons.Default.Visibility,
                accentColor = CyberCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Theme: ${if (isDarkMode) "Dark Stealth Mode" else "Light Solar Mode"}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDarkMode) "Dark" else "Light",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = NeonGreen
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkMode) ObsidianBg else LightBg)
                        .border(1.dp, if (isDarkMode) BorderDark else com.example.ui.theme.LightBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulated Pill
                    val previewBg = if (isDarkMode) DarkSurfaceVariant.copy(alpha = opacity) else com.example.ui.theme.LightSurface.copy(alpha = opacity)
                    val previewBorder = if (isDarkMode) NeonGreen.copy(alpha = opacity) else com.example.ui.theme.LightGreenPrimary.copy(alpha = opacity)
                    val previewFpsColor = if (isDarkMode) NeonGreen else com.example.ui.theme.LightGreenPrimary
                    val previewHzColor = if (isDarkMode) CyberCyan else com.example.ui.theme.LightCyanSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(previewBg)
                            .border(1.5.dp, previewBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showFps) {
                                Text(
                                    text = "60 FPS",
                                    color = previewFpsColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (showFps && showHz) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(12.dp)
                                        .background(if (isDarkMode) BorderDark else com.example.ui.theme.LightBorder)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (showHz) {
                                Text(
                                    text = "90Hz",
                                    color = previewHzColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (showTemp) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "32°C",
                                    color = WarningAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Appearance & Opacity
        item {
            GamingCard(
                title = "HUD Transparency & Size",
                accentColor = ElectricBlue
            ) {
                Text(
                    text = "Overlay Opacity: ${(opacity * 100).toInt()}%",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        viewModel.prefs.overlayOpacity = it
                    },
                    valueRange = 0.3f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Size preset: Normal (optimized for 11\" tablet and mobile displays)",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Metric Toggles
        item {
            GamingCard(
                title = "Displayed Telemetry Metrics",
                accentColor = CyberCyan
            ) {
                val metrics = listOf(
                    Triple("Live Game Rendered FPS", showFps) { checked: Boolean ->
                        showFps = checked
                        viewModel.prefs.overlayShowFps = checked
                    },
                    Triple("Display Refresh Rate (Hardware Hz)", showHz) { checked: Boolean ->
                        showHz = checked
                        viewModel.prefs.overlayShowRefreshRate = checked
                    },
                    Triple("RAM Usage & Working Set", showRam) { checked: Boolean ->
                        showRam = checked
                        viewModel.prefs.overlayShowRam = checked
                    },
                    Triple("Battery Temperature (°C)", showTemp) { checked: Boolean ->
                        showTemp = checked
                        viewModel.prefs.overlayShowTemp = checked
                    },
                    Triple("Battery Percentage & Charging State", showBattery) { checked: Boolean ->
                        showBattery = checked
                        viewModel.prefs.overlayShowBattery = checked
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    metrics.forEach { (label, isChecked, onToggle) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, color = TextPrimary, fontSize = 13.sp)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = onToggle,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeonGreen,
                                    checkmarkColor = ObsidianBg
                                )
                            )
                        }
                    }
                }
            }
        }

        // Truth Guarantee
        item {
            HonestHardwareBanner(
                title = "Overlay Distinction Guarantee",
                description = "Game FPS (frames produced by the game engine) and Hardware Refresh Rate (how often the display panel redraws, 90Hz on XPad 20) are strictly presented as separate metrics.",
                icon = Icons.Default.Info,
                color = CyberCyan
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
