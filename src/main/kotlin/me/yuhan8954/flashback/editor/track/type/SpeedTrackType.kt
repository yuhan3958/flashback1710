package me.yuhan8954.flashback.editor.track.type

import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayInterpolationMath
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrackType
import java.io.DataInput
import java.io.DataOutput

object SpeedTrackType : ReplayTrackType<Float> {
    override val id = "speed"

    override fun validate(value: Float) {
        require(value.isFinite() && value in -8.0f..8.0f) { "Speed must be finite and between -8 and 8" }
    }

    override fun evaluate(keyframes: List<ReplayKeyframe<Float>>, beforeIndex: Int, timestampNanos: Long): Float? {
        if (keyframes.isEmpty()) return null
        val before = keyframes[beforeIndex]
        if (timestampNanos <= before.timestampNanos || beforeIndex == keyframes.lastIndex) return before.value
        val after = keyframes[beforeIndex + 1]
        if (timestampNanos >= after.timestampNanos) return after.value
        if (before.interpolation == ReplayInterpolation.HOLD) return before.value
        val fraction = (timestampNanos - before.timestampNanos).toDouble() / (after.timestampNanos - before.timestampNanos).toDouble()
        val result = if (before.interpolation == ReplayInterpolation.SMOOTH) {
            val previous = keyframes.getOrNull(beforeIndex - 1) ?: before
            val next = keyframes.getOrNull(beforeIndex + 2) ?: after
            ReplayInterpolationMath.smooth(
                previous.value.toDouble(), before.value.toDouble(), after.value.toDouble(), next.value.toDouble(),
                previous.timestampNanos, before.timestampNanos, after.timestampNanos, next.timestampNanos, timestampNanos,
            )
        } else {
            ReplayInterpolationMath.lerp(before.value.toDouble(), after.value.toDouble(), fraction)
        }
        return result.coerceIn(-8.0, 8.0).toFloat()
    }

    override fun writeValue(output: DataOutput, value: Float) = output.writeFloat(value)

    override fun readValue(input: DataInput): Float = input.readFloat()
}
