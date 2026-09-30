package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import com.example.GameBoostApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.TelemetryData
import com.example.data.preferences.PreferenceManager
import com.example.telemetry.FpsMonitor
import com.example.telemetry.SystemMonitor
import com.example.ui.overlay.FloatingOverlayContent
import com.example.ui.theme.Typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Floating Window Service using Jetpack Compose that displays a real-time
 * frame rate counter, hardware telemetry, and anti-lag controls on top of other applications.
 */
class FloatingBoosterService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var systemMonitor: SystemMonitor
    private lateinit var preferenceManager: PreferenceManager
    private val fpsMonitor = FpsMonitor()

    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private val overlayLifecycleOwner = OverlayLifecycleOwner()

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var telemetryJob: Job? = null

    // State flows driving the Compose floating window
    private val isExpanded = MutableStateFlow(false)
    private val currentOpacity = MutableStateFlow(0.90f)
    private val lagKillMessage = MutableStateFlow<String?>(null)
    private val telemetryData = MutableStateFlow(
        TelemetryData(
            ramUsedBytes = 0L,
            ramTotalBytes = 1L,
            batteryPercent = 100,
            batteryTemperatureC = 28f,
            isCharging = false,
            displayRefreshRate = 60f,
            liveFps = 60
        )
    )

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        systemMonitor = SystemMonitor(this)
        preferenceManager = PreferenceManager(this)
        currentOpacity.value = preferenceManager.overlayOpacity

        overlayLifecycleOwner.onCreate()
        overlayLifecycleOwner.onStart()

        startForeground(NOTIFICATION_ID, createNotification())
        createComposeFloatingWindow()
        fpsMonitor.start()
        startTelemetryPolling()
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

        val stopIntent = Intent(this, FloatingBoosterService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GameBoostApplication.CHANNEL_ID)
            .setContentTitle("Real-Time FPS Overlay Active")
            .setContentText("Monitoring live frame rate on top of other apps")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun createComposeFloatingWindow() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 220
        }

        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            overlayLifecycleOwner.attachTo(this)

            setContent {
                MaterialTheme(typography = Typography) {
                    val liveFps by fpsMonitor.currentFps.collectAsState()
                    val fpsHistory by fpsMonitor.fpsHistory.collectAsState()
                    val stability by fpsMonitor.stabilityState.collectAsState()
                    val expanded by isExpanded.collectAsState()
                    val opacity by currentOpacity.collectAsState()
                    val telemetry by telemetryData.collectAsState()
                    val message by lagKillMessage.collectAsState()

                    FloatingOverlayContent(
                        fps = liveFps,
                        fpsHistory = fpsHistory,
                        stability = stability,
                        telemetry = telemetry,
                        isExpanded = expanded,
                        opacity = opacity,
                        lagKillMessage = message,
                        onToggleExpanded = {
                            isExpanded.value = !isExpanded.value
                        },
                        onClose = {
                            stopSelf()
                        },
                        onForceKillLag = {
                            executeForceKillLag()
                        },
                        onChangeOpacity = { newOpacity ->
                            currentOpacity.value = newOpacity
                            preferenceManager.overlayOpacity = newOpacity
                        },
                        onDragDelta = { dx, dy ->
                            params?.let { p ->
                                p.x += dx.toInt()
                                p.y += dy.toInt()
                                try {
                                    windowManager.updateViewLayout(composeView, p)
                                } catch (ignored: Exception) {
                                }
                            }
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun executeForceKillLag() {
        serviceScope.launch {
            val freedMb = systemMonitor.trimBackgroundMemory()
            lagKillMessage.value = "⚡ LAG PURGED: +${freedMb}MB RAM Freed!"
            delay(3500)
            lagKillMessage.value = null
        }
    }

    private fun startTelemetryPolling() {
        telemetryJob = serviceScope.launch {
            while (isActive) {
                val currentFps = fpsMonitor.currentFps.value
                telemetryData.value = systemMonitor.getTelemetry(liveFps = currentFps)
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
        fpsMonitor.stop()
        overlayLifecycleOwner.onDestroy()

        composeView?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (ignored: Exception) {
            }
            composeView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 901
        const val ACTION_STOP = "com.example.action.STOP_OVERLAY"
    }
}
