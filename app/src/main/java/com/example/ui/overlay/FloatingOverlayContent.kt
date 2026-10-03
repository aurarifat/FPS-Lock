package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TelemetryData
import com.example.telemetry.FpsMonitor
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LightBg
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightCyanSecondary
import com.example.ui.theme.LightGreenPrimary
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.HapticHelper

/**
 * Floating Game Booster Bubble & Expanded In-Game Gaming Toolbar.
 * Features:
 * - Compact Bubble: Live FPS, Temperature, RAM, pulse indicator.
 * - Expanded Toolbar: Performance Mode (Eco/Balanced/Beast 90FPS),
 *   Clear Memory, Screenshot, Start/Stop Monitoring, Thermal Protection Guard.
 */
@Composable
fun FloatingOverlayContent(
    fps: Int,
    fpsHistory: List<Int>,
    stability: FpsMonitor.StabilityReport,
    telemetry: TelemetryData,
    isExpanded: Boolean,
    isDarkMode: Boolean = true,
    opacity: Float,
    performanceMode: String = "BEAST_90FPS",
    isMonitoring: Boolean = true,
    lagKillMessage: String? = null,
    screenshotToastMessage: String? = null,
    onToggleExpanded: () -> Unit,
    onClose: () -> Unit,
    onForceKillLag: () -> Unit,
    onChangeOpacity: (Float) -> Unit,
    onSelectPerformanceMode: (String) -> Unit = {},
    onTakeScreenshot: () -> Unit = {},
    onToggleMonitoring: () -> Unit = {},
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "fpsPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Dynamic FPS color grading based on performance and theme
    val fpsColor = when {
        fps >= 58 -> if (isDarkMode) NeonGreen else LightGreenPrimary
        fps >= 45 -> WarningAmber
        else -> AlertRed
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
    ) {
        if (!isExpanded) {
            // COMPACT FLOATING GAME BOOSTER BUBBLE (FPS + TEMP + RAM)
            CompactFpsBubble(
                fps = fps,
                fpsColor = fpsColor,
                temperatureC = telemetry.batteryTemperatureC,
                ramUsedBytes = telemetry.ramUsedBytes,
                ramTotalBytes = telemetry.ramTotalBytes,
                isDarkMode = isDarkMode,
                opacity = opacity,
                pulseAlpha = pulseAlpha,
                onToggleExpanded = {
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.MEDIUM)
                    onToggleExpanded()
                },
                onDragDelta = onDragDelta
            )
        } else {
            // EXPANDED IN-GAME GAMING TOOLBAR / HUD
            ExpandedFpsHud(
                fps = fps,
                fpsColor = fpsColor,
                fpsHistory = fpsHistory,
                stability = stability,
                telemetry = telemetry,
                isDarkMode = isDarkMode,
                opacity = opacity,
                pulseAlpha = pulseAlpha,
                performanceMode = performanceMode,
                isMonitoring = isMonitoring,
                lagKillMessage = lagKillMessage,
                screenshotToastMessage = screenshotToastMessage,
                onToggleExpanded = {
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.MEDIUM)
                    onToggleExpanded()
                },
                onClose = {
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.MEDIUM)
                    onClose()
                },
                onForceKillLag = {
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.SUCCESS)
                    onForceKillLag()
                },
                onTakeScreenshot = {
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.SUCCESS)
                    onTakeScreenshot()
                },
                onToggleMonitoring = {
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.LIGHT)
                    onToggleMonitoring()
                },
                onSelectPerformanceMode = { mode ->
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.MEDIUM)
                    onSelectPerformanceMode(mode)
                },
                onChangeOpacity = { op ->
                    HapticHelper.performHaptic(context, HapticHelper.HapticType.LIGHT)
                    onChangeOpacity(op)
                },
                onDragDelta = onDragDelta
            )
        }
    }
}

/**
 * Compact Floating Game Booster Bubble showing FPS + Temperature + RAM
 */
