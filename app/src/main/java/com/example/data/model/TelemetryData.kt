package com.example.data.model

data class DeviceInfo(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val chipset: String,
    val totalRamBytes: Long,
    val totalStorageBytes: Long,
    val freeStorageBytes: Long,
    val supportedRefreshRates: List<Float>,
    val currentRefreshRate: Float,
    val isXPadDetected: Boolean
)

data class TelemetryData(
    val liveFps: Int = 60,
    val displayRefreshRate: Float = 60f,
    val ramUsedBytes: Long = 0L,
    val ramTotalBytes: Long = 0L,
    val batteryPercent: Int = 100,
    val batteryTemperatureC: Float = 28f,
    val isCharging: Boolean = false,
    val pingMs: Int = -1,
    val pingHost: String = "8.8.8.8",
    val networkType: String = "Wi-Fi",
    val networkSpeedMbps: Int = 0
)

data class OptimizationStep(
    val id: String,
    val title: String,
    val description: String,
    val status: StepStatus,
    val details: String = ""
)

enum class StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    SKIPPED,
    PERMISSION_NEEDED,
    FAILED
}

data class OptimizationResult(
    val timestamp: Long = System.currentTimeMillis(),
    val ramFreedMb: Long = 0L,
    val steps: List<OptimizationStep> = emptyList(),
    val isReversible: Boolean = true
)
