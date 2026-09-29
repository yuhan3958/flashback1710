package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.editor.ReplayKeyframeSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplayTimelineHitTestTest {
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
