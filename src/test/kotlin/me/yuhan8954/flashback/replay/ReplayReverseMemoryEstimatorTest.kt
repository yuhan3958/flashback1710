package me.yuhan8954.flashback.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplayReverseMemoryEstimatorTest {

    @Test
    fun `thirty second transform history has bounded estimate`() {
        val frames =
            30 *
                20 +
                1

        val entitiesPerFrame =
            100

        val estimatedBytes =
            ReplayReverseMemoryEstimator.estimate(
                frameCount =
                frames,
                entityTransformCount =
                frames *
                    entitiesPerFrame,
            )

        assertTrue(
            estimatedBytes <
                16L *
                1024L *
                1024L,
        )
    }

    @Test
    fun `memory estimate grows linearly with retained transforms`() {
        val oneEntity =
            ReplayReverseMemoryEstimator.estimate(
                frameCount = 1,
                entityTransformCount = 1,
            )

        val twoEntities =
            ReplayReverseMemoryEstimator.estimate(
                frameCount = 1,
                entityTransformCount = 2,
            )

        assertEquals(
            128L,
            twoEntities -
                oneEntity,
        )
    }
}
