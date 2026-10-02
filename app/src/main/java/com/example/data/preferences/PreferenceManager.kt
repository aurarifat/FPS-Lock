package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gameboost_settings", Context.MODE_PRIVATE)

    // Original system settings backups
    var originalWindowAnimationScale: Float
        get() = prefs.getFloat(KEY_ORIG_WINDOW_ANIM, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ORIG_WINDOW_ANIM, value).apply()

    var originalTransitionAnimationScale: Float
        get() = prefs.getFloat(KEY_ORIG_TRANS_ANIM, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ORIG_TRANS_ANIM, value).apply()

    var originalAnimatorDurationScale: Float
        get() = prefs.getFloat(KEY_ORIG_ANIM_DUR, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ORIG_ANIM_DUR, value).apply()

    var originalBrightness: Int
        get() = prefs.getInt(KEY_ORIG_BRIGHTNESS, -1)
        set(value) = prefs.edit().putInt(KEY_ORIG_BRIGHTNESS, value).apply()

    var originalDndState: Int
        get() = prefs.getInt(KEY_ORIG_DND, -1)
        set(value) = prefs.edit().putInt(KEY_ORIG_DND, value).apply()

    // Overlay settings
    var overlayOpacity: Float
        get() = prefs.getFloat(KEY_OVERLAY_OPACITY, 0.85f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY_OPACITY, value).apply()

    var overlaySize: String
        get() = prefs.getString(KEY_OVERLAY_SIZE, "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString(KEY_OVERLAY_SIZE, value).apply()

    var overlayShowFps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FPS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_FPS, value).apply()

    var overlayShowRefreshRate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_REFRESH_RATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_REFRESH_RATE, value).apply()

    var overlayShowRam: Boolean
        get() = prefs.getBoolean(KEY_SHOW_RAM, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_RAM, value).apply()

    var overlayShowTemp: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TEMP, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TEMP, value).apply()

    var overlayShowBattery: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BATTERY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BATTERY, value).apply()

    // Ping test
    var pingHost: String
        get() = prefs.getString(KEY_PING_HOST, "8.8.8.8") ?: "8.8.8.8"
        set(value) = prefs.edit().putString(KEY_PING_HOST, value).apply()

    // Stability mode
    var stabilityModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_STABILITY_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_STABILITY_MODE, value).apply()

    // Dynamic Theme (Light / Dark mode)
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            _isDarkModeFlow.value = value
        }

    // Haptic Feedback
    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    init {
        _isDarkModeFlow.value = isDarkMode
    }

    companion object {
        private const val KEY_DARK_MODE = "app_dark_mode"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"
        private val _isDarkModeFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
        val isDarkModeFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isDarkModeFlow

        private const val KEY_ORIG_WINDOW_ANIM = "orig_window_anim"
        private const val KEY_ORIG_TRANS_ANIM = "orig_trans_anim"
        private const val KEY_ORIG_ANIM_DUR = "orig_anim_dur"
        private const val KEY_ORIG_BRIGHTNESS = "orig_brightness"
        private const val KEY_ORIG_DND = "orig_dnd"

        private const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        private const val KEY_OVERLAY_SIZE = "overlay_size"
        private const val KEY_SHOW_FPS = "overlay_show_fps"
        private const val KEY_SHOW_REFRESH_RATE = "overlay_show_refresh_rate"
        private const val KEY_SHOW_RAM = "overlay_show_ram"
        private const val KEY_SHOW_TEMP = "overlay_show_temp"
        private const val KEY_SHOW_BATTERY = "overlay_show_battery"

        private const val KEY_PING_HOST = "ping_host"
        private const val KEY_STABILITY_MODE = "stability_mode"
    }
}
