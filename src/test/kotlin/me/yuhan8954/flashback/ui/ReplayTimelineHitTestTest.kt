package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.editor.ReplayKeyframeSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplayTimelineHitTestTest {
    @Test
    fun `row hit testing excludes ruler and marker row`() {
        assertNull(ReplayTimelineHitTest.keyframeTrackAt(17, 18, 14))
        assertEquals("camera", ReplayTimelineHitTest.keyframeTrackAt(18, 18, 14))
        assertEquals("fov", ReplayTimelineHitTest.keyframeTrackAt(32, 18, 14))
        assertEquals("speed", ReplayTimelineHitTest.keyframeTrackAt(46, 18, 14))
        assertNull(ReplayTimelineHitTest.keyframeTrackAt(60, 18, 14))
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
