package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.preferences.PreferenceManager

/**
 * Utility for providing sensory haptic feedback across buttons,
 * switches, theme changes, and floating overlay actions.
 */
object HapticHelper {

    enum class HapticType {
        LIGHT,
        MEDIUM,
        SUCCESS,
        HEAVY
    }

    fun performHaptic(context: Context, type: HapticType = HapticType.LIGHT) {
        try {
            val prefs = PreferenceManager(context)
            if (!prefs.hapticFeedbackEnabled) return

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = when (type) {
                        HapticType.LIGHT -> VibrationEffect.createOneShot(18, 120)
                        HapticType.MEDIUM -> VibrationEffect.createOneShot(35, 180)
                        HapticType.SUCCESS -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                            } else {
                                VibrationEffect.createOneShot(25, 200)
                            }
                        }
                        HapticType.HEAVY -> VibrationEffect.createOneShot(60, 255)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            }
        } catch (ignored: Exception) {
        }
    }
}
