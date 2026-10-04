package com.example.ui.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GamingCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber

@Composable
fun DiagnosticsScreen(viewModel: MainViewModel) {
    val caps by viewModel.capabilities.collectAsState()
    val testReport by viewModel.capabilityReport.collectAsState()
    val stabilityMetrics by viewModel.stabilityMetrics.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()
    val isRunningTest by viewModel.isRunningCapabilityTest.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("diagnostics_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Device Diagnostics",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Hardware Refresh Rate & Shizuku Audit",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.runCapabilityDiagnosticTest() },
                            enabled = !isRunningTest,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("run_capability_test_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Run Test",
                                tint = ObsidianBg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunningTest) "Testing..." else "Run Test",
                                color = ObsidianBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Test Report Card (if executed)
        testReport?.let { report ->
            item {
                GamingCard(
                    title = "Test Results: ${report.overallStatus}",
                    icon = Icons.Default.Speed,
                    accentColor = if (report.overallStatus.contains("FULLY")) NeonGreen else CyberCyan
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        report.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                val icon = when {
                                    item.isSupported -> Icons.Default.CheckCircle
                                    item.isWarning -> Icons.Default.Warning
                                    else -> Icons.Default.Error
                                }
                                val iconColor = when {
                                    item.isSupported -> NeonGreen
                                    item.isWarning -> WarningAmber
                                    else -> AlertRed
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.details,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hardware & Display Spec Grid
        item {
            GamingCard(
                title = "Display & Hardware Capabilities",
                icon = Icons.Default.DisplaySettings,
                accentColor = NeonGreen
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiagnosticRow(
                        label = "Device",
                        value = "${caps.manufacturer} ${caps.model}"
                    )
                    DiagnosticRow(
                        label = "Android Version",
                        value = caps.androidVersion
                    )
                    DiagnosticRow(
                        label = "Current Refresh Rate",
                        value = "${caps.currentRefreshRate.toInt()} Hz",
                        valueColor = if (caps.currentRefreshRate >= 85f) NeonGreen else CyberCyan
                    )
                    DiagnosticRow(
                        label = "Maximum Supported Rate",
                        value = "${caps.maxSupportedRefreshRate.toInt()} Hz",
                        valueColor = NeonGreen
                    )
                    DiagnosticRow(
                        label = "90 Hz Native Availability",
                        value = if (caps.is90HzSupported) "✓ Supported" else "✕ Not Supported",
                        valueColor = if (caps.is90HzSupported) NeonGreen else AlertRed
                    )
                    DiagnosticRow(
                        label = "Control Method",
                        value = caps.controlMethod.label,
                        valueColor = when (caps.controlMethod) {
                            com.example.telemetry.RefreshControlMethod.SHIZUKU_PRIVILEGED -> NeonGreen
                            com.example.telemetry.RefreshControlMethod.SYSTEM_WRITE_SETTINGS -> CyberCyan
                            com.example.telemetry.RefreshControlMethod.MONITORING_ONLY -> WarningAmber
                        }
                    )
                }
            }
        }

        // Supported Display Modes List
        item {
            GamingCard(
                title = "Supported Display Modes (${caps.supportedModes.size})",
                icon = Icons.Default.DisplaySettings,
                accentColor = CyberCyan
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    caps.supportedModes.forEach { mode ->
                        val isCurrent = caps.currentMode?.modeId == mode.modeId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) CyberCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background)
                                .border(
                                    1.dp,
                                    if (isCurrent) CyberCyan else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mode #${mode.modeId}: ${mode.width}x${mode.height}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isCurrent) "ACTIVE MODE" else "Supported",
                                    color = if (isCurrent) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                            StatusBadge(
                                status = "${mode.refreshRate.toInt()} Hz",
                                color = if (mode.refreshRate >= 85f) NeonGreen else CyberCyan
                            )
                        }
                    }
                }
            }
        }

        // Shizuku Connection Status
        item {
            GamingCard(
                title = "Shizuku Privileged Status",
                icon = Icons.Default.Terminal,
                accentColor = if (permissionsState.isShizukuAuthorized) NeonGreen else WarningAmber
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiagnosticRow(
                        label = "Status",
                        value = permissionsState.shizukuStatus.label,
                        valueColor = if (permissionsState.isShizukuAuthorized) NeonGreen else WarningAmber
                    )
                    DiagnosticRow(
                        label = "Permission Granted",
                        value = if (permissionsState.isShizukuAuthorized) "YES" else "NO",
                        valueColor = if (permissionsState.isShizukuAuthorized) NeonGreen else AlertRed
                    )
                    DiagnosticRow(
                        label = "High Refresh Shell Lock",
                        value = if (permissionsState.isShizukuAuthorized) "AVAILABLE" else "DISABLED",
                        valueColor = if (permissionsState.isShizukuAuthorized) NeonGreen else TextMuted
                    )
                }
            }
        }

        // Honest FPS Measurement Notice
        item {
            GamingCard(
                title = "FPS & Telemetry Transparency",
                icon = Icons.Default.Info,
                accentColor = WarningAmber
            ) {
                Text(
                    text = "🔒 Android OS Security Architecture:\nNon-root applications cannot hook into external third-party game render loops to extract raw swapchain framebuffers without root or custom ROMs.\n\n✓ GameBoost measures real hardware display refresh rate, overlay VSYNC frame cadence (Choreographer), and system thermal sensors with 100% honesty — no fake FPS generators.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
