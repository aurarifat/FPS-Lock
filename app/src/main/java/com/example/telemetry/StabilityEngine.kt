package com.example.telemetry

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import com.example.data.preferences.PreferenceManager
import com.example.service.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class FpsMode(val title: String, val targetHz: Float) {
    OFF("OFF", 0f),
    MODE_60HZ("60 FPS / 60 Hz", 60f),
    MODE_90HZ("90 FPS / 90 Hz", 90f),
    AUTO_HIGHEST("AUTO / HIGHEST SUPPORTED", -1f)
}

enum class StabilityStatus(val label: String) {
    STABLE("90 FPS STABLE"),
    STABLE_60("60 FPS STABLE"),
    FPS_DROP("FPS DROP DETECTED"),
    FALLBACK_60HZ("REFRESH RATE FALLBACK DETECTED"),
    THERMAL_WARNING("THERMAL SAFEGUARD ACTIVE"),
    SHIZUKU_DISCONNECTED("SHIZUKU DISCONNECTED"),
    OFF("STABILITY ENGINE IDLE")
}

enum class ThermalTier(val label: String) {
    NORMAL("NORMAL"),
    WARM("WARM"),
    HOT("HOT (THROTTLING)"),
    CRITICAL("CRITICAL OVERHEAT")
}

data class StabilityMetrics(
    val selectedMode: FpsMode = FpsMode.OFF,
    val currentRefreshRate: Float = 60f,
    val targetRefreshRate: Float = 60f,
    val verifiedRefreshRate: Float = 60f,
    val isVerified: Boolean = false,
    val frameTimeMs: Float = 16.7f,
    val status: StabilityStatus = StabilityStatus.OFF,
    val thermalTier: ThermalTier = ThermalTier.NORMAL,
    val temperatureC: Float = 30f,
    val ramUsedGb: Float = 2.0f,
    val ramTotalGb: Float = 4.0f,
    val lastFallbackTimestamp: Long = 0L,
    val fallbackCount: Int = 0,
    val statusMessage: String = "Ready",
    val is90HzSupported: Boolean = false
)

