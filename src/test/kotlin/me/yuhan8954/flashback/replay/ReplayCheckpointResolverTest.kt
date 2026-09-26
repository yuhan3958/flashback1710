package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.snapshot.ReplayInventorySnapshot
import me.yuhan8954.flashback.snapshot.ReplayPlayerSnapshot
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotDelta
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayCheckpointResolverTest {

    @Test
    fun `resolve never applies checkpoints after target time`() {
        val initial =
            snapshot(
                0L,
            )

        val at100 =
            snapshot(
                100L,
            )

        val at200 =
            snapshot(
                200L,
            )

        val checkpoints =
            listOf(
                ReplayCheckpoint.Full(
                    timestampNanos = 100L,
                    packetIndex = 10,
                    snapshot = at100,
                ),
                ReplayCheckpoint.Delta(
                    timestampNanos = 200L,
                    packetIndex = 20,
                    delta = SnapshotDelta.create(
                        at100,
                        at200,
                    ),
                ),
            )

        val resolved =
            ReplayCheckpointResolver.resolve(
                initial,
                checkpoints,
                150L,
            )

        assertEquals(
            100L,
            resolved.timestampNanos,
        )
        assertEquals(
            10,
            resolved.packetIndex,
        )
        assertEquals(
            100L,
            resolved.snapshot.worldTime,
        )
    }

    @Test
    fun `resolve applies deltas after latest eligible full anchor`() {
        val initial =
            snapshot(
                0L,
            )

        val at100 =
            snapshot(
                100L,
            )

        val at200 =
            snapshot(
                200L,
            )

        val at300 =
            snapshot(
                300L,
            )

        val checkpoints =
            listOf(
                ReplayCheckpoint.Full(
                    timestampNanos = 100L,
                    packetIndex = 10,
                    snapshot = at100,
                ),
                ReplayCheckpoint.Delta(
                    timestampNanos = 200L,
                    packetIndex = 20,
                    delta = SnapshotDelta.create(
                        at100,
                        at200,
                    ),
                ),
                ReplayCheckpoint.Delta(
                    timestampNanos = 300L,
                    packetIndex = 30,
                    delta = SnapshotDelta.create(
                        at200,
                        at300,
                    ),
                ),
            )

        val resolved =
            ReplayCheckpointResolver.resolve(
                initial,
                checkpoints,
                300L,
            )

        assertEquals(
            300L,
            resolved.timestampNanos,
        )
        assertEquals(
            30,
            resolved.packetIndex,
        )
        assertEquals(
            300L,
            resolved.snapshot.worldTime,
        )
    }

    @Test
    fun `resolve falls back to initial snapshot before first anchor`() {
        val initial =
            snapshot(
                5L,
            )

        val resolved =
            ReplayCheckpointResolver.resolve(
                initial,
                listOf(
                    ReplayCheckpoint.Full(
                        timestampNanos = 100L,
                        packetIndex = 10,
                        snapshot = snapshot(
                            100L,
                        ),
                    ),
                ),
                50L,
            )

        assertEquals(
            0L,
            resolved.timestampNanos,
        )
        assertEquals(
            0,
            resolved.packetIndex,
        )
        assertEquals(
            5L,
            resolved.snapshot.worldTime,
        )
    }

    private fun snapshot(
        worldTime: Long,
    ): ReplaySnapshot = ReplaySnapshot(
        dimensionId = 0,
        seed = 123L,
        worldTime = worldTime,
        totalWorldTime = worldTime,
        raining = false,
        thundering = false,
        rainStrength = 0.0f,
        thunderStrength = 0.0f,
        player = ReplayPlayerSnapshot(
            entityId = 1,
            profileId = null,
            profileName = "Tester",
            x = 0.0,
            y = 64.0,
            z = 0.0,
            yaw = 0.0f,
            pitch = 0.0f,
            motionX = 0.0,
            motionY = 0.0,
            motionZ = 0.0,
            inventory = ReplayInventorySnapshot(
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
