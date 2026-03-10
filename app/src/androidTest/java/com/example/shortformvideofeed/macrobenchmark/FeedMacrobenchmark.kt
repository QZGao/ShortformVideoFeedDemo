package com.example.shortformvideofeed.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupFeedActivity() = benchmarkRule.measureRepeated(
        packageName = "com.example.shortformvideofeed",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.None(),
        setupBlock = {
            pressHome()
        },
        measureBlock = {
            startActivityAndWait()
        }
    )

    @Test
    fun feedScrollTransitionLatency() = benchmarkRule.measureRepeated(
        packageName = "com.example.shortformvideofeed",
        metrics = listOf(FrameTimingMetric()),
        iterations = 3,
        startupMode = StartupMode.WARM,
        setupBlock = {
            startActivityAndWait()
        },
        measureBlock = {
            repeat(8) {
                device.swipe(
                    device.displayWidth / 2,
                    (device.displayHeight * 0.82).toInt(),
                    device.displayWidth / 2,
                    (device.displayHeight * 0.18).toInt(),
                    20
                )
                device.waitForIdle()
            }
        }
    )
}
