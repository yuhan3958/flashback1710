package me.yuhan8954.flashback.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplayReversePlannerTest {

    @Test
    fun `coverage includes exact thirty second history boundary`() {
        val coverage =
            ReplayReverseCoverage(
                startTimeNanos = 10_000_000_000L,
                endTimeNanos = 40_000_000_000L,
            )

        assertTrue(
            coverage.contains(
                10_000_000_000L,
            ),
        )
        assertTrue(
            coverage.contains(
                40_000_000_000L,
            ),
        )
        assertFalse(
            coverage.contains(
                9_999_999_999L,
            ),
        )
    }

    @Test
    fun `in place reverse requires both journal and frame coverage`() {
        val journal =
            ReplayReverseCoverage(
                startTimeNanos = 10L,
                endTimeNanos = 100L,
            )

        val frames =
            ReplayReverseCoverage(
                startTimeNanos = 20L,
                endTimeNanos = 100L,
            )

        assertEquals(
            ReplayReversePath.IN_PLACE,
            ReplayReversePlanner.choose(
                50L,
                journal,
                frames,
            ),
        )

        assertEquals(
            ReplayReversePath.REBUILD,
            ReplayReversePlanner.choose(
                15L,
                journal,
                frames,
            ),
        )
    }

    @Test
    fun `trimmed journal forces fallback rebuild even with frames`() {
        val journal =
            ReplayReverseCoverage(
                startTimeNanos = 30_000_000_000L,
                endTimeNanos = 60_000_000_000L,
            )

        val frames =
            ReplayReverseCoverage(
                startTimeNanos = 20_000_000_000L,
                endTimeNanos = 60_000_000_000L,
            )

        assertEquals(
            ReplayReversePath.REBUILD,
            ReplayReversePlanner.choose(
                29_999_999_999L,
                journal,
                frames,
            ),
        )

        assertEquals(
            ReplayReversePath.IN_PLACE,
            ReplayReversePlanner.choose(
                30_000_000_000L,
                journal,
                frames,
            ),
        )
    }

    @Test
    fun `seek with no retained reverse coverage rebuilds`() {
        assertEquals(
            ReplayReversePath.REBUILD,
            ReplayReversePlanner.choose(
                1_000L,
                mutationCoverage = null,
                frameCoverage =
                ReplayReverseCoverage(
                    startTimeNanos = 0L,
                    endTimeNanos = 2_000L,
                ),
            ),
        )

        assertEquals(
            ReplayReversePath.REBUILD,
            ReplayReversePlanner.choose(
                1_000L,
                mutationCoverage =
                ReplayReverseCoverage(
                    startTimeNanos = 0L,
                    endTimeNanos = 2_000L,
                ),
                frameCoverage = null,
            ),
        )
    }
}
