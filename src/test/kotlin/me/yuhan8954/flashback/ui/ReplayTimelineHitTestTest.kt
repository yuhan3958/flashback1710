package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.editor.ReplayKeyframeSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayTimelineHitTestTest {
    @Test
    fun `drag target and pan state reset on release`() {
        val interaction = ReplayTimelineInteraction()
        interaction.pressKeyframe(true)
        assertTrue(interaction.draggingKeyframe())
        assertTrue(interaction.release(0))
        assertFalse(interaction.draggingKeyframe())
        interaction.pressKeyframe(false)
        assertFalse(interaction.draggingKeyframe())

        interaction.beginPan(20)
        assertTrue(interaction.panning)
        assertEquals(5, interaction.panDelta(25))
        assertEquals(-3, interaction.panDelta(22))
        assertTrue(interaction.release(2))
        assertFalse(interaction.panning)
        assertFalse(interaction.release(1))
    }

    @Test
    fun `row hit testing excludes ruler and marker row`() {
        assertNull(ReplayTimelineHitTest.keyframeTrackAt(17, 18, 14))
        assertEquals("camera", ReplayTimelineHitTest.keyframeTrackAt(18, 18, 14))
        assertEquals("fov", ReplayTimelineHitTest.keyframeTrackAt(32, 18, 14))
        assertEquals("speed", ReplayTimelineHitTest.keyframeTrackAt(46, 18, 14))
        assertEquals("time_of_day", ReplayTimelineHitTest.keyframeTrackAt(60, 18, 14))
        assertNull(ReplayTimelineHitTest.keyframeTrackAt(74, 18, 14))
        assertNull(ReplayTimelineHitTest.keyframeTrackAt(18, 18, 0))
    }

    @Test
    fun `identical times remain track specific`() {
        val transform = { time: Long -> time.toInt() }
        assertEquals(
            ReplayKeyframeSelection("camera", 10L),
            ReplayTimelineHitTest.nearestKeyframe("camera", listOf(10L), 11, 3, transform),
        )
        assertEquals(
            ReplayKeyframeSelection("fov", 10L),
            ReplayTimelineHitTest.nearestKeyframe("fov", listOf(10L), 11, 3, transform),
        )
        assertNull(ReplayTimelineHitTest.nearestKeyframe("speed", emptyList(), 11, 3, transform))
    }
}
