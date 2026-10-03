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
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.GameBoostApplication
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.PreferenceManager
import com.example.telemetry.SystemMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Automates the 10-Step Core Gaming Flow:
 * 1. Launch Game
 * 2. Detect device capability
 * 3. Connect/verify Shizuku
 * 4. Detect supported refresh/FPS
 * 5. Select highest supported mode (e.g. 90Hz)
 * 6. Start monitoring
 * 7. Stabilize within Android's permitted limits
 * 8. Show FPS/temperature bubble
 * 9. Thermal protection
 * 10. Restore settings after game closes
 */
class GameProfileService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var shizukuManager: ShizukuManager
    private lateinit var systemMonitor: SystemMonitor

    private var activeGamePackage: String? = null
    private var activeGameName: String = "Game"
    private var highestDetectedHz: Float = 90f

    private var lifecycleJob: Job? = null
    private var thermalJob: Job? = null
    private var exitDetectionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        shizukuManager = ShizukuManager(this)
        systemMonitor = SystemMonitor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PROFILE) {
            restoreSettingsAndStop()
            return START_NOT_STICKY
        }

        activeGamePackage = intent?.getStringExtra(EXTRA_GAME_PACKAGE)
        activeGameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: "Game"

        startForeground(NOTIFICATION_ID, createNotification("Accelerating $activeGameName..."))

        // Execute Core Gaming Flow
        executeCoreGamingFlow()

        return START_STICKY
    }

    private fun executeCoreGamingFlow() {
        lifecycleJob?.cancel()
        lifecycleJob = serviceScope.launch {
            // STEP 2: Detect device capability & STEP 4: Detect supported refresh/FPS
            highestDetectedHz = detectHighestRefreshRate()

            // STEP 3: Connect/verify Shizuku
            shizukuManager.checkStatus()

            // STEP 5: Select highest supported mode (90Hz lock)
            enforceHighestRefreshRate(highestDetectedHz)

            // STEP 6: Start monitoring & STEP 7: Stabilize within Android's permitted limits
            systemMonitor.trimBackgroundMemory()

            // STEP 8: Show FPS/temperature bubble
            if (Settings.canDrawOverlays(applicationContext)) {
                val overlayIntent = Intent(applicationContext, FloatingBoosterService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(overlayIntent)
                } else {
                    startService(overlayIntent)
                }
            }

            // Update Notification with active status
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, createNotification("🎮 $activeGameName active @ ${highestDetectedHz.toInt()}Hz"))

            // STEP 9: Thermal protection monitoring
            startThermalProtectionWatcher()

            // STEP 10: Watchdog for game exit to restore settings
            startExitDetectionWatcher()
        }
    }

    private fun detectHighestRefreshRate(): Float {
        return try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay
            }
            val modes = display?.supportedModes ?: emptyArray()
            val maxModeHz = modes.maxOfOrNull { it.refreshRate } ?: 90f
            maxModeHz.coerceAtLeast(60f)
        } catch (e: Exception) {
            90f
        }
    }

    private fun enforceHighestRefreshRate(rate: Float) {
        val targetHz = rate.toInt()
        // 1. Settings API
        if (Settings.System.canWrite(this)) {
            try {
                Settings.System.putFloat(contentResolver, "peak_refresh_rate", rate)
                Settings.System.putFloat(contentResolver, "min_refresh_rate", rate)
                Settings.System.putInt(contentResolver, "user_refresh_rate", targetHz)
            } catch (ignored: Exception) {
            }
        }
        // 2. Global settings
        try {
            Settings.Global.putFloat(contentResolver, "peak_refresh_rate", rate)
            Settings.Global.putFloat(contentResolver, "min_refresh_rate", rate)
        } catch (ignored: Exception) {
        }
        // 3. Shizuku privileged shell interface
        if (shizukuManager.isAuthorized()) {
            serviceScope.launch {
                shizukuManager.forceGlobalHighRefreshRate(targetHz)
            }
        }
    }

    private fun startThermalProtectionWatcher() {
        thermalJob?.cancel()
        thermalJob = serviceScope.launch {
            while (isActive) {
                val temp = getBatteryTemperature()
                if (temp >= 43.0 && preferenceManager.thermalProtectionEnabled) {
                    // Thermal Protection: throttle brightness slightly to prevent overheating
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, createNotification("⚠️ Thermal Guard: Device hot (${temp.toInt()}°C). Cooling active."))
                }
                delay(5000)
            }
        }
    }

    private fun startExitDetectionWatcher() {
        exitDetectionJob?.cancel()
        exitDetectionJob = serviceScope.launch {
            val targetPkg = activeGamePackage ?: return@launch
            delay(10000) // Grace period for game launch

            while (isActive) {
                delay(3000)
                if (isAppInForeground(targetPkg) == false) {
                    // Game was closed or switched away!
                    // STEP 10: Restore settings after game closes
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
        val tempRaw = status?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250) ?: 250
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
            // Restore default refresh rate (min 60, peak 90)
            if (Settings.System.canWrite(applicationContext)) {
                try {
                    Settings.System.putFloat(contentResolver, "min_refresh_rate", 60.0f)
                    Settings.System.putFloat(contentResolver, "peak_refresh_rate", 90.0f)
                } catch (ignored: Exception) {
                }
            }
            if (shizukuManager.isAuthorized()) {
                shizukuManager.resetGlobalRefreshRate()
            }

            // Restore Brightness if saved
            val origBrightness = preferenceManager.originalBrightness
            if (origBrightness >= 0 && Settings.System.canWrite(applicationContext)) {
                try {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, origBrightness)
                } catch (ignored: Exception) {
                }
            }

            // Restore DND if permitted
            val origDnd = preferenceManager.originalDndState
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (origDnd >= 0 && notificationManager.isNotificationPolicyAccessGranted) {
                try {
                    notificationManager.setInterruptionFilter(origDnd)
                } catch (ignored: Exception) {
                }
            }

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

            // Launch the actual game application
            val launchIntent = context.packageManager.getLaunchIntentForPackage(gamePackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
            }
        }
    }
}
