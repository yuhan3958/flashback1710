package me.yuhan8954.flashback.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import me.yuhan8954.flashback.snapshot.ReplayInventorySnapshot
import me.yuhan8954.flashback.snapshot.ReplayPlayerSnapshot
import me.yuhan8954.flashback.snapshot.ReplaySnapshot

class ReplaySegmentIndexTest {

    @Test
    fun findsSegmentContainingTargetTime() {
        val initial =
            snapshot(
                worldTime = 0L,
            )

        val first =
            snapshot(
                worldTime = 100L,
            )

        val second =
            snapshot(
                worldTime = 200L,
            )

        val index =
            ReplaySegmentIndex(
                initialSnapshot = initial,
                checkpoints =
                listOf(
                    ReplayCheckpoint.Full(
                        timestampNanos = 10L,
                        packetIndex = 3,
                        snapshot = first,
                    ),
                    ReplayCheckpoint.Full(
                        timestampNanos = 20L,
                        packetIndex = 7,
                        snapshot = second,
                    ),
                ),
                durationNanos = 30L,
            )

        assertEquals(
            0L,
            index.find(
                9L,
            ).startTimeNanos,
        )

        assertEquals(
            10L,
            index.find(
                10L,
            ).startTimeNanos,
        )

        assertEquals(
            10L,
            index.find(
                19L,
            ).startTimeNanos,
        )

        assertEquals(
            20L,
            index.find(
                30L,
            ).startTimeNanos,
        )
    }

    private fun snapshot(
        worldTime: Long,
    ): ReplaySnapshot =
        ReplaySnapshot(
            dimensionId = 0,
            seed = 0L,
            worldTime = worldTime,
            totalWorldTime = worldTime,
            raining = false,
            thundering = false,
            rainStrength = 0.0f,
            thunderStrength = 0.0f,
            player =
            ReplayPlayerSnapshot(
                entityId = 1,
                profileId = null,
                profileName = "Player",
                x = 0.0,
                y = 0.0,
                z = 0.0,
                yaw = 0.0f,
                pitch = 0.0f,
                motionX = 0.0,
                motionY = 0.0,
                motionZ = 0.0,
                inventory =
                ReplayInventorySnapshot(
                    selectedSlot = 0,
                    mainInventory = emptyList(),
                    armorInventory = emptyList(),
                ),
            ),
            chunks = emptyList(),
            tileEntities = emptyList(),
            entities = emptyList(),
        )
}
