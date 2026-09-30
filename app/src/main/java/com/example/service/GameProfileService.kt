package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.GameBoostApplication
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class GameProfileService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var preferenceManager: PreferenceManager

    private var activeGamePackage: String? = null
    private var activeGameName: String = "Game"
    private var sessionStartTime: Long = 0L
    private var sessionStartBattery: Int = 100

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PROFILE) {
            restoreSettingsAndStop()
            return START_NOT_STICKY
        }

        activeGamePackage = intent?.getStringExtra(EXTRA_GAME_PACKAGE)
        activeGameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: "Game"
        sessionStartTime = System.currentTimeMillis()
        sessionStartBattery = getCurrentBatteryLevel()

        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun getCurrentBatteryLevel(): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val status = registerReceiver(null, filter)
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 100
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
            .setContentTitle("Active Profile: $activeGameName")
            .setContentText("GameBoost profile applied. Tap to open or Exit to restore settings.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Exit Profile", exitPending)
            .setOngoing(true)
            .build()
    }

    private fun restoreSettingsAndStop() {
        // Restore Brightness if saved
        val origBrightness = preferenceManager.originalBrightness
        if (origBrightness >= 0 && Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    origBrightness
                )
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 902
        const val ACTION_STOP_PROFILE = "com.example.action.STOP_PROFILE"
        const val EXTRA_GAME_PACKAGE = "extra_game_pkg"
        const val EXTRA_GAME_NAME = "extra_game_name"
    }
}
