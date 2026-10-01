package me.yuhan8954.flashback.editor.track

object ReplayInterpolationMath {
    fun lerp(a: Double, b: Double, fraction: Double): Double = a + (b - a) * fraction.coerceIn(0.0, 1.0)

    fun unwrap(reference: Double, angle: Double): Double {
        var difference = (angle - reference) % 360.0
        if (difference < -180.0) difference += 360.0
        if (difference >= 180.0) difference -= 360.0
        return reference + difference
    }

    fun lerpAngle(a: Double, b: Double, fraction: Double): Double = lerp(a, unwrap(a, b), fraction)

    /** Time-aware cubic Hermite interpolation with monotone tangents and bounded segment output. */
    fun smooth(
        previous: Double,
        start: Double,
        end: Double,
        next: Double,
        previousTime: Long,
        startTime: Long,
        endTime: Long,
        nextTime: Long,
        timestamp: Long,
    ): Double {
        val duration = (endTime - startTime).toDouble()
        val fraction = ((timestamp - startTime).toDouble() / duration).coerceIn(0.0, 1.0)
        val slope = (end - start) / duration
        val left = if (previousTime < startTime) {
            tangent((start - previous) / (startTime - previousTime).toDouble(), slope)
        } else {
            slope
        }
        val right = if (nextTime > endTime) {
            tangent(slope, (next - end) / (nextTime - endTime).toDouble())
        } else {
            slope
        }
        val t2 = fraction * fraction
        val t3 = t2 * fraction
        val result = (2 * t3 - 3 * t2 + 1) * start + (t3 - 2 * t2 + fraction) * duration * left +
            (-2 * t3 + 3 * t2) * end + (t3 - t2) * duration * right
        return if (result.isFinite()) result.coerceIn(minOf(start, end), maxOf(start, end)) else lerp(start, end, fraction)
    }

    private fun tangent(left: Double, right: Double): Double {
        if (left * right <= 0.0) return 0.0
        return 2.0 * left * right / (left + right)
    }
}
