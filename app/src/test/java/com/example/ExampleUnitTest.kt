package com.example

import com.example.telemetry.FpsMonitor
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFpsMonitorDefaults() {
    val monitor = FpsMonitor()
    assertEquals(60, monitor.currentFps.value)
    assertTrue(monitor.fpsHistory.value.isNotEmpty())
    assertTrue(monitor.stabilityState.value.isStable)
  }

  @Test
  fun testStabilityCalculations() {
    val stable = FpsMonitor.StabilityReport(fps = 60, isStable = true, droppedFrames = 0, message = "Solid 60 FPS Stability")
    assertTrue(stable.isStable)
    assertEquals(0, stable.droppedFrames)

    val throttled = FpsMonitor.StabilityReport(fps = 38, isStable = false, droppedFrames = 22, message = "Heavy Throttling Detected")
    assertFalse(throttled.isStable)
    assertEquals(22, throttled.droppedFrames)
  }
}
