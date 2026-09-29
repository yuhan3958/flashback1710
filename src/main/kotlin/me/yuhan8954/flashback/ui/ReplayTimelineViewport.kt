package me.yuhan8954.flashback.ui

import kotlin.math.roundToInt
import kotlin.math.roundToLong

class ReplayTimelineViewport {
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
}
