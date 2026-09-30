package com.example.telemetry

import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FpsMonitor {

    private var isRunning = false
    private var frameCount = 0
    private var lastFpsTimestamp = 0L

    private val _currentFps = MutableStateFlow(60)
    val currentFps: StateFlow<Int> = _currentFps.asStateFlow()

    private val _fpsHistory = MutableStateFlow<List<Int>>(listOf(60, 60, 60, 60, 60))
    val fpsHistory: StateFlow<List<Int>> = _fpsHistory.asStateFlow()

    private val _stabilityState = MutableStateFlow(StabilityReport(fps = 60, isStable = true, message = "Cadence Stable"))
    val stabilityState: StateFlow<StabilityReport> = _stabilityState.asStateFlow()

    data class StabilityReport(
        val fps: Int,
        val isStable: Boolean,
        val droppedFrames: Int = 0,
        val message: String
    )

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            frameCount++
            val nowMs = System.currentTimeMillis()
            if (lastFpsTimestamp == 0L) {
                lastFpsTimestamp = nowMs
            } else {
                val delta = nowMs - lastFpsTimestamp
                if (delta >= 1000) {
                    val calculatedFps = (frameCount * 1000.0 / delta).toInt().coerceIn(1, 120)
                    _currentFps.value = calculatedFps

                    // Update rolling history for real-time Compose graph
                    val history = _fpsHistory.value.toMutableList()
                    history.add(calculatedFps)
                    if (history.size > 20) {
                        history.removeAt(0)
                    }
                    _fpsHistory.value = history

                    // Stability calculation
                    val isStable = calculatedFps >= 55
                    val drops = if (calculatedFps < 60) 60 - calculatedFps else 0
                    val msg = when {
                        calculatedFps >= 85 -> "Ultra Smooth (90Hz Target)"
                        calculatedFps >= 58 -> "Solid 60 FPS Stability"
                        calculatedFps >= 45 -> "Minor Jitter Detected ($drops fps drop)"
                        else -> "Heavy Throttling Detected! Lower graphics setting"
                    }
                    _stabilityState.value = StabilityReport(
                        fps = calculatedFps,
                        isStable = isStable,
                        droppedFrames = drops,
                        message = msg
                    )

                    frameCount = 0
                    lastFpsTimestamp = nowMs
                }
            }

            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        if (!isRunning) {
            isRunning = true
            frameCount = 0
            lastFpsTimestamp = 0L
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
}
