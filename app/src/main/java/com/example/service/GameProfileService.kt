package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.GameBoostApplication
import com.example.MainActivity
import com.example.R
import com.example.data.local.GameBoostDatabase
import com.example.data.model.GameProfile
import com.example.data.preferences.PreferenceManager
import com.example.telemetry.CapabilityScanner
import com.example.telemetry.FpsMode
import com.example.telemetry.StabilityEngine
import com.example.telemetry.SystemMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Automates the 90 FPS Stability Engine & Game-Specific Profile Lifecycle:
 * 1. Detect game foreground entry.
 * 2. Load game profile from Room.
 * 3. Verify hardware capability & apply legitimate refresh-rate configuration.
 * 4. Start 90 FPS Stability Engine & telemetry monitoring.
 * 5. Display floating overlay HUD.
 * 6. Thermal-aware protection watchdog.
 * 7. When game exits: safely restore previous display state & booster settings.
 */
class GameProfileService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var shizukuManager: ShizukuManager
    private lateinit var systemMonitor: SystemMonitor
    private lateinit var capabilityScanner: CapabilityScanner
    private lateinit var stabilityEngine: StabilityEngine
    private lateinit var database: GameBoostDatabase

    private var activeGamePackage: String? = null
    private var activeGameName: String = "Game"
    private var activeProfile: GameProfile? = null

    private var lifecycleJob: Job? = null
    private var thermalJob: Job? = null
    private var exitDetectionJob: Job? = null

    // Saved display settings for clean auto-restore
    private var savedMinRefreshRate: Float = 60f
    private var savedPeakRefreshRate: Float = 90f
    private var hasSavedDisplaySettings = false

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        shizukuManager = ShizukuManager(this)
        systemMonitor = SystemMonitor(this)
        capabilityScanner = CapabilityScanner(this)
        stabilityEngine = StabilityEngine(this, shizukuManager, preferenceManager)
        database = GameBoostDatabase.getDatabase(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PROFILE) {
            restoreSettingsAndStop()
            return START_NOT_STICKY
        }

        activeGamePackage = intent?.getStringExtra(EXTRA_GAME_PACKAGE)
        activeGameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: "Game"

        startForeground(NOTIFICATION_ID, createNotification("Accelerating $activeGameName..."))
        executeGameProfileFlow()

        return START_STICKY
    }

    private fun executeGameProfileFlow() {
        lifecycleJob?.cancel()
        lifecycleJob = serviceScope.launch {
            val pkg = activeGamePackage ?: ""
            // STEP 1: Load game profile from Room DB
            val profile = database.gameProfileDao().getProfile(pkg) ?: GameProfile(
                packageName = pkg,
                gameName = activeGameName,
                targetRefreshRate = 90f,
                fpsTarget = 90,
                stabilityEngine = true,
                thermalProtection = true,
                autoRestore = true
            )
            activeProfile = profile

            // STEP 2: Save original display state before altering
            saveOriginalDisplayState()

            // STEP 3: Detect capability & Apply legitimate refresh rate
            val caps = capabilityScanner.scanCapabilities(shizukuManager)
            val requestedHz = profile.targetRefreshRate
            val targetHz = if (requestedHz >= 85f && caps.is90HzSupported) {
                90f
            } else if (requestedHz in 55f..65f) {
                60f
            } else {
                caps.highestSupportedRate
            }

            // Apply via Stability Engine
            val fpsMode = if (targetHz >= 85f) FpsMode.MODE_90HZ else FpsMode.MODE_60HZ
            if (profile.stabilityEngine) {
                stabilityEngine.setFpsMode(fpsMode, capabilityScanner)
            } else {
                applyDirectRefreshRate(targetHz)
            }

            // STEP 4: Memory trim
            systemMonitor.trimBackgroundMemory()

            // STEP 5: Launch Floating Overlay if enabled
            if (profile.launchOverlay && Settings.canDrawOverlays(applicationContext)) {
                val overlayIntent = Intent(applicationContext, FloatingBoosterService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(overlayIntent)
                } else {
                    startService(overlayIntent)
                }
            }

            // Update Notification
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, createNotification("🎮 $activeGameName: ${targetHz.toInt()}Hz Stability Engine Active"))

            // STEP 6: Thermal Watchdog
            if (profile.thermalProtection) {
                startThermalProtectionWatcher()
            }

            // STEP 7: Watchdog for game exit to auto-restore
            if (profile.autoRestore) {
                startExitDetectionWatcher()
            }
        }
    }

    private fun saveOriginalDisplayState() {
        if (hasSavedDisplaySettings) return
        try {
            savedMinRefreshRate = Settings.System.getFloat(contentResolver, "min_refresh_rate", 60.0f)
            savedPeakRefreshRate = Settings.System.getFloat(contentResolver, "peak_refresh_rate", 90.0f)
        } catch (ignored: Exception) {
            savedMinRefreshRate = 60.0f
            savedPeakRefreshRate = 90.0f
        }
        hasSavedDisplaySettings = true
    }

    private fun applyDirectRefreshRate(rate: Float) {
        val targetHz = rate.toInt()
        if (Settings.System.canWrite(this)) {
            try {
                Settings.System.putFloat(contentResolver, "peak_refresh_rate", rate)
                Settings.System.putFloat(contentResolver, "min_refresh_rate", rate)
                Settings.System.putInt(contentResolver, "user_refresh_rate", targetHz)
            } catch (ignored: Exception) {
            }
        }
        try {
            Settings.Global.putFloat(contentResolver, "peak_refresh_rate", rate)
            Settings.Global.putFloat(contentResolver, "min_refresh_rate", rate)
        } catch (ignored: Exception) {
        }
        if (shizukuManager.isAuthorized()) {
            serviceScope.launch {
                shizukuManager.forceGlobalHighRefreshRate(targetHz)
            }
        }
    }

    private fun startThermalProtectionWatcher() {
        thermalJob?.cancel()
        thermalJob = serviceScope.launch {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            while (isActive) {
                val temp = getBatteryTemperature()
                val isThrottling = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
                    powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE
                } else {
                    temp >= 42.0f
                }

                if (isThrottling) {
                    nm.notify(
                        NOTIFICATION_ID,
                        createNotification("⚠️ Thermal Safeguard: Device warm (${temp.toInt()}°C). Stabilizing in safe mode.")
                    )
                }
                delay(4000)
            }
        }
    }

    private fun startExitDetectionWatcher() {
        exitDetectionJob?.cancel()
        exitDetectionJob = serviceScope.launch {
            val targetPkg = activeGamePackage ?: return@launch
            delay(8000) // Initial game load grace period

            while (isActive) {
                delay(3000)
                if (isAppInForeground(targetPkg) == false) {
                    // Game exited or switched away! Auto-restore settings
                    restoreSettingsAndStop()
                    break
                }
            }
        }
    }

    private fun isAppInForeground(packageName: String): Boolean? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val time = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
            if (stats.isNullOrEmpty()) return null
            val recent = stats.maxByOrNull { it.lastTimeUsed }
            recent?.packageName == packageName
        } catch (e: Exception) {
            null
        }
    }

    private fun getBatteryTemperature(): Float {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val status = registerReceiver(null, filter)
        val tempRaw = status?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        return tempRaw / 10.0f
    }

    private fun createNotification(subtitle: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent = Intent(this, GameProfileService::class.java).apply {
            action = ACTION_STOP_PROFILE
        }
        val exitPending = PendingIntent.getService(
            this,
            2,
            exitIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GameBoostApplication.CHANNEL_ID)
            .setContentTitle("🚀 GameBoost: $activeGameName")
            .setContentText(subtitle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Exit & Restore", exitPending)
            .setOngoing(true)
            .build()
    }

    private fun restoreSettingsAndStop() {
        serviceScope.launch {
            // Restore previous display state
            if (hasSavedDisplaySettings && Settings.System.canWrite(applicationContext)) {
                try {
                    Settings.System.putFloat(contentResolver, "min_refresh_rate", savedMinRefreshRate)
                    Settings.System.putFloat(contentResolver, "peak_refresh_rate", savedPeakRefreshRate)
                } catch (ignored: Exception) {
                }
            }

            if (shizukuManager.isAuthorized()) {
                shizukuManager.resetGlobalRefreshRate()
            }

            stabilityEngine.restorePreviousState()

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleJob?.cancel()
        thermalJob?.cancel()
        exitDetectionJob?.cancel()
        serviceScope.cancel()
        shizukuManager.destroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 902
        const val ACTION_STOP_PROFILE = "com.example.action.STOP_PROFILE"
        const val EXTRA_GAME_PACKAGE = "extra_game_pkg"
        const val EXTRA_GAME_NAME = "extra_game_name"

        fun launchGameWithFlow(context: Context, gamePackage: String, gameName: String) {
            val intent = Intent(context, GameProfileService::class.java).apply {
                putExtra(EXTRA_GAME_PACKAGE, gamePackage)
                putExtra(EXTRA_GAME_NAME, gameName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(gamePackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
            }
        }
    }
}
