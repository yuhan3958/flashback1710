package me.yuhan8954.flashback.editor.track.type

import me.yuhan8954.flashback.editor.ReplayCameraPose
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrackType
import java.io.DataInput
import java.io.DataOutput
import kotlin.math.abs

object CameraTrackType : ReplayTrackType<ReplayCameraPose> {
    override val id = "camera"

    override fun evaluate(keyframes: List<ReplayKeyframe<ReplayCameraPose>>, timestampNanos: Long): ReplayCameraPose? {
        if (keyframes.isEmpty()) return null
        val before = keyframes.lastOrNull { it.timestampNanos <= timestampNanos } ?: keyframes.first()
        val after = keyframes.firstOrNull { it.timestampNanos >= timestampNanos } ?: keyframes.last()
        if (before.timestampNanos == after.timestampNanos) return before.value

        val fraction = (timestampNanos - before.timestampNanos).toDouble() /
            (after.timestampNanos - before.timestampNanos).toDouble()
        val older = before.value
        val newer = after.value
        return ReplayCameraPose(
            lerp(older.x, newer.x, fraction),
            lerp(older.y, newer.y, fraction),
            lerp(older.z, newer.z, fraction),
            lerpAngle(older.yaw, newer.yaw, fraction),
            lerp(older.pitch.toDouble(), newer.pitch.toDouble(), fraction).toFloat(),
        )
    }

    override fun writeValue(output: DataOutput, value: ReplayCameraPose) {
        output.writeDouble(value.x)
        output.writeDouble(value.y)
        output.writeDouble(value.z)
        output.writeFloat(value.yaw)
        output.writeFloat(value.pitch)
    }

    override fun readValue(input: DataInput): ReplayCameraPose = ReplayCameraPose(
        input.readDouble(),
        input.readDouble(),
        input.readDouble(),
        input.readFloat(),
        input.readFloat(),
    )

    private fun lerp(older: Double, newer: Double, fraction: Double): Double = older + (newer - older) * fraction.coerceIn(0.0, 1.0)

    private fun lerpAngle(older: Float, newer: Float, fraction: Double): Float {
        var difference = newer - older
        while (difference < -180.0f) difference += 360.0f
        while (difference >= 180.0f) difference -= 360.0f
        val result = older + difference * fraction.coerceIn(0.0, 1.0)
        return if (abs(result) < 0.000001) 0.0f else result.toFloat()
    }
}
