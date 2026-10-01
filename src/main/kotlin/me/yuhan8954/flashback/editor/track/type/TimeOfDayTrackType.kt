package me.yuhan8954.flashback.editor.track.type

import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayInterpolationMath
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrackType
import java.io.DataInput
import java.io.DataOutput
import kotlin.math.roundToInt

object TimeOfDayTrackType : ReplayTrackType<Int> {
    override val id = "time_of_day"
    const val DAY_TICKS = 24_000

    fun normalize(ticks: Int): Int = Math.floorMod(ticks, DAY_TICKS)

    override fun validate(value: Int) {
        require(value in 0 until DAY_TICKS) { "Time of day must be between 0 and 23999 ticks" }
    }

    override fun evaluate(keyframes: List<ReplayKeyframe<Int>>, beforeIndex: Int, timestampNanos: Long): Int? {
        if (keyframes.isEmpty()) return null
        val before = keyframes[beforeIndex]
        if (timestampNanos <= before.timestampNanos || beforeIndex == keyframes.lastIndex) return before.value
        val after = keyframes[beforeIndex + 1]
        if (timestampNanos >= after.timestampNanos) return after.value
        if (before.interpolation == ReplayInterpolation.HOLD) return before.value

        val start = before.value.toDouble()
        val end = unwrap(start, after.value)
        val fraction = (timestampNanos - before.timestampNanos).toDouble() /
            (after.timestampNanos - before.timestampNanos).toDouble()
        val result = if (before.interpolation == ReplayInterpolation.SMOOTH) {
            val previous = keyframes.getOrNull(beforeIndex - 1) ?: before
            val next = keyframes.getOrNull(beforeIndex + 2) ?: after
            ReplayInterpolationMath.smooth(
                unwrap(start, previous.value), start, end, unwrap(end, next.value),
                previous.timestampNanos, before.timestampNanos, after.timestampNanos, next.timestampNanos, timestampNanos,
            )
        } else {
            ReplayInterpolationMath.lerp(start, end, fraction)
        }
        return normalize(result.roundToInt())
    }

    private fun unwrap(reference: Double, ticks: Int): Double {
        var difference = (ticks - reference) % DAY_TICKS
        if (difference < -DAY_TICKS / 2) difference += DAY_TICKS
        if (difference > DAY_TICKS / 2) difference -= DAY_TICKS
        return reference + difference
    }

    override fun writeValue(output: DataOutput, value: Int) = output.writeInt(value)

    override fun readValue(input: DataInput): Int = input.readInt()
}
