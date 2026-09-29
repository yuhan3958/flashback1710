package me.yuhan8954.flashback.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplayTimelineViewportTest {
    @Test
    fun zoomKeepsCursorTimeAndPanClamps() {
        val viewport = ReplayTimelineViewport()
        viewport.fit(100_000_000_000L, 1000)
        val before = viewport.xToTime(400)
        viewport.zoom(400, 2.0)
        assertTrue(kotlin.math.abs(before - viewport.xToTime(400)) < 100_000_000L)
        viewport.pan(100_000)
        assertEquals(0.0, viewport.visibleStartNanos)
        viewport.pan(-100_000)
        assertTrue(viewport.xToTime(1000) <= 100_000_000_000L)
    }
}