@Composable
private fun CompactFpsBubble(
    fps: Int,
    fpsColor: Color,
    temperatureC: Float,
    ramUsedBytes: Long,
    ramTotalBytes: Long,
    isDarkMode: Boolean,
    opacity: Float,
    pulseAlpha: Float,
    onToggleExpanded: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = if (isDarkMode) ObsidianBg else LightSurface
    val borderColor = if (isDarkMode) fpsColor.copy(alpha = 0.85f) else fpsColor.copy(alpha = 0.95f)
    val textColor = if (isDarkMode) TextPrimary else LightTextPrimary
    val iconTint = if (isDarkMode) TextSecondary else LightTextSecondary
    val badgeBg = if (isDarkMode) DarkSurfaceVariant else LightSurfaceVariant

    val ramPercent = if (ramTotalBytes > 0) ((ramUsedBytes.toFloat() / ramTotalBytes.toFloat()) * 100).toInt() else 50
    val tempColor = when {
        temperatureC >= 42.0 -> AlertRed
        temperatureC >= 38.0 -> WarningAmber
        else -> if (isDarkMode) NeonGreen else LightGreenPrimary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor.copy(alpha = opacity.coerceIn(0.45f, 1.0f)))
            .border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleExpanded
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Draggable grip icon
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag overlay",
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Real-time pulse dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fpsColor.copy(alpha = pulseAlpha))
            )

            // FPS Readout
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$fps",
                    color = fpsColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "FPS",
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
                )
            }

            // Temperature Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${temperatureC.toInt()}°C",
                    color = tempColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // RAM % Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$ramPercent% RAM",
                    color = if (isDarkMode) CyberCyan else LightCyanSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Expand icon
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Expand HUD",
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Expanded In-Game Gaming Toolbar / HUD
 */
