package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.type.SpeedTrackType
import me.yuhan8954.flashback.replay.ReplayClock
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpeedTrackTest {
    @Test
    fun evaluatesAndSelectsIndependently() {
        val editor = ReplayEditorState(emptyList(), emptyList())
        assertEquals(1.0f, editor.speedAt(0L))
        editor.addFovKeyframe(10L, 70.0f)
        editor.addCameraKeyframe(ReplayCameraKeyframe(10L, 0.0, 0.0, 0.0, 0.0f, 0.0f))
        assertTrue(editor.selectKeyframe("camera", 10L))
        assertEquals(ReplayKeyframeSelection("camera", 10L), editor.selectedKeyframe)
        assertTrue(editor.selectKeyframe("fov", 10L))
        assertEquals(ReplayKeyframeSelection("fov", 10L), editor.selectedKeyframe)
        editor.clearKeyframeSelection()
        assertEquals(null, editor.selectedKeyframe)
        editor.addSpeedKeyframe(0L, 1.0f)
        editor.addSpeedKeyframe(10L, -1.0f)
        assertEquals(0.0f, editor.speedAt(5L))
        assertEquals(1.0f, editor.speedAt(-1L))
        assertEquals(-1.0f, editor.speedAt(20L))
    }

    @Test
    fun rejectsInvalidValues() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 8.1f).forEach {
            assertFailsWith<IllegalArgumentException> { SpeedTrackType.validate(it) }
        }
    }

    @Test
    fun composesManualAndAutomatedSpeedWithoutPausingAtZero() {
        val now = AtomicLong(0L)
        val clock = ReplayClock(now::get)
        clock.setSpeed(2.0)
        clock.setSpeedAutomation { 0.25 }
        clock.resume()
        now.set(1_000_000_000L)
        clock.update()
        assertEquals(500_000_000L, clock.currentTimeNanos)
        clock.setSpeedAutomation { 0.0 }
        now.set(2_000_000_000L)
        clock.update()
        assertEquals(500_000_000L, clock.currentTimeNanos)
        assertEquals(false, clock.paused)
        clock.setSpeedAutomation { -1.0 }
        now.set(2_100_000_000L)
        clock.update()
        assertEquals(300_000_000L, clock.currentTimeNanos)
    }
}
