package me.yuhan8954.flashback.ui

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class ReplayTimelineViewport {
    data class RulerTick(val timeNanos: Long, val x: Int, val major: Boolean)

    var visibleStartNanos = 0.0
    var nanosPerPixel = 1.0
    private var durationNanos = 0L
    private var width = 1

    fun fit(duration: Long, pixelWidth: Int) {
        if (duration == durationNanos && pixelWidth == width) return
        durationNanos = duration.coerceAtLeast(0L)
        width = pixelWidth.coerceAtLeast(1)
        nanosPerPixel = (durationNanos.toDouble() / width).coerceAtLeast(1.0)
        visibleStartNanos = 0.0
    }

    fun timeToX(time: Long): Int = ((time - visibleStartNanos) / nanosPerPixel).roundToInt()

    fun xToTime(x: Int): Long = (visibleStartNanos + x * nanosPerPixel).roundToLong().coerceIn(0L, durationNanos)

    fun zoom(x: Int, factor: Double) {
        val anchor = xToTime(x)
        val fit = (durationNanos.toDouble() / width).coerceAtLeast(1.0)
        nanosPerPixel = (nanosPerPixel / factor).coerceIn(minOf(5_000_000.0, fit), fit)
        visibleStartNanos = anchor - x * nanosPerPixel
        clamp()
    }

    fun pan(pixels: Int) {
        visibleStartNanos -= pixels * nanosPerPixel
        clamp()
    }

    fun clamp() {
        visibleStartNanos = visibleStartNanos.coerceIn(0.0, (durationNanos - width * nanosPerPixel).coerceAtLeast(0.0))
    }

    fun rulerTicks(targetLabelPixels: Double): Sequence<RulerTick> = sequence {
        if (durationNanos <= 0L) return@sequence
        val majorStep = NICE_STEPS_NANOS.firstOrNull { it >= nanosPerPixel * targetLabelPixels } ?: NICE_STEPS_NANOS.last()
        val minorStep = majorStep / MINOR_DIVISIONS
        var time = ceil(visibleStartNanos / minorStep).toLong() * minorStep
        val visibleEnd = visibleStartNanos + width * nanosPerPixel
        while (time.toDouble() <= visibleEnd && time <= durationNanos) {
            yield(RulerTick(time, timeToX(time), time % majorStep == 0L))
            time += minorStep
        }
    }

    private companion object {
        const val MINOR_DIVISIONS = 5L
        val NICE_STEPS_NANOS = longArrayOf(
            50_000_000L, 100_000_000L, 250_000_000L, 500_000_000L,
            1_000_000_000L, 2_000_000_000L, 5_000_000_000L, 10_000_000_000L,
            30_000_000_000L, 60_000_000_000L, 120_000_000_000L, 300_000_000_000L,
            600_000_000_000L, 1_800_000_000_000L, 3_600_000_000_000L,
        )
    }
}
