package me.yuhan8954.flashback.editor.track.type

import me.yuhan8954.flashback.editor.ReplayCameraOrbit
import me.yuhan8954.flashback.editor.ReplayCameraOrbitMath
import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayInterpolationMath
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrackType
import java.io.DataInput
import java.io.DataOutput

object CameraOrbitTrackType : ReplayTrackType<ReplayCameraOrbit> {
    override val id = "camera_orbit"

    override fun validate(value: ReplayCameraOrbit) {
        require(value.centerX.isFinite() && value.centerY.isFinite() && value.centerZ.isFinite())
        require(value.distance.isFinite() && value.distance >= ReplayCameraOrbitMath.MIN_DISTANCE)
        require(value.yaw.isFinite() && value.pitch.isFinite() && value.pitch in -ReplayCameraOrbitMath.MAX_PITCH..ReplayCameraOrbitMath.MAX_PITCH)
    }

    override fun evaluate(keyframes: List<ReplayKeyframe<ReplayCameraOrbit>>, beforeIndex: Int, timestampNanos: Long): ReplayCameraOrbit? {
        if (keyframes.isEmpty()) return null
        val before = keyframes[beforeIndex]
        if (timestampNanos <= before.timestampNanos || beforeIndex == keyframes.lastIndex) return before.value
        val after = keyframes[beforeIndex + 1]
        if (timestampNanos >= after.timestampNanos) return after.value
        if (before.interpolation == ReplayInterpolation.HOLD) return before.value
        val fraction = (timestampNanos - before.timestampNanos).toDouble() / (after.timestampNanos - before.timestampNanos)
        val previous = keyframes.getOrNull(beforeIndex - 1) ?: before
        val next = keyframes.getOrNull(beforeIndex + 2) ?: after
        fun component(previousValue: Double, start: Double, end: Double, nextValue: Double): Double =
            if (before.interpolation == ReplayInterpolation.SMOOTH) {
                ReplayInterpolationMath.smooth(previousValue, start, end, nextValue,
                    previous.timestampNanos, before.timestampNanos, after.timestampNanos, next.timestampNanos, timestampNanos)
            } else ReplayInterpolationMath.lerp(start, end, fraction)
        val a = before.value
        val b = after.value
        return ReplayCameraOrbit(
            component(previous.value.centerX, a.centerX, b.centerX, next.value.centerX),
            component(previous.value.centerY, a.centerY, b.centerY, next.value.centerY),
            component(previous.value.centerZ, a.centerZ, b.centerZ, next.value.centerZ),
            component(previous.value.distance, a.distance, b.distance, next.value.distance).coerceAtLeast(ReplayCameraOrbitMath.MIN_DISTANCE),
            component(previous.value.yaw.toDouble(), a.yaw.toDouble(), b.yaw.toDouble(), next.value.yaw.toDouble()).toFloat(),
            component(previous.value.pitch.toDouble(), a.pitch.toDouble(), b.pitch.toDouble(), next.value.pitch.toDouble())
                .coerceIn(-ReplayCameraOrbitMath.MAX_PITCH.toDouble(), ReplayCameraOrbitMath.MAX_PITCH.toDouble()).toFloat(),
        )
    }

    override fun writeValue(output: DataOutput, value: ReplayCameraOrbit) {
        output.writeDouble(value.centerX)
        output.writeDouble(value.centerY)
        output.writeDouble(value.centerZ)
        output.writeDouble(value.distance)
        output.writeFloat(value.yaw)
        output.writeFloat(value.pitch)
    }

    override fun readValue(input: DataInput): ReplayCameraOrbit = ReplayCameraOrbit(
        input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble(), input.readFloat(), input.readFloat(),
    )
}