@Composable
private fun ExpandedFpsHud(
    fps: Int,
    fpsColor: Color,
    fpsHistory: List<Int>,
    stability: FpsMonitor.StabilityReport,
    telemetry: TelemetryData,
    isDarkMode: Boolean,
    opacity: Float,
    pulseAlpha: Float,
    performanceMode: String,
    isMonitoring: Boolean,
    lagKillMessage: String?,
    screenshotToastMessage: String?,
    onToggleExpanded: () -> Unit,
    onClose: () -> Unit,
    onForceKillLag: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onToggleMonitoring: () -> Unit,
    onSelectPerformanceMode: (String) -> Unit,
    onChangeOpacity: (Float) -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    val usedMb = telemetry.ramUsedBytes / (1024 * 1024)
    val totalMb = telemetry.ramTotalBytes / (1024 * 1024)
    val ramFraction = if (totalMb > 0) usedMb.toFloat() / totalMb.toFloat() else 0f

    val hudBg = if (isDarkMode) DarkSurface else LightSurface
    val hudBorder = if (isDarkMode) BorderDark else LightBorder
    val headerBg = if (isDarkMode) DarkSurfaceVariant else LightSurfaceVariant
    val headerText = if (isDarkMode) TextPrimary else LightTextPrimary
    val iconTint = if (isDarkMode) TextSecondary else LightTextSecondary
    val subText = if (isDarkMode) TextSecondary else LightTextSecondary
    val mutedText = if (isDarkMode) TextMuted else LightTextMuted
    val primaryText = if (isDarkMode) TextPrimary else LightTextPrimary
    val hzColor = if (isDarkMode) CyberCyan else LightCyanSecondary

    val isThermalHot = telemetry.batteryTemperatureC >= 42.0

    Box(
        modifier = Modifier
            .width(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(hudBg.copy(alpha = opacity.coerceIn(0.5f, 1.0f)))
            .border(1.5.dp, hudBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // DRAGGABLE HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(headerBg)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag handle",
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(fpsColor.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "GAMEBOOSTER HUD",
                        color = headerText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Minimize HUD",
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close overlay",
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // MAIN FPS DIGITAL DISPLAY + REFRESH RATE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$fps",
                        color = fpsColor,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 40.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = "FPS",
                            color = primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${telemetry.displayRefreshRate.toInt()}Hz Display",
                            color = hzColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (stability.isStable) fpsColor.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (stability.isStable) "STABLE" else "DROPS: ${stability.droppedFrames}",
                            color = if (stability.isStable) fpsColor else AlertRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stability.message,
                        color = subText,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }

            // REAL-TIME FRAME RATE TRAJECTORY GRAPH
            RealTimeFpsGraph(
                history = fpsHistory,
                fpsColor = fpsColor,
                targetHz = telemetry.displayRefreshRate,
                isDarkMode = isDarkMode
            )

            // HARDWARE TELEMETRY STRIP (RAM, THERMAL, BATTERY)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(headerBg)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "RAM", color = mutedText, fontSize = 9.sp)
                    Text(
                        text = "${usedMb}MB (${(ramFraction * 100).toInt()}%)",
                        color = primaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(text = "THERMAL", color = mutedText, fontSize = 9.sp)
                    Text(
                        text = String.format("%.1f°C", telemetry.batteryTemperatureC),
                        color = if (isThermalHot) AlertRed else WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(text = "BATTERY", color = mutedText, fontSize = 9.sp)
                    Text(
                        text = "${telemetry.batteryPercent}%${if (telemetry.isCharging) " ⚡" else ""}",
                        color = if (isDarkMode) NeonGreen else LightGreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // RAM USAGE PROGRESS BAR
            LinearProgressIndicator(
                progress = { ramFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (ramFraction > 0.85f) AlertRed else hzColor,
                trackColor = hudBorder
            )

            // PERFORMANCE MODE SELECTOR CHIPS
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PERFORMANCE MODE",
                    color = mutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ECO" to "Eco (60Hz)", "BALANCED" to "Balanced", "BEAST_90FPS" to "🔥 Beast 90FPS").forEach { (modeKey, modeTitle) ->
                        val isSelected = performanceMode.equals(modeKey, ignoreCase = true)
                        val chipBg = if (isSelected) (if (modeKey == "BEAST_90FPS") NeonGreen else hzColor) else headerBg
                        val chipTextColor = if (isSelected) ObsidianBg else subText
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(chipBg)
                                .clickable { onSelectPerformanceMode(modeKey) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeTitle,
                                color = chipTextColor,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // GAMING QUICK ACTION GRID (CLEAR MEMORY, SCREENSHOT, MONITORING, THERMAL GUARD)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Clear Memory Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AlertRed)
                        .clickable(onClick = onForceKillLag)
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear RAM", tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(text = "CLEAR RAM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                // 2. Screenshot Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(headerBg)
                        .clickable(onClick = onTakeScreenshot)
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Screenshot", tint = hzColor, modifier = Modifier.size(13.dp))
                        Text(text = "SCREENSHOT", color = primaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 3. Start/Stop Monitoring Toggle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(headerBg)
                        .clickable(onClick = onToggleMonitoring)
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Monitor status",
                            tint = if (isMonitoring) NeonGreen else WarningAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isMonitoring) "FPS: LIVE" else "FPS: PAUSED",
                            color = primaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4. Thermal Protection Indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isThermalHot) AlertRed.copy(alpha = 0.2f) else headerBg)
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = "Thermal Guard",
                            tint = if (isThermalHot) AlertRed else NeonGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isThermalHot) "HOT: GUARD ON" else "THERMAL: SAFE",
                            color = if (isThermalHot) AlertRed else NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // TEMPORARY TOAST BANNER FOR FEEDBACK (CLEAR RAM / SCREENSHOT)
            AnimatedVisibility(
                visible = lagKillMessage != null || screenshotToastMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val bannerText = screenshotToastMessage ?: lagKillMessage ?: ""
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bannerText,
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // OPACITY CHIPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HUD Transparency",
                    color = mutedText,
                    fontSize = 10.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.50f, 0.75f, 1.0f).forEach { op ->
                        val isSelected = kotlin.math.abs(opacity - op) < 0.05f
                        val chipBg = if (isSelected) hzColor else headerBg
                        val chipText = if (isSelected) (if (isDarkMode) ObsidianBg else Color.White) else subText
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(chipBg)
                                .clickable { onChangeOpacity(op) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${(op * 100).toInt()}%",
                                color = chipText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Real-time Canvas Graph for smooth visual frame cadence monitoring.
 */
@Composable
private fun RealTimeFpsGraph(
    history: List<Int>,
    fpsColor: Color,
    targetHz: Float,
    isDarkMode: Boolean
) {
    val graphBg = if (isDarkMode) DarkSurfaceVariant else LightSurfaceVariant
    val gridColor = if (isDarkMode) BorderDark else LightBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(graphBg)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            val width = size.width
            val height = size.height
            if (history.isEmpty() || width <= 0 || height <= 0) return@Canvas

            val maxScale = if (targetHz > 75) 100f else 75f
            val minScale = 20f

            // Reference baseline (60 FPS or 90 FPS)
            val refFps = if (targetHz > 75) 90f else 60f
            val refY = height - ((refFps - minScale) / (maxScale - minScale) * height).coerceIn(0f, height)

            drawLine(
                color = gridColor,
                start = Offset(0f, refY),
                end = Offset(width, refY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            val stepX = width / (history.size - 1).coerceAtLeast(1)
            val path = Path()
            val fillPath = Path()

            history.forEachIndexed { index, fpsVal ->
                val x = index * stepX
                val normalizedY = ((fpsVal.toFloat() - minScale) / (maxScale - minScale)).coerceIn(0f, 1f)
                val y = height - (normalizedY * height)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == history.lastIndex) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            // Fill gradient under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fpsColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = fpsColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