class StabilityEngine(
    private val context: Context,
    private val shizukuManager: ShizukuManager,
    private val preferenceManager: PreferenceManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val _metrics = MutableStateFlow(StabilityMetrics())
    val metrics: StateFlow<StabilityMetrics> = _metrics.asStateFlow()

    private var monitoringJob: Job? = null
    private var lastRecoveryAttemptTime = 0L
    private var recoveryAttemptsInWindow = 0

    // Saved state for clean auto-restore
    private var savedMinRefreshRate: Float? = null
    private var savedPeakRefreshRate: Float? = null
    private var hasSavedOriginalState = false

    init {
        // Observe Shizuku binder status to detect unexpected disconnect during gameplay
        shizukuManager.onStatusChangedListener = { status ->
            if (status != ShizukuManager.ShizukuStatus.CONNECTED && _metrics.value.selectedMode != FpsMode.OFF) {
                _metrics.value = _metrics.value.copy(
                    status = StabilityStatus.SHIZUKU_DISCONNECTED,
                    statusMessage = "Shizuku disconnected. Continuing monitoring with standard Android APIs."
                )
            }
        }
    }

    fun startMonitoring() {
        if (monitoringJob != null && monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            while (isActive) {
                updateTelemetry()
                delay(1500)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    suspend fun setFpsMode(mode: FpsMode, capabilityScanner: CapabilityScanner): Pair<Boolean, String> {
        val caps = capabilityScanner.scanCapabilities(shizukuManager)
        _metrics.value = _metrics.value.copy(
            selectedMode = mode,
            is90HzSupported = caps.is90HzSupported
        )

        if (mode == FpsMode.OFF) {
            restorePreviousState()
            _metrics.value = _metrics.value.copy(
                status = StabilityStatus.OFF,
                statusMessage = "Stability engine turned off. Default refresh rate restored."
            )
            return Pair(true, "Booster display lock disabled. System defaults active.")
        }

        val targetHz = when (mode) {
            FpsMode.MODE_60HZ -> 60f
            FpsMode.MODE_90HZ -> 90f
            FpsMode.AUTO_HIGHEST -> caps.highestSupportedRate
            FpsMode.OFF -> 60f
        }

        // STEP 1: Verify hardware support
        if (targetHz >= 89f && !caps.is90HzSupported) {
            _metrics.value = _metrics.value.copy(
                isVerified = false,
                status = StabilityStatus.STABLE_60,
                statusMessage = "90 FPS mode unavailable on this device (Hardware maximum: ${caps.maxSupportedRefreshRate.toInt()}Hz)."
            )
            return Pair(false, "90 FPS mode unavailable on this device (Hardware maximum: ${caps.maxSupportedRefreshRate.toInt()}Hz).")
        }

        // STEP 2: Save current state before any modification
        saveCurrentStateIfNeeded()

        // STEP 3: Apply the refresh rate using safest available method
        val applied = applyRefreshRate(targetHz)

        // STEP 4: Verification step after delay
        delay(600)
        val verifiedRate = readCurrentHardwareRefreshRate()
        val isVerified = (verifiedRate >= targetHz - 2f)

        val status = if (isVerified) {
            if (verifiedRate >= 85f) StabilityStatus.STABLE else StabilityStatus.STABLE_60
        } else {
            StabilityStatus.FALLBACK_60HZ
        }

        val resultMsg = if (isVerified) {
            "✓ Verified: Hardware refresh rate confirmed at ${verifiedRate.toInt()}Hz"
        } else {
            "⚠ Requested ${targetHz.toInt()}Hz, but hardware rate is currently ${verifiedRate.toInt()}Hz. System power management or OEM mode active."
        }

        _metrics.value = _metrics.value.copy(
            targetRefreshRate = targetHz,
            verifiedRefreshRate = verifiedRate,
            currentRefreshRate = verifiedRate,
            isVerified = isVerified,
            status = status,
            statusMessage = resultMsg
        )

        startMonitoring()
        return Pair(isVerified || applied, resultMsg)
    }

    private suspend fun applyRefreshRate(rate: Float): Boolean {
        val targetHz = rate.toInt()
        var success = false

        // 1. Shizuku Privileged Shell
        if (shizukuManager.isAuthorized()) {
            val (shizukuSuccess, _) = shizukuManager.forceGlobalHighRefreshRate(targetHz)
            if (shizukuSuccess) success = true
        }

        // 2. Settings.System API fallback if permitted
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putFloat(context.contentResolver, "peak_refresh_rate", rate)
                Settings.System.putFloat(context.contentResolver, "min_refresh_rate", rate)
                Settings.System.putInt(context.contentResolver, "user_refresh_rate", targetHz)
                success = true
            } catch (ignored: Exception) {
            }
        }

        // 3. Global Settings attempt
        try {
            Settings.Global.putFloat(context.contentResolver, "peak_refresh_rate", rate)
            Settings.Global.putFloat(context.contentResolver, "min_refresh_rate", rate)
        } catch (ignored: Exception) {
        }

        return success
    }

    private fun updateTelemetry() {
        val currentRate = readCurrentHardwareRefreshRate()
        val temp = getBatteryTemperature()
        val thermal = getThermalTier(temp)

        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val ramUsedGb = ((memInfo.totalMem - memInfo.availMem) / (1024f * 1024f * 1024f))
        val ramTotalGb = (memInfo.totalMem / (1024f * 1024f * 1024f))

        // Frame time: T = 1000 / rate (e.g. 11.1ms for 90Hz, 16.7ms for 60Hz)
        val frameTime = if (currentRate > 0) (1000f / currentRate) else 16.7f

        val currentMetrics = _metrics.value

        // Check for 90 -> 60 Hz fallback
        var newStatus = currentMetrics.status
        var newFallbackCount = currentMetrics.fallbackCount
        var lastFallback = currentMetrics.lastFallbackTimestamp
        var statusMsg = currentMetrics.statusMessage

        if (currentMetrics.selectedMode == FpsMode.MODE_90HZ && currentRate < 75f) {
            val now = System.currentTimeMillis()
            newFallbackCount++
            lastFallback = now
            newStatus = StabilityStatus.FALLBACK_60HZ

            // Thermal check: do not fight Android thermal protection
            if (thermal == ThermalTier.HOT || thermal == ThermalTier.CRITICAL) {
                newStatus = StabilityStatus.THERMAL_WARNING
                statusMsg = "90 Hz cannot currently be maintained by the system due to thermal protection (${temp.toInt()}°C). Stabilizing in Safe Mode."
            } else {
                statusMsg = "REFRESH RATE FALLBACK DETECTED: Hardware dropped to ${currentRate.toInt()}Hz."
                // Safe recovery with cooldown
                if (now - lastRecoveryAttemptTime > 15000 && recoveryAttemptsInWindow < 3) {
                    lastRecoveryAttemptTime = now
                    recoveryAttemptsInWindow++
                    scope.launch {
                        applyRefreshRate(90f)
                    }
                } else if (recoveryAttemptsInWindow >= 3) {
                    statusMsg = "90 Hz cannot currently be maintained by the system. Cooldown active."
                }
            }
        } else if (currentMetrics.selectedMode == FpsMode.MODE_90HZ && currentRate >= 85f) {
            newStatus = StabilityStatus.STABLE
            statusMsg = "90 FPS STABLE"
        } else if (currentMetrics.selectedMode == FpsMode.MODE_60HZ) {
            newStatus = StabilityStatus.STABLE_60
            statusMsg = "60 FPS STABLE"
        }

        _metrics.value = currentMetrics.copy(
            currentRefreshRate = currentRate,
            frameTimeMs = frameTime,
            temperatureC = temp,
            thermalTier = thermal,
            ramUsedGb = ((ramUsedGb * 10).toInt() / 10f),
            ramTotalGb = ((ramTotalGb * 10).toInt() / 10f),
            status = newStatus,
            statusMessage = statusMsg,
            fallbackCount = newFallbackCount,
            lastFallbackTimestamp = lastFallback
        )
    }

    private fun getThermalTier(tempC: Float): ThermalTier {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            val status = powerManager.currentThermalStatus
            if (status >= PowerManager.THERMAL_STATUS_CRITICAL) return ThermalTier.CRITICAL
            if (status >= PowerManager.THERMAL_STATUS_SEVERE) return ThermalTier.HOT
            if (status >= PowerManager.THERMAL_STATUS_MODERATE) return ThermalTier.WARM
        }
        return when {
            tempC >= 44.0f -> ThermalTier.CRITICAL
            tempC >= 41.0f -> ThermalTier.HOT
            tempC >= 38.0f -> ThermalTier.WARM
            else -> ThermalTier.NORMAL
        }
    }

    private fun readCurrentHardwareRefreshRate(): Float {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        return display?.refreshRate ?: 60f
    }

    private fun getBatteryTemperature(): Float {
        val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val raw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300
        return raw / 10.0f
    }

    private fun saveCurrentStateIfNeeded() {
        if (hasSavedOriginalState) return
        try {
            savedMinRefreshRate = Settings.System.getFloat(context.contentResolver, "min_refresh_rate", 60.0f)
            savedPeakRefreshRate = Settings.System.getFloat(context.contentResolver, "peak_refresh_rate", 90.0f)
        } catch (ignored: Exception) {
            savedMinRefreshRate = 60.0f
            savedPeakRefreshRate = 90.0f
        }
        hasSavedOriginalState = true
    }

    suspend fun restorePreviousState() {
        if (!hasSavedOriginalState) return

        val minRate = savedMinRefreshRate ?: 60f
        val peakRate = savedPeakRefreshRate ?: 90f

        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putFloat(context.contentResolver, "min_refresh_rate", minRate)
                Settings.System.putFloat(context.contentResolver, "peak_refresh_rate", peakRate)
            } catch (ignored: Exception) {
            }
        }

        if (shizukuManager.isAuthorized()) {
            shizukuManager.resetGlobalRefreshRate()
        }

        hasSavedOriginalState = false
    }
}
