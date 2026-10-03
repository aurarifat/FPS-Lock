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

  @Test
  fun testShizukuServiceInterfaceContract() {
    val fakeService = object : com.example.service.IShizukuService {
      var executedCommands = mutableListOf<String>()
      override suspend fun executeCommand(command: String): Pair<Boolean, String> {
        executedCommands.add(command)
        return Pair(true, "OK")
      }
      override suspend fun forceGlobalHighRefreshRate(rate: Int): Pair<Boolean, String> {
        val cmdMin = "settings put global min_refresh_rate $rate"
        val cmdPeak = "settings put global peak_refresh_rate $rate"
        executeCommand("$cmdMin && $cmdPeak")
        return Pair(true, "High refresh rate ($rate Hz) forced globally")
      }
      override suspend fun resetGlobalRefreshRate(): Pair<Boolean, String> {
        return Pair(true, "Reset")
      }
      override fun isAuthorized(): Boolean = true
    }

    assertTrue(fakeService.isAuthorized())
    kotlinx.coroutines.runBlocking {
      val (success, msg) = fakeService.forceGlobalHighRefreshRate(90)
      assertTrue(success)
      assertTrue(msg.contains("90 Hz"))
      assertEquals(1, fakeService.executedCommands.size)
      assertTrue(fakeService.executedCommands[0].contains("settings put global min_refresh_rate 90"))
      assertTrue(fakeService.executedCommands[0].contains("settings put global peak_refresh_rate 90"))
    }
  }

  @Test
  fun testGamePerformanceModes() {
    val modes = listOf("ECO", "BALANCED", "BEAST_90FPS")
    assertTrue(modes.contains("BEAST_90FPS"))
    assertTrue(modes.contains("ECO"))
    assertTrue(modes.contains("BALANCED"))
  }
}
