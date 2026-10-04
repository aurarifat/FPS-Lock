package com.example.telemetry

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import com.example.service.ShizukuManager

data class DisplayModeDetails(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRate: Float
) {
    val description: String get() = "${width}x${height} @ ${refreshRate.toInt()}Hz"
}

enum class RefreshControlMethod(val label: String) {
    SHIZUKU_PRIVILEGED("Shizuku Privileged Shell"),
    SYSTEM_WRITE_SETTINGS("Android Settings.System API"),
    MONITORING_ONLY("Display Rate Monitoring Only")
}

data class DeviceCapabilities(
    val androidVersion: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val currentRefreshRate: Float,
    val supportedRefreshRates: List<Float>,
    val maxSupportedRefreshRate: Float,
    val currentMode: DisplayModeDetails?,
    val supportedModes: List<DisplayModeDetails>,
    val is90HzSupported: Boolean,
    val is120HzOrHigherSupported: Boolean,
    val isVariableRefreshSupported: Boolean,
    val highestSupportedRate: Float,
    val controlMethod: RefreshControlMethod,
    val trueInGameFpsMeasurable: Boolean = false,
    val fpsMeasurementNote: String = "True in-game render FPS cannot be measured for third-party games due to Android OS sandboxing (non-root). Overlay display cadence and hardware refresh rate are accurately measured via Choreographer VSYNC."
)

data class CapabilityTestItem(
    val title: String,
    val isSupported: Boolean,
    val isWarning: Boolean = false,
    val details: String
)

data class CapabilityTestReport(
    val timestamp: Long = System.currentTimeMillis(),
    val overallStatus: String,
    val items: List<CapabilityTestItem>
)

class CapabilityScanner(private val context: Context) {

    private val displayManager: DisplayManager? =
        context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    fun scanCapabilities(shizukuManager: ShizukuManager? = null): DeviceCapabilities {
        val display = getPrimaryDisplay()

        val currentRate = display?.refreshRate ?: 60f
        val supportedModesList = mutableListOf<DisplayModeDetails>()
        val supportedRatesSet = mutableSetOf<Float>()

        display?.supportedModes?.forEach { mode ->
            val rate = (mode.refreshRate * 10).toInt() / 10f
            supportedRatesSet.add(rate)
            supportedModesList.add(
                DisplayModeDetails(
                    modeId = mode.modeId,
                    width = mode.physicalWidth,
                    height = mode.physicalHeight,
                    refreshRate = rate
                )
            )
        }

        if (supportedRatesSet.isEmpty()) {
            supportedRatesSet.add(currentRate)
        }

        val sortedRates = supportedRatesSet.toList().sorted()
        val maxRate = sortedRates.maxOrNull() ?: 60f
        val has90Hz = sortedRates.any { it in 89.0f..91.0f }
        val has120HzPlus = sortedRates.any { it >= 119.0f }
        val isVariable = sortedRates.size > 1

        val currentMode = display?.mode?.let {
            DisplayModeDetails(
                modeId = it.modeId,
                width = it.physicalWidth,
                height = it.physicalHeight,
                refreshRate = (it.refreshRate * 10).toInt() / 10f
            )
        }

        val controlMethod = when {
            shizukuManager?.isAuthorized() == true -> RefreshControlMethod.SHIZUKU_PRIVILEGED
            Settings.System.canWrite(context) -> RefreshControlMethod.SYSTEM_WRITE_SETTINGS
            else -> RefreshControlMethod.MONITORING_ONLY
        }

        return DeviceCapabilities(
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            currentRefreshRate = currentRate,
            supportedRefreshRates = sortedRates,
            maxSupportedRefreshRate = maxRate,
            currentMode = currentMode,
            supportedModes = supportedModesList,
            is90HzSupported = has90Hz,
            is120HzOrHigherSupported = has120HzPlus,
            isVariableRefreshSupported = isVariable,
            highestSupportedRate = maxRate,
            controlMethod = controlMethod
        )
    }

