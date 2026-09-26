package me.yuhan8954.flashback.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplayMutationTimelineTest {

    @Test
    fun `remove after returns newest mutations first`() {
        val timeline =
            ReplayMutationTimeline<String>(
                historyDurationNanos =
                30L,
            )

        timeline.start(
            0L,
        )
        timeline.append(
            10L,
            "a",
        )
        timeline.append(
            20L,
            "b",
        )
        timeline.append(
            30L,
            "c",
        )

        assertEquals(
            listOf(
                "c",
                "b",
            ),
            timeline.removeAfter(
                10L,
            ),
        )
    }

    @Test
    fun `thirty second history boundary remains reversible`() {
        val timeline =
            ReplayMutationTimeline<String>(
                historyDurationNanos =
                30_000_000_000L,
            )

        timeline.start(
            0L,
        )
        timeline.advanceTo(
            60_000_000_000L,
        )

        val coverage =
            requireNotNull(
                timeline.coverage,
            )

        assertEquals(
            30_000_000_000L,
            coverage.startTimeNanos,
        )
        assertEquals(
            60_000_000_000L,
            coverage.endTimeNanos,
        )
        assertTrue(
            coverage.contains(
                30_000_000_000L,
            ),
        )
        assertFalse(
            coverage.contains(
                29_999_999_999L,
            ),
        )
    }

    @Test
    fun `trim drops mutations older than retained window`() {
        val timeline =
            ReplayMutationTimeline<String>(
                historyDurationNanos =
                30L,
            )

        timeline.start(
            0L,
        )
        timeline.append(
            10L,
            "old",
        )
        timeline.append(
            35L,
            "middle",
        )
        timeline.append(
            40L,
            "new",
        )
        timeline.advanceTo(
            50L,
        )

        assertEquals(
            listOf(
                "new",
                "middle",
            ),
            timeline.removeAfter(
                20L,
            ),
        )
    }

    @Test
    fun `timeline without mutations still exposes retained reverse window`() {
        val timeline =
            ReplayMutationTimeline<String>(
                historyDurationNanos =
                30L,
            )

        timeline.start(
            100L,
        )
        timeline.advanceTo(
            120L,
        )

        assertEquals(
            ReplayReverseCoverage(
                startTimeNanos = 100L,
                endTimeNanos = 120L,
            ),
            timeline.coverage,
        )
    }
}
