package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.GameBoostApplication
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background watchdog service that permanently enforces a 90 FPS / 90Hz hardware lock.
 * Prevents Android, battery savers, and third-party apps from dropping the refresh rate down to 60Hz.
 */
class RefreshRateGuardianService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var shizukuManager: ShizukuManager

    private var watchdogJob: Job? = null
    private var contentObserver: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        shizukuManager = ShizukuManager(this)

        startForeground(NOTIFICATION_ID, createNotification())
        registerRefreshRateObserver()
        startWatchdog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_90FPS_LOCK) {
            stopLockAndSelf()
            return START_NOT_STICKY
        }
        enforce90HzLock()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, RefreshRateGuardianService::class.java).apply {
            action = ACTION_STOP_90FPS_LOCK
        }
        val stopPending = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GameBoostApplication.CHANNEL_ID)
            .setContentTitle("⚡ 90 FPS Global Lock Active")
            .setContentText("Display locked at 90Hz. Android 60Hz downclocking blocked across all apps.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Unlock 90Hz", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun registerRefreshRateObserver() {
        val handler = Handler(Looper.getMainLooper())
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                enforce90HzLock()
            }
        }

        try {
            contentResolver.registerContentObserver(
                Settings.System.getUriFor("min_refresh_rate"),
                false,
                contentObserver!!
            )
            contentResolver.registerContentObserver(
                Settings.System.getUriFor("peak_refresh_rate"),
                false,
                contentObserver!!
            )
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor("min_refresh_rate"),
                false,
                contentObserver!!
            )
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor("peak_refresh_rate"),
                false,
                contentObserver!!
            )
        } catch (ignored: Exception) {
        }
    }

    private fun startWatchdog() {
        watchdogJob = serviceScope.launch {
            while (isActive) {
                enforce90HzLock()
                delay(2000)
            }
        }
    }

    private fun enforce90HzLock() {
        serviceScope.launch {
            // 1. Write Settings API (if permitted)
            if (Settings.System.canWrite(applicationContext)) {
                try {
                    Settings.System.putFloat(contentResolver, "peak_refresh_rate", 90.0f)
                    Settings.System.putFloat(contentResolver, "min_refresh_rate", 90.0f)
                    Settings.System.putInt(contentResolver, "user_refresh_rate", 90)
                } catch (ignored: Exception) {
                }
            }

            // 2. Global settings attempt
            try {
                Settings.Global.putFloat(contentResolver, "peak_refresh_rate", 90.0f)
                Settings.Global.putFloat(contentResolver, "min_refresh_rate", 90.0f)
            } catch (ignored: Exception) {
            }

            // 3. Shizuku privileged shell execution (Permanent hardware lock)
            if (shizukuManager.isAuthorized()) {
                shizukuManager.forceGlobalHighRefreshRate(90)
            }
        }
    }

    private fun stopLockAndSelf() {
        preferenceManager.force90FpsLockEnabled = false

        serviceScope.launch {
            // Restore default variable refresh rate (min 60, peak 90)
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
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogJob?.cancel()
        contentObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (ignored: Exception) {
            }
        }
        shizukuManager.destroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 903
        const val ACTION_STOP_90FPS_LOCK = "com.example.action.STOP_90FPS_LOCK"

        fun startService(context: Context) {
            val intent = Intent(context, RefreshRateGuardianService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RefreshRateGuardianService::class.java).apply {
                action = ACTION_STOP_90FPS_LOCK
            }
            context.startService(intent)
        }
    }
}