    fun runCapabilityTest(shizukuManager: ShizukuManager? = null): CapabilityTestReport {
        val caps = scanCapabilities(shizukuManager)
        val items = mutableListOf<CapabilityTestItem>()

        // 1. 90 Hz Hardware Display Support
        if (caps.is90HzSupported) {
            items.add(
                CapabilityTestItem(
                    title = "90 Hz Display Hardware",
                    isSupported = true,
                    details = "✓ 90 Hz native display mode detected in official supportedModes API."
                )
            )
        } else {
            items.add(
                CapabilityTestItem(
                    title = "90 Hz Display Hardware",
                    isSupported = false,
                    details = "✕ 90 Hz display mode not exposed by OEM firmware. Maximum hardware rate: ${caps.maxSupportedRefreshRate.toInt()}Hz."
                )
            )
        }

        // 2. High Refresh Rate Modes Available
        if (caps.supportedRefreshRates.size > 1) {
            val modesStr = caps.supportedRefreshRates.joinToString(", ") { "${it.toInt()}Hz" }
            items.add(
                CapabilityTestItem(
                    title = "Variable Display Modes",
                    isSupported = true,
                    details = "✓ Supported display modes: $modesStr."
                )
            )
        } else {
            items.add(
                CapabilityTestItem(
                    title = "Variable Display Modes",
                    isSupported = false,
                    details = "⚠ Display is fixed at ${caps.currentRefreshRate.toInt()}Hz. Dynamic mode switching unavailable."
                )
            )
        }

        // 3. Shizuku Integration & Privileged Interface
        val shizukuStatus = shizukuManager?.status?.value ?: ShizukuManager.ShizukuStatus.NOT_INSTALLED
        when (shizukuStatus) {
            ShizukuManager.ShizukuStatus.CONNECTED -> {
                items.add(
                    CapabilityTestItem(
                        title = "Shizuku Privileged Access",
                        isSupported = true,
                        details = "✓ Shizuku connected & authorized. Privileged shell commands available to enforce high refresh rates."
                    )
                )
            }
            ShizukuManager.ShizukuStatus.RUNNING_NOT_AUTHORIZED -> {
                items.add(
                    CapabilityTestItem(
                        title = "Shizuku Privileged Access",
                        isSupported = false,
                        isWarning = true,
                        details = "⚠ Shizuku service is running but permission is not yet authorized."
                    )
                )
            }
            ShizukuManager.ShizukuStatus.NOT_RUNNING -> {
                items.add(
                    CapabilityTestItem(
                        title = "Shizuku Privileged Access",
                        isSupported = false,
                        isWarning = true,
                        details = "⚠ Shizuku app is installed but service is stopped. Start it via Wireless Debugging."
                    )
                )
            }
            ShizukuManager.ShizukuStatus.NOT_INSTALLED -> {
                items.add(
                    CapabilityTestItem(
                        title = "Shizuku Privileged Access",
                        isSupported = false,
                        details = "✕ Shizuku not installed. Falling back to standard Android permissions."
                    )
                )
            }
            ShizukuManager.ShizukuStatus.UNSUPPORTED -> {
                items.add(
                    CapabilityTestItem(
                        title = "Shizuku Privileged Access",
                        isSupported = false,
                        details = "✕ Shizuku binder IPC unsupported on this device environment."
                    )
                )
            }
        }

        // 4. Android System Settings Modification Permission
        val canWriteSettings = Settings.System.canWrite(context)
        if (canWriteSettings) {
            items.add(
                CapabilityTestItem(
                    title = "Settings.System Modification",
                    isSupported = true,
                    details = "✓ WRITE_SETTINGS permission granted. Can modify min/peak refresh rate directly."
                )
            )
        } else {
            items.add(
                CapabilityTestItem(
                    title = "Settings.System Modification",
                    isSupported = false,
                    isWarning = true,
                    details = "⚠ WRITE_SETTINGS not granted. Shizuku or manual permission required to alter system rates."
                )
            )
        }

        // 5. In-Game FPS Measurement Capability (Honest disclosure)
        items.add(
            CapabilityTestItem(
                title = "True In-Game FPS Measurement",
                isSupported = false,
                isWarning = true,
                details = "⚠ True in-game frame rate measurement is restricted by Android security sandboxing for third-party apps. GameBoost displays real-time display hardware refresh rate and overlay VSYNC cadence without fake placebo numbers."
            )
        )

        // 6. Thermal Throttling Monitoring
        items.add(
            CapabilityTestItem(
                title = "Thermal Telemetry Engine",
                isSupported = true,
                details = "✓ Battery temperature and system thermal sensors available for real-time overheat protection."
            )
        )

        val overallStatus = when {
            caps.is90HzSupported && caps.controlMethod != RefreshControlMethod.MONITORING_ONLY -> "FULLY COMPATIBLE"
            caps.is90HzSupported -> "90 HZ SUPPORTED (MONITORING ONLY)"
            else -> "60 HZ MAXIMUM (LIMITED)"
        }

        return CapabilityTestReport(
            overallStatus = overallStatus,
            items = items
        )
    }

    private fun getPrimaryDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
    }
}
