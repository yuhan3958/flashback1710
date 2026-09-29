package me.yuhan8954.flashback.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplayTimelineViewportTest {
    @Test
    fun rulerTicksUseViewportScaleAndDuration() {
        val viewport = ReplayTimelineViewport()
        viewport.fit(1_000_000_000L, 100)
        val ticks = viewport.rulerTicks(72.0).toList()
        assertEquals(0L, ticks.first().timeNanos)
        assertTrue(ticks.first().major)
        assertEquals(0, ticks.first().x)
        assertEquals(1_000_000_000L, ticks.last().timeNanos)
        assertEquals(100, ticks.last().x)
    }

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
