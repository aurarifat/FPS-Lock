package com.example.service

/**
 * Service interface for privileged system shell operations via Shizuku.
 */
interface IShizukuService {
    /**
     * Executes arbitrary privileged shell command using Shizuku.
     * @param command The shell command string to execute.
     * @return Pair of success flag and command output or error message.
     */
    suspend fun executeCommand(command: String): Pair<Boolean, String>

    /**
     * Executes the shell commands:
     * 'settings put global min_refresh_rate 90'
     * and
     * 'settings put global peak_refresh_rate 90'
     * to force high refresh rates globally when enabled in the app.
     *
     * @param rate The target refresh rate in Hz (default: 90).
     * @return Pair of success flag and result logs.
     */
    suspend fun forceGlobalHighRefreshRate(rate: Int = 90): Pair<Boolean, String>

    /**
     * Resets global refresh rates to standard variable scaling (min 60, peak 90).
     * @return Pair of success flag and result logs.
     */
    suspend fun resetGlobalRefreshRate(): Pair<Boolean, String>

    /**
     * Checks if Shizuku is currently authorized with granted permissions.
     */
    fun isAuthorized(): Boolean
}
