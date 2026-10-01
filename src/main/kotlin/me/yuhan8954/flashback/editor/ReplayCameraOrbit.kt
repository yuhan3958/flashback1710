package me.yuhan8954.flashback.editor

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

data class ReplayCameraOrbit(
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val distance: Double,
    val yaw: Float,
    val pitch: Float,
)

object ReplayCameraOrbitMath {
    const val MIN_DISTANCE = 0.01
    const val MAX_PITCH = 89.9f

    fun fromCameraLook(pose: ReplayCameraPose, distance: Double): ReplayCameraOrbit {
        require(distance.isFinite() && distance >= MIN_DISTANCE)
        val yaw = Math.toRadians(pose.yaw.toDouble())
        val safePitch = pose.pitch.coerceIn(-MAX_PITCH, MAX_PITCH)
        val pitch = Math.toRadians(safePitch.toDouble())
        val horizontal = cos(pitch) * distance
        return ReplayCameraOrbit(
            pose.x - sin(yaw) * horizontal,
            pose.y - sin(pitch) * distance,
            pose.z + cos(yaw) * horizontal,
            distance,
            pose.yaw,
            safePitch,
        )
    }

    fun toCameraPose(orbit: ReplayCameraOrbit): ReplayCameraPose {
        val yaw = Math.toRadians(orbit.yaw.toDouble())
        val pitch = Math.toRadians(orbit.pitch.toDouble())
        val horizontal = cos(pitch) * orbit.distance
        return ReplayCameraPose(
            orbit.centerX + sin(yaw) * horizontal,
            orbit.centerY + sin(pitch) * orbit.distance,
            orbit.centerZ - cos(yaw) * horizontal,
            orbit.yaw,
            orbit.pitch,
        )
    }

    fun fromCameraPose(pose: ReplayCameraPose, centerX: Double, centerY: Double, centerZ: Double, referenceYaw: Float = pose.yaw): ReplayCameraOrbit? {
        val dx = pose.x - centerX
        val dy = pose.y - centerY
        val dz = pose.z - centerZ
        val distance = hypot(hypot(dx, dz), dy)
        if (!distance.isFinite() || distance < MIN_DISTANCE) return null
        val yaw = Math.toDegrees(atan2(dx, -dz))
        val unwrappedYaw = yaw + round((referenceYaw - yaw) / 360.0) * 360.0
        val pitch = Math.toDegrees(atan2(dy, hypot(dx, dz))).coerceIn(-MAX_PITCH.toDouble(), MAX_PITCH.toDouble())
        return ReplayCameraOrbit(centerX, centerY, centerZ, distance, unwrappedYaw.toFloat(), pitch.toFloat())
    }
}
