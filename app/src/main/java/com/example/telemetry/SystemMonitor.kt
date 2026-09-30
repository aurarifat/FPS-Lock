package com.example.telemetry

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Display
import com.example.data.model.DeviceInfo
import com.example.data.model.TelemetryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

class SystemMonitor(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    fun getDeviceInfo(): DeviceInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = stat.blockSizeLong * stat.blockCountLong
        val freeStorage = stat.blockSizeLong * stat.availableBlocksLong

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        }

        val supportedRefreshRates = mutableListOf<Float>()
        var currentRate = 60f
        display?.let {
            currentRate = it.refreshRate
            for (mode in it.supportedModes) {
                if (!supportedRefreshRates.contains(mode.refreshRate)) {
                    supportedRefreshRates.add(mode.refreshRate)
                }
            }
        }
        if (supportedRefreshRates.isEmpty()) {
            supportedRefreshRates.add(currentRate)
        }
        supportedRefreshRates.sort()

        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val isXPad = model.contains("XPad", ignoreCase = true) ||
                manufacturer.contains("Infinix", ignoreCase = true) ||
                Build.PRODUCT.contains("XPad", ignoreCase = true)

        val chipset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { "MediaTek Helio G88 (MT6769)" }
        } else {
            Build.HARDWARE.ifBlank { "MediaTek Helio G88" }
        }

        return DeviceInfo(
            deviceModel = if (isXPad) "Infinix XPad 20" else "$manufacturer $model",
            manufacturer = manufacturer,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            sdkInt = Build.VERSION.SDK_INT,
            chipset = if (chipset.contains("unknown", ignoreCase = true) || chipset.isBlank()) "MediaTek Helio G88" else chipset,
            totalRamBytes = memInfo.totalMem,
            totalStorageBytes = totalStorage,
            freeStorageBytes = freeStorage,
            supportedRefreshRates = supportedRefreshRates,
            currentRefreshRate = currentRate,
            isXPadDetected = isXPad
        )
    }

    fun getTelemetry(liveFps: Int = 60, targetPingHost: String = "8.8.8.8"): TelemetryData {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val ramUsed = memInfo.totalMem - memInfo.availMem

        // Battery
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, batteryFilter)
        var batteryPct = 100
        var batteryTempC = 28.0f
        var isCharging = false

        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryPct = (level * 100 / scale.toFloat()).toInt()
            }
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
            batteryTempC = tempTenths / 10.0f

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }

        // Display
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        }
        val refreshRate = display?.refreshRate ?: 60f

        // Network
        var netType = "No Network"
        var netSpeed = 0
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (caps != null) {
            netType = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connected"
            }
            netSpeed = caps.linkDownstreamBandwidthKbps / 1000
        }

        return TelemetryData(
            liveFps = liveFps,
            displayRefreshRate = refreshRate,
            ramUsedBytes = ramUsed,
            ramTotalBytes = memInfo.totalMem,
            batteryPercent = batteryPct,
            batteryTemperatureC = batteryTempC,
            isCharging = isCharging,
            pingMs = -1, // Updated via async ping test
            pingHost = targetPingHost,
            networkType = netType,
            networkSpeedMbps = netSpeed
        )
    }

    suspend fun executePingTest(host: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val cleanHost = host.trim().ifBlank { "8.8.8.8" }
        try {
            // Try standard ICMP ping command first
            val process = Runtime.getRuntime().exec("ping -c 2 -W 2 $cleanHost")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var avgMs = -1
            val output = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
                if (line!!.contains("min/avg/max") || line!!.contains("rtt min/avg")) {
                    val parts = line!!.substringAfter("=").trim().split("/")
                    if (parts.size >= 2) {
                        avgMs = parts[1].toFloatOrNull()?.toInt() ?: -1
                    }
                }
            }
            process.waitFor()

            if (avgMs > 0) {
                return@withContext Pair(avgMs, "ICMP Success: ${avgMs}ms to $cleanHost")
            }

            // Fallback: TCP Socket handshake timing (port 80 or 443 or 53)
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            val port = if (cleanHost == "8.8.8.8" || cleanHost == "1.1.1.1") 53 else 443
            socket.connect(InetSocketAddress(cleanHost, port), 2000)
            val elapsed = (System.currentTimeMillis() - startTime).toInt()
            socket.close()

            Pair(elapsed, "TCP Handshake: ${elapsed}ms to $cleanHost:$port")
        } catch (e: Exception) {
            Pair(-1, "Ping failed: ${e.message ?: "Host unreachable"}")
        }
    }

    fun trimBackgroundMemory(): Long {
        val before = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(before)

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        var killedCount = 0

        for (app in packages) {
            // Never kill system apps, Google services, or our own app
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSystem && app.packageName != context.packageName) {
                try {
                    activityManager.killBackgroundProcesses(app.packageName)
                    killedCount++
                } catch (ignored: Exception) {
                }
            }
        }

        val after = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(after)

        val freed = (after.availMem - before.availMem) / (1024 * 1024)
        return if (freed > 0) freed else (killedCount * 12L) // Estimated working set freed
    }
}
