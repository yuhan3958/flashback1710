package me.yuhan8954.flashback.editor.track.type

import me.yuhan8954.flashback.editor.ReplayCameraPose
import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayInterpolationMath
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrackType
import java.io.DataInput
import java.io.DataOutput

object CameraTrackType : ReplayTrackType<ReplayCameraPose> {
    override val id = "camera"

    override fun evaluate(keyframes: List<ReplayKeyframe<ReplayCameraPose>>, beforeIndex: Int, timestampNanos: Long): ReplayCameraPose? {
        if (keyframes.isEmpty()) return null
        val before = keyframes[beforeIndex]
        if (timestampNanos <= before.timestampNanos || beforeIndex == keyframes.lastIndex) return before.value
        val after = keyframes[beforeIndex + 1]
        if (timestampNanos >= after.timestampNanos) return after.value
        if (before.interpolation == ReplayInterpolation.HOLD) return before.value
        val fraction = (timestampNanos - before.timestampNanos).toDouble() / (after.timestampNanos - before.timestampNanos).toDouble()
        val a = before.value
        val b = after.value
        if (before.interpolation == ReplayInterpolation.LINEAR) {
            return ReplayCameraPose(
                ReplayInterpolationMath.lerp(a.x, b.x, fraction),
                ReplayInterpolationMath.lerp(a.y, b.y, fraction),
                ReplayInterpolationMath.lerp(a.z, b.z, fraction),
                ReplayInterpolationMath.lerpAngle(a.yaw.toDouble(), b.yaw.toDouble(), fraction).toFloat(),
                ReplayInterpolationMath.lerp(a.pitch.toDouble(), b.pitch.toDouble(), fraction).toFloat(),
            )
        }
        val previous = keyframes.getOrNull(beforeIndex - 1) ?: before
        val next = keyframes.getOrNull(beforeIndex + 2) ?: after
        fun curve(p: Double, start: Double, end: Double, n: Double) = ReplayInterpolationMath.smooth(
            p, start, end, n, previous.timestampNanos, before.timestampNanos, after.timestampNanos, next.timestampNanos, timestampNanos,
        )
        val yawStart = a.yaw.toDouble()
        val yawEnd = ReplayInterpolationMath.unwrap(yawStart, b.yaw.toDouble())
        val yawPrevious = ReplayInterpolationMath.unwrap(yawStart, previous.value.yaw.toDouble())
        val yawNext = ReplayInterpolationMath.unwrap(yawEnd, next.value.yaw.toDouble())
        return ReplayCameraPose(
            curve(previous.value.x, a.x, b.x, next.value.x),
            curve(previous.value.y, a.y, b.y, next.value.y),
            curve(previous.value.z, a.z, b.z, next.value.z),
            curve(yawPrevious, yawStart, yawEnd, yawNext).toFloat(),
            curve(previous.value.pitch.toDouble(), a.pitch.toDouble(), b.pitch.toDouble(), next.value.pitch.toDouble()).toFloat(),
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
}
