package com.example.ui.launcher

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameProfile
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
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLauncherScreen(
    viewModel: MainViewModel
) {
    val gameProfiles by viewModel.gameProfiles.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val scope = rememberCoroutineScope()

    var editingProfile by remember { mutableStateOf<GameProfile?>(null) }
    var showAddGameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("game_launcher_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gaming Hub",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Individual game tuning profiles & quick launcher",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row {
                    NeonOutlinedButton(
                        text = "+ Add Game",
                        icon = Icons.Default.Add,
                        accentColor = NeonGreen,
                        onClick = { showAddGameDialog = true }
                    )
                }
            }
        }

        // Built-in Game Space Shortcut
        item {
            GamingCard(
                title = "Device Game Mode Shortcut",
                icon = Icons.Default.Gamepad,
                accentColor = CyberCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Infinix / Transsion Game Space",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Launch device hardware Game Zone or System Settings",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    NeonOutlinedButton(
                        text = "Open Game Space",
                        accentColor = CyberCyan,
                        onClick = { viewModel.openInfinixGameSpace() }
                    )
                }
            }
        }

        // Game Profiles List
        item {
            Text(
                text = "SAVED GAME PROFILES (${gameProfiles.size})",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (gameProfiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Games Configured",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap '+ Add Game' above to select installed games like Free Fire MAX",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(gameProfiles, key = { it.packageName }) { profile ->
                GameProfileCard(
                    profile = profile,
                    onLaunch = { viewModel.launchGameWithProfile(profile) },
                    onEdit = { editingProfile = profile },
                    onToggleFavorite = {
                        scope.launch {
                            viewModel.gameDao.updateFavorite(profile.packageName, !profile.isFavorite)
                        }
                    },
                    onDelete = {
                        scope.launch {
                            viewModel.gameDao.deleteProfile(profile)
                        }
                    }
                )
            }
        }

        // Educational notice
        item {
            HonestHardwareBanner(
                title = "Automatic Profile Mechanism",
                description = "When you launch a game from GameBoost, the foreground service monitors your session, activates custom DND & brightness, and restores your previous system settings when you exit.",
                icon = Icons.Default.Tune,
                color = CyberCyan
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Edit Profile Dialog
    if (editingProfile != null) {
        val p = editingProfile!!
        var targetBrightness by remember { mutableIntStateOf(if (p.targetBrightness >= 0) p.targetBrightness else 80) }
        var alterBrightness by remember { mutableStateOf(p.targetBrightness >= 0) }
        var enableDnd by remember { mutableStateOf(p.enableDnd) }
        var refreshRate by remember { mutableFloatStateOf(if (p.targetRefreshRate > 0) p.targetRefreshRate else 90f) }
        var lockRefreshRate by remember { mutableStateOf(p.targetRefreshRate > 0) }
        var launchOverlay by remember { mutableStateOf(p.launchOverlay) }
        var animScale by remember { mutableFloatStateOf(if (p.targetAnimationScale >= 0) p.targetAnimationScale else 0.5f) }

        AlertDialog(
            onDismissRequest = { editingProfile = null },
            title = {
                Text(
                    text = "Edit Profile: ${p.gameName}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Refresh Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Target Refresh Rate", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Locks display to 90Hz (or 60Hz)", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = lockRefreshRate,
                            onCheckedChange = { lockRefreshRate = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = NeonGreen)
                        )
                    }

                    if (lockRefreshRate) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NeonOutlinedButton(
                                text = "60 Hz",
                                accentColor = if (refreshRate == 60f) NeonGreen else TextMuted,
                                onClick = { refreshRate = 60f }
                            )
                            NeonOutlinedButton(
                                text = "90 Hz (XPad)",
                                accentColor = if (refreshRate == 90f) NeonGreen else TextMuted,
                                onClick = { refreshRate = 90f }
                            )
                        }
                    }

                    // Brightness
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Fixed Brightness ($targetBrightness%)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Overrides brightness on game start", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = alterBrightness,
                            onCheckedChange = { alterBrightness = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = NeonGreen)
                        )
                    }
                    if (alterBrightness) {
                        Slider(
                            value = targetBrightness.toFloat(),
                            onValueChange = { targetBrightness = it.toInt() },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                        )
                    }

                    // DND
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Do Not Disturb", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = enableDnd,
                            onCheckedChange = { enableDnd = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = NeonGreen)
                        )
                    }

                    // Floating HUD Overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Launch Floating HUD", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = launchOverlay,
                            onCheckedChange = { launchOverlay = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = NeonGreen)
                        )
                    }
                }
            },
            confirmButton = {
                NeonPrimaryButton(
                    text = "Save Profile",
                    onClick = {
                        val updated = p.copy(
                            targetBrightness = if (alterBrightness) targetBrightness else -1,
                            enableDnd = enableDnd,
                            targetRefreshRate = if (lockRefreshRate) refreshRate else -1f,
                            targetAnimationScale = animScale,
                            launchOverlay = launchOverlay
                        )
                        scope.launch {
                            viewModel.gameDao.updateProfile(updated)
                            editingProfile = null
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { editingProfile = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Add Game from Installed Apps Dialog
    if (showAddGameDialog) {
        AlertDialog(
            onDismissRequest = { showAddGameDialog = false },
            title = {
                Text("Select App to Add as Game", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    val availableApps = installedApps.filter { app ->
                        gameProfiles.none { it.packageName == app.packageName }
                    }
                    if (availableApps.isEmpty()) {
                        item {
                            Text("All detected games and apps have already been added!", color = TextMuted, fontSize = 12.sp)
                        }
                    } else {
                        items(availableApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            viewModel.gameDao.insertOrUpdateProfile(
                                                GameProfile(
                                                    packageName = app.packageName,
                                                    gameName = app.appName,
                                                    isFavorite = false,
                                                    targetRefreshRate = 90f,
                                                    targetAnimationScale = 0.5f,
                                                    enableDnd = true
                                                )
                                            )
                                            showAddGameDialog = false
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(app.packageName, color = TextMuted, fontSize = 10.sp)
                                }
                                if (app.isGame) {
                                    StatusBadge(status = "GAME", color = NeonGreen)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddGameDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun GameProfileCard(
    profile: GameProfile,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (profile.isFavorite) NeonGreen.copy(alpha = 0.6f) else BorderDark, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = if (profile.isFavorite) NeonGreen else CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = profile.gameName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile.packageName,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (profile.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (profile.isFavorite) WarningAmber else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profile active features pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (profile.targetRefreshRate > 0) {
                    StatusBadge(status = "${profile.targetRefreshRate.toInt()}Hz", color = CyberCyan)
                }
                if (profile.enableDnd) {
                    StatusBadge(status = "DND Active", color = WarningAmber)
                }
                if (profile.targetBrightness in 0..100) {
                    StatusBadge(status = "${profile.targetBrightness}% Brightness", color = ElectricBlue)
                }
                if (profile.launchOverlay) {
                    StatusBadge(status = "Floating HUD", color = NeonGreen)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Edit Profile", tint = TextSecondary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed.copy(alpha = 0.7f))
                    }
                }

                NeonPrimaryButton(
                    text = "Launch Game",
                    icon = Icons.Default.PlayArrow,
                    onClick = onLaunch
                )
            }
        }
    }
}
