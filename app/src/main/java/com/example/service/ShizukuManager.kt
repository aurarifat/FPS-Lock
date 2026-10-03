package com.example.service

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuManager(private val context: Context) : IShizukuService {

    enum class ShizukuStatus {
        NOT_INSTALLED,
        SERVICE_STOPPED,
        PERMISSION_REQUIRED,
        AUTHORIZED,
        ERROR
    }

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _lastCommandOutput = MutableStateFlow<String?>(null)
    val lastCommandOutput: StateFlow<String?> = _lastCommandOutput.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        checkStatus()
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            _status.value = ShizukuStatus.AUTHORIZED
        } else {
            _status.value = ShizukuStatus.PERMISSION_REQUIRED
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (ignored: Throwable) {
        }
        checkStatus()
    }

    fun isShizukuInstalled(): Boolean {
        val pm = context.packageManager
        val packagesToCheck = listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")
        for (pkg in packagesToCheck) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (ignored: Exception) {
            }
        }
        val intent = android.content.Intent("moe.shizuku.manager.action.START").setPackage("moe.shizuku.privileged.api")
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved.isNotEmpty()
    }

    fun checkStatus() {
        try {
            val ping = Shizuku.pingBinder()
            if (ping) {
                val granted = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Throwable) {
                    false
                }
                _status.value = if (granted) ShizukuStatus.AUTHORIZED else ShizukuStatus.PERMISSION_REQUIRED
                return
            }
        } catch (t: Throwable) {
            // Binder ping failed, proceed to installation check
        }

        val installed = isShizukuInstalled()
        _status.value = if (installed) ShizukuStatus.SERVICE_STOPPED else ShizukuStatus.NOT_INSTALLED
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    _status.value = ShizukuStatus.AUTHORIZED
                    return
                }
                Shizuku.requestPermission(requestCode)
            } else {
                checkStatus()
            }
        } catch (e: Exception) {
            _lastCommandOutput.value = "Failed to request permission: ${e.message}"
        }
    }

    fun openShizukuApp() {
        val pm = context.packageManager
        val packagesToCheck = listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")
        for (pkg in packagesToCheck) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        }
        // Fallback: Open Play Store for Shizuku
        try {
            val marketIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=moe.shizuku.privileged.api")
            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://shizuku.rikka.app/")
            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webIntent)
        }
    }

    override suspend fun executeCommand(command: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (_status.value != ShizukuStatus.AUTHORIZED) {
            return@withContext Pair(false, "Shizuku not authorized or not running")
        }

        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append("ERR: ").append(line).append("\n")
            }

            val exitCode = process.waitFor()
            val result = output.toString().trim()
            _lastCommandOutput.value = if (result.isBlank()) "Success (code $exitCode)" else result
            Pair(exitCode == 0, if (result.isBlank()) "Executed successfully" else result)
        } catch (e: Exception) {
            val err = "Command failed: ${e.message}"
            _lastCommandOutput.value = err
            Pair(false, err)
        }
    }

    override fun isAuthorized(): Boolean {
        return _status.value == ShizukuStatus.AUTHORIZED
    }

    override suspend fun forceGlobalHighRefreshRate(rate: Int): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isAuthorized()) {
            val msg = "Shizuku not authorized. Please start Shizuku and grant permission."
            _lastCommandOutput.value = msg
            return@withContext Pair(false, msg)
        }

        // Execute user requested commands to force high refresh rate globally
        val cmdGlobalMin = "settings put global min_refresh_rate $rate"
        val cmdGlobalPeak = "settings put global peak_refresh_rate $rate"
        val cmdSystemMin = "settings put system min_refresh_rate $rate"
        val cmdSystemPeak = "settings put system peak_refresh_rate $rate"
        val cmdUserRate = "settings put system user_refresh_rate $rate"

        val combinedCmd = "$cmdGlobalMin && $cmdGlobalPeak && $cmdSystemMin && $cmdSystemPeak && $cmdUserRate"
        val (success, output) = executeCommand(combinedCmd)

        val logMessage = if (success) {
            "✓ Executed '$cmdGlobalMin' and '$cmdGlobalPeak' successfully via Shizuku service interface"
        } else {
            "Failed to execute high refresh rate command: $output"
        }
        _lastCommandOutput.value = logMessage
        Pair(success, logMessage)
    }

    override suspend fun resetGlobalRefreshRate(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isAuthorized()) {
            return@withContext Pair(false, "Shizuku not authorized")
        }
        val cmd = "settings put global min_refresh_rate 60 && settings put global peak_refresh_rate 90 && settings put system min_refresh_rate 60 && settings put system peak_refresh_rate 90"
        executeCommand(cmd)
    }

    suspend fun setAnimationScales(window: Float, transition: Float, animator: Float): Boolean {
        val cmd = "settings put global window_animation_scale $window && " +
                "settings put global transition_animation_scale $transition && " +
                "settings put global animator_duration_scale $animator"
        val (success, _) = executeCommand(cmd)
        return success
    }

    suspend fun setRefreshRate(targetRate: Float): Boolean {
        val rateInt = targetRate.toInt()
        val cmd = "settings put system peak_refresh_rate $rateInt && " +
                "settings put system min_refresh_rate $rateInt && " +
                "settings put system user_refresh_rate $rateInt"
        val (success, _) = executeCommand(cmd)
        return success
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (ignored: Throwable) {
        }
    }
}
