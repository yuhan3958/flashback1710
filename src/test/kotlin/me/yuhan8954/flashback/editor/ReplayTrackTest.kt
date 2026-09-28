package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrack
import me.yuhan8954.flashback.editor.track.type.CameraTrackType
import me.yuhan8954.flashback.editor.track.type.FovTrackType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayTrackTest {

    @Test
    fun `fov track interpolates clamps replaces and rejects invalid values`() {
        val track = ReplayTrack(FovTrackType.id, FovTrackType)
        assertNull(track.evaluate(50L))
        track.put(ReplayKeyframe(10L, 70.0f))
        assertEquals(70.0f, track.evaluate(0L))
        assertEquals(70.0f, track.evaluate(10L))
        assertEquals(70.0f, track.evaluate(100L))
        track.put(ReplayKeyframe(110L, 30.0f))
        assertEquals(50.0f, track.evaluate(60L))
        assertEquals(30.0f, track.evaluate(200L))
        track.put(ReplayKeyframe(110L, 50.0f))
        assertEquals(60.0f, track.evaluate(60L))
        assertEquals(2, track.size)
        assertFailsWith<IllegalArgumentException> { track.put(ReplayKeyframe(0L, Float.NaN)) }
        assertFailsWith<IllegalArgumentException> { track.put(ReplayKeyframe(0L, Float.POSITIVE_INFINITY)) }
        assertFailsWith<IllegalArgumentException> { track.put(ReplayKeyframe(0L, 180.0f)) }
        track.clear()
        assertNull(track.evaluate(60L))
    }
    private fun pose(x: Double, yaw: Float = 0.0f, pitch: Float = 0.0f) = ReplayCameraPose(x, x + 1.0, x + 2.0, yaw, pitch)

    @Test
    fun `empty and single camera tracks evaluate at every time`() {
        val track = ReplayTrack(CameraTrackType.id, CameraTrackType)
        assertNull(track.evaluate(0L))
        val value = pose(3.0)
        track.put(ReplayKeyframe(10L, value))
        assertEquals(value, track.evaluate(0L))
        assertEquals(value, track.evaluate(10L))
        assertEquals(value, track.evaluate(20L))
    }

    @Test
    fun `camera interpolation clamps and crosses yaw boundary by shortest path`() {
        val track = ReplayTrack(CameraTrackType.id, CameraTrackType)
        val first = pose(0.0, 179.0f, 10.0f)
        val last = pose(10.0, -179.0f, 30.0f)
        track.put(ReplayKeyframe(10L, first))
        track.put(ReplayKeyframe(110L, last))
        assertEquals(first, track.evaluate(0L))
        assertEquals(last, track.evaluate(120L))
        val middle = track.evaluate(60L)!!
        assertEquals(5.0, middle.x)
        assertEquals(6.0, middle.y)
        assertEquals(7.0, middle.z)
        assertEquals(20.0f, middle.pitch)
        assertTrue(kotlin.math.abs(middle.yaw) == 180.0f)
    }

    @Test
    fun `replacement move deletion and ordering are track operations`() {
        val track = ReplayTrack(CameraTrackType.id, CameraTrackType)
        track.put(ReplayKeyframe(30L, pose(3.0)))
        track.put(ReplayKeyframe(10L, pose(1.0)))
        track.put(ReplayKeyframe(20L, pose(2.0)))
        track.put(ReplayKeyframe(10L, pose(4.0)))
        assertEquals(listOf(10L, 20L, 30L), track.keyframes().map { it.timestampNanos })
        assertEquals(4.0, track.keyframeAt(10L)?.value?.x)
        assertTrue(track.move(10L, 20L))
        assertNull(track.keyframeAt(10L))
        assertEquals(4.0, track.keyframeAt(20L)?.value?.x)
        assertTrue(track.delete(20L))
        assertEquals(listOf(30L), track.keyframes().map { it.timestampNanos })
    }
}
