package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayInterpolationMath
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrack
import me.yuhan8954.flashback.editor.track.type.CameraTrackType
import me.yuhan8954.flashback.editor.track.type.FovTrackType
import me.yuhan8954.flashback.editor.track.type.SpeedTrackType
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReplayInterpolationTest {
    @Test
    fun primitivesAndLegacyIds() {
        assertEquals(5.0, ReplayInterpolationMath.lerp(0.0, 10.0, 0.5))
        assertEquals(5.0, ReplayInterpolationMath.smooth(0.0, 0.0, 10.0, 10.0, 0, 0, 10, 10, 5))
        assertTrue(abs(ReplayInterpolationMath.lerpAngle(179.0, -179.0, 0.5)) == 180.0)
        assertEquals(ReplayInterpolation.LINEAR, ReplayInterpolation.fromSerializedId("LINEAR"))
        assertEquals(ReplayInterpolation.SMOOTH, ReplayInterpolation.fromSerializedId("smooth"))
        assertFailsWith<IllegalArgumentException> { ReplayInterpolation.fromSerializedId("other") }
    }

    @Test
    fun cameraModesAndUnevenTimes() {
        val track = ReplayTrack(CameraTrackType.id, CameraTrackType)
        val times = listOf(0L, 1_000_000_000L, 4_000_000_000L, 10_000_000_000L)
        val yaws = listOf(170f, 179f, -179f, -170f)
        times.forEachIndexed { index, time ->
            track.put(ReplayKeyframe(time, ReplayCameraPose(index * 10.0, 0.0, 0.0, yaws[index], 0f), ReplayInterpolation.SMOOTH))
        }
        times.forEachIndexed { index, time -> assertEquals(index * 10.0, track.evaluate(time)!!.x) }
        for (time in 1_000_000_000L..4_000_000_000L step 250_000_000L) {
            val pose = track.evaluate(time)!!
            assertTrue(pose.x.isFinite() && pose.x in 10.0..20.0)
            assertTrue(abs(pose.yaw) in 179f..181f)
        }
        track.updateInterpolation(times[1], ReplayInterpolation.HOLD)
        assertEquals(10.0, track.evaluate(2_000_000_000L)!!.x)
        assertEquals(20.0, track.evaluate(times[2])!!.x)
        track.updateInterpolation(times[1], ReplayInterpolation.LINEAR)
        assertEquals(15.0, track.evaluate(2_500_000_000L)!!.x)
        assertTrue(abs(track.evaluate(2_500_000_000L)!!.yaw) == 180f)
        track.updateInterpolation(times[1], ReplayInterpolation.SMOOTH)
        assertTrue(track.evaluate(2_500_000_000L)!!.x.isFinite())
    }

    @Test
    fun smoothCameraReducesVelocityJumpAtInteriorKey() {
        val track = ReplayTrack(CameraTrackType.id, CameraTrackType)
        listOf(0L to 0.0, 1_000_000_000L to 10.0, 4_000_000_000L to 20.0, 10_000_000_000L to 30.0).forEach {
            track.put(ReplayKeyframe(it.first, ReplayCameraPose(it.second, 0.0, 0.0, 0f, 0f)))
        }
        val keyTime = 1_000_000_000L
        val step = 1_000_000L
        val linearLeft = (track.evaluate(keyTime)!!.x - track.evaluate(keyTime - step)!!.x) / step
        val linearRight = (track.evaluate(keyTime + step)!!.x - track.evaluate(keyTime)!!.x) / step
        track.updateInterpolation(0L, ReplayInterpolation.SMOOTH)
        track.updateInterpolation(keyTime, ReplayInterpolation.SMOOTH)
        val smoothLeft = (track.evaluate(keyTime)!!.x - track.evaluate(keyTime - step)!!.x) / step
        val smoothRight = (track.evaluate(keyTime + step)!!.x - track.evaluate(keyTime)!!.x) / step
        assertTrue(abs(smoothLeft - smoothRight) < abs(linearLeft - linearRight) * 0.01)
    }

    @Test
    fun fovAndSpeedModesStayBounded() {
        val fov = ReplayTrack(FovTrackType.id, FovTrackType)
        fov.put(ReplayKeyframe(0L, 70f, ReplayInterpolation.HOLD))
        fov.put(ReplayKeyframe(10L, 30f))
        assertEquals(70f, fov.evaluate(5L))
        assertEquals(30f, fov.evaluate(10L))
        fov.updateInterpolation(0L, ReplayInterpolation.LINEAR)
        assertEquals(50f, fov.evaluate(5L))
        fov.updateInterpolation(0L, ReplayInterpolation.SMOOTH)
        assertTrue(fov.evaluate(5L)!! in 30f..70f)

        val speed = ReplayTrack(SpeedTrackType.id, SpeedTrackType)
        speed.put(ReplayKeyframe(0L, 1f, ReplayInterpolation.HOLD))
        speed.put(ReplayKeyframe(10L, 0f, ReplayInterpolation.LINEAR))
        speed.put(ReplayKeyframe(20L, -1f))
        assertEquals(1f, speed.evaluate(5L))
        assertEquals(0f, speed.evaluate(10L))
        assertEquals(-0.5f, speed.evaluate(15L))
        speed.updateInterpolation(0L, ReplayInterpolation.SMOOTH)
        speed.updateInterpolation(10L, ReplayInterpolation.SMOOTH)
        (0L..20L).forEach { assertTrue(speed.evaluate(it)!!.isFinite() && speed.evaluate(it)!! in -8f..8f) }
    }

    @Test
    fun valueEditsPreserveSelectedInterpolation() {
        val editor = ReplayEditorState(emptyList(), emptyList())
        editor.addCameraKeyframe(ReplayCameraKeyframe(10L, 1.0, 2.0, 3.0, 4f, 5f))
        assertTrue(editor.updateSelectedInterpolation(ReplayInterpolation.SMOOTH))
        editor.addCameraKeyframe(ReplayCameraKeyframe(10L, 6.0, 2.0, 3.0, 4f, 5f))
        assertEquals(ReplayInterpolation.SMOOTH, editor.selectedInterpolation())
        assertEquals(6.0, editor.selectedCameraKeyframe()?.x)
        editor.addFovKeyframe(10L, 70f)
        editor.updateSelectedInterpolation(ReplayInterpolation.HOLD)
        editor.setSelectedFloatValue(30f)
        assertEquals(ReplayInterpolation.HOLD, editor.selectedInterpolation())
        editor.addSpeedKeyframe(10L, 1f)
        editor.updateSelectedInterpolation(ReplayInterpolation.SMOOTH)
        editor.setSelectedFloatValue(0f)
        assertEquals(ReplayInterpolation.SMOOTH, editor.selectedInterpolation())
    }
}
