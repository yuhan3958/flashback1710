package me.yuhan8954.flashback.editor.track.type

import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrackType
import java.io.DataInput
import java.io.DataOutput

object SpeedTrackType : ReplayTrackType<Float> {
    override val id = "speed"

    override fun validate(value: Float) {
        require(value.isFinite() && value in -8.0f..8.0f) { "Speed must be finite and between -8 and 8" }
    }

    override fun evaluate(keyframes: List<ReplayKeyframe<Float>>, timestampNanos: Long): Float? {
        if (keyframes.isEmpty()) return null
        val before = keyframes.lastOrNull { it.timestampNanos <= timestampNanos } ?: keyframes.first()
        val after = keyframes.firstOrNull { it.timestampNanos >= timestampNanos } ?: keyframes.last()
        if (before.timestampNanos == after.timestampNanos) return before.value
        val fraction = (timestampNanos - before.timestampNanos).toDouble() /
            (after.timestampNanos - before.timestampNanos).toDouble()
        return (before.value + (after.value - before.value) * fraction.coerceIn(0.0, 1.0)).toFloat()
    }

    override fun writeValue(output: DataOutput, value: Float) = output.writeFloat(value)

    override fun readValue(input: DataInput): Float = input.readFloat()
}
