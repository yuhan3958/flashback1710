package me.yuhan8954.flashback.snapshot

import net.minecraft.nbt.NBTTagCompound
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnapshotDeltaTest {

    @Test
    fun `create and apply reconstruct target snapshot`() {
        val previous =
            snapshot(
                worldTime = 10L,
                chunks = listOf(
                    chunk(
                        0,
                        0,
                        1,
                    ),
                    chunk(
                        1,
                        0,
                        2,
                    ),
                ),
                tileEntities = listOf(
                    tileEntity(
                        1,
                        64,
                        1,
                        10,
                    ),
                    tileEntity(
                        17,
                        64,
                        1,
                        20,
                    ),
                ),
                entities = listOf(
                    entity(
                        100,
                        1.0,
                    ),
                    entity(
                        200,
                        2.0,
                    ),
                ),
            )

        val target =
            snapshot(
                worldTime = 20L,
                chunks = listOf(
                    chunk(
                        0,
                        0,
                        3,
                    ),
                    chunk(
                        2,
                        0,
                        4,
                    ),
                ),
                tileEntities = listOf(
                    tileEntity(
                        1,
                        64,
                        1,
                        11,
                    ),
                    tileEntity(
                        33,
                        64,
                        1,
                        30,
                    ),
                ),
                entities = listOf(
                    entity(
                        100,
                        3.0,
                    ),
                    entity(
                        300,
                        4.0,
                    ),
                ),
            )

        val delta =
            SnapshotDelta.create(
                previous,
                target,
            )

        val applied =
            SnapshotDelta.apply(
                previous,
                delta,
            )

        assertSnapshotEquivalent(
            target,
            applied,
        )

        assertEquals(
            listOf(
                ReplayChunkPosition(
                    1,
                    0,
                ),
            ),
            delta.removedChunks,
        )
        assertEquals(
            listOf(
                ReplayBlockPosition(
                    17,
                    64,
                    1,
                ),
            ),
            delta.removedTileEntities,
        )
        assertContentEquals(
            intArrayOf(
                200,
            ),
            delta.removedEntityIds,
        )
    }

    @Test
    fun `unchanged collections produce an empty structural delta`() {
        val snapshot =
            snapshot(
                chunks = listOf(
                    chunk(
                        0,
                        0,
                        1,
                    ),
                ),
                tileEntities = listOf(
                    tileEntity(
                        1,
                        64,
                        1,
                        10,
                    ),
                ),
                entities = listOf(
                    entity(
                        100,
                        1.0,
                    ),
                ),
            )

        val delta =
            SnapshotDelta.create(
                snapshot,
                snapshot,
            )

        assertTrue(
            delta.snapshot.chunks.isEmpty(),
        )
        assertTrue(
            delta.snapshot.tileEntities.isEmpty(),
        )
        assertTrue(
            delta.snapshot.entities.isEmpty(),
        )
        assertTrue(
            delta.removedChunks.isEmpty(),
        )
        assertTrue(
            delta.removedTileEntities.isEmpty(),
        )
        assertContentEquals(
            intArrayOf(),
            delta.removedEntityIds,
        )
    }

    @Test
    fun `dimension change replaces all replay collections`() {
        val previous =
            snapshot(
                dimensionId = 0,
                chunks = listOf(
                    chunk(
                        0,
                        0,
                        1,
                    ),
                ),
                entities = listOf(
                    entity(
                        10,
                        1.0,
                    ),
                ),
            )

        val target =
            snapshot(
                dimensionId = 1,
                chunks = listOf(
                    chunk(
                        4,
                        4,
                        2,
                    ),
                ),
                entities = listOf(
                    entity(
                        20,
                        2.0,
                    ),
                ),
            )

        val delta =
            SnapshotDelta.create(
                previous,
                target,
            )

        assertEquals(
            1,
            delta.snapshot.chunks.size,
        )
        assertEquals(
            1,
            delta.snapshot.entities.size,
        )
        assertEquals(
            listOf(
                ReplayChunkPosition(
                    0,
                    0,
                ),
            ),
            delta.removedChunks,
        )
        assertContentEquals(
            intArrayOf(
                10,
            ),
            delta.removedEntityIds,
        )

        assertSnapshotEquivalent(
            target,
            SnapshotDelta.apply(
                previous,
                delta,
            ),
        )
    }

    private fun snapshot(
        dimensionId: Int = 0,
        worldTime: Long = 0L,
        chunks: List<ReplayChunkSnapshot> = emptyList(),
        tileEntities: List<ReplayTileEntitySnapshot> = emptyList(),
        entities: List<ReplayEntitySnapshot> = emptyList(),
    ): ReplaySnapshot = ReplaySnapshot(
        dimensionId = dimensionId,
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
        chunks = chunks,
        tileEntities = tileEntities,
        entities = entities,
    )

    private fun chunk(
        x: Int,
        z: Int,
        marker: Int,
    ): ReplayChunkSnapshot = ReplayChunkSnapshot(
        chunkX = x,
        chunkZ = z,
        blockIds = intArrayOf(
            marker,
            marker + 1,
        ),
        metadata = byteArrayOf(
            marker.toByte(),
        ),
        biomes = byteArrayOf(
            marker.toByte(),
        ),
    )

    private fun tileEntity(
        x: Int,
        y: Int,
        z: Int,
        value: Int,
    ): ReplayTileEntitySnapshot = ReplayTileEntitySnapshot(
        x = x,
        y = y,
        z = z,
        nbt = NBTTagCompound().apply {
            setInteger(
                "value",
                value,
            )
        },
    )

    private fun entity(
        id: Int,
        x: Double,
    ): ReplayEntitySnapshot = ReplayEntitySnapshot(
        entityId = id,
        entityType = "Pig",
        entityClass = "example.Entity",
        playerProfileId = null,
        playerProfileName = null,
        serverPosX = 0,
        serverPosY = 0,
        serverPosZ = 0,
        x = x,
        y = 64.0,
        z = 0.0,
        yaw = 0.0f,
        pitch = 0.0f,
        motionX = 0.0,
        motionY = 0.0,
        motionZ = 0.0,
        nbt = NBTTagCompound().apply {
            setInteger(
                "idValue",
                id,
            )
        },
    )

    private fun assertSnapshotEquivalent(
        expected: ReplaySnapshot,
        actual: ReplaySnapshot,
    ) {
        assertEquals(
            expected.dimensionId,
            actual.dimensionId,
        )
        assertEquals(
            expected.worldTime,
            actual.worldTime,
        )
        assertEquals(
            expected.chunks.size,
            actual.chunks.size,
        )
        expected.chunks.zip(
            actual.chunks,
        ).forEach {
                (
                    expectedChunk,
                    actualChunk,
                ),
            ->
            assertEquals(
                expectedChunk.chunkX,
                actualChunk.chunkX,
            )
            assertEquals(
                expectedChunk.chunkZ,
                actualChunk.chunkZ,
            )
            assertContentEquals(
                expectedChunk.blockIds,
                actualChunk.blockIds,
            )
            assertContentEquals(
                expectedChunk.metadata,
                actualChunk.metadata,
            )
            assertContentEquals(
                expectedChunk.biomes,
                actualChunk.biomes,
            )
        }
        assertEquals(
            expected.tileEntities,
            actual.tileEntities,
        )
        assertEquals(
            expected.entities,
            actual.entities,
        )
    }
}
