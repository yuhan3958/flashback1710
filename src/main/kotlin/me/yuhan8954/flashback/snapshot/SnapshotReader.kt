package me.yuhan8954.flashback.snapshot

import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayInputStream
import java.io.DataInput

object SnapshotReader {

    private const val BLOCKS_PER_CHUNK =
        16 * 16 * 256

    private const val BIOMES_PER_CHUNK =
        16 * 16

    private const val MAX_CHUNKS = 16384
    private const val MAX_TILE_ENTITIES = 1_000_000
    private const val MAX_ENTITIES = 1_000_000
    private const val MAX_NBT_BYTES = 64 * 1024 * 1024

    fun read(input: DataInput): ReplaySnapshot {
        val dimensionId =
            input.readInt()

        val seed =
            input.readLong()

        val worldTime =
            input.readLong()

        val totalWorldTime =
            input.readLong()

        val raining =
            input.readBoolean()

        val thundering =
            input.readBoolean()

        val rainStrength =
            input.readFloat()

        val thunderStrength =
            input.readFloat()

        val player =
            readPlayer(
                input,
            )

        val chunkCount =
            input.readInt()

        require(
            chunkCount in 0..MAX_CHUNKS,
        ) {
            "Invalid snapshot chunk count: $chunkCount"
        }

        val chunks =
            List(
                chunkCount,
            ) {
                readChunk(
                    input,
                )
            }

        val tileEntityCount =
            input.readInt()

        require(
            tileEntityCount in
                0..MAX_TILE_ENTITIES,
        ) {
            "Invalid snapshot tile entity count: " +
                tileEntityCount
        }

        val tileEntities =
            List(
                tileEntityCount,
            ) {
                ReplayTileEntitySnapshot(
                    x = input.readInt(),
                    y = input.readInt(),
                    z = input.readInt(),
                    nbt = readNbt(
                        input,
                    ),
                )
            }

        val entityCount =
            input.readInt()

        require(
            entityCount in 0..MAX_ENTITIES,
        ) {
            "Invalid snapshot entity count: " +
                entityCount
        }

        val entities =
            List(
                entityCount,
            ) {
                readEntity(
                    input,
                )
            }

        return ReplaySnapshot(
            dimensionId = dimensionId,
            seed = seed,
            worldTime = worldTime,
            totalWorldTime = totalWorldTime,
            raining = raining,
            thundering = thundering,
            rainStrength = rainStrength,
            thunderStrength = thunderStrength,
            player = player,
            chunks = chunks,
            tileEntities = tileEntities,
            entities = entities,
        )
    }

    private fun readPlayer(
        input: DataInput,
    ): ReplayPlayerSnapshot = ReplayPlayerSnapshot(
        entityId = input.readInt(),
        x = input.readDouble(),
        y = input.readDouble(),
        z = input.readDouble(),
        yaw = input.readFloat(),
        pitch = input.readFloat(),
        motionX = input.readDouble(),
        motionY = input.readDouble(),
        motionZ = input.readDouble(),
    )

    private fun readChunk(
        input: DataInput,
    ): ReplayChunkSnapshot {
        val chunkX =
            input.readInt()

        val chunkZ =
            input.readInt()

        val runCount =
            input.readInt()

        require(
            runCount in 1..BLOCKS_PER_CHUNK,
        ) {
            "Invalid snapshot block run count: $runCount"
        }

        val blockIds =
            IntArray(
                BLOCKS_PER_CHUNK,
            )

        val metadata =
            ByteArray(
                BLOCKS_PER_CHUNK,
            )

        var blockIndex = 0

        repeat(
            runCount,
        ) {
            val blockId =
                input.readInt()

            val blockMetadata =
                input.readByte()

            val runLength =
                input.readInt()

            require(
                runLength > 0 &&
                    blockIndex + runLength <=
                    BLOCKS_PER_CHUNK,
            ) {
                "Invalid snapshot block run length: " +
                    runLength
            }

            for (
            index in
            blockIndex until
                blockIndex + runLength
            ) {
                blockIds[index] =
                    blockId

                metadata[index] =
                    blockMetadata
            }

            blockIndex +=
                runLength
        }

        require(
            blockIndex == BLOCKS_PER_CHUNK,
        ) {
            "Incomplete snapshot chunk block data"
        }

        val biomeCount =
            input.readInt()

        require(
            biomeCount == BIOMES_PER_CHUNK,
        ) {
            "Invalid snapshot biome count: $biomeCount"
        }

        val biomes =
            ByteArray(
                biomeCount,
            )

        input.readFully(
            biomes,
        )

        return ReplayChunkSnapshot(
            chunkX = chunkX,
            chunkZ = chunkZ,
            blockIds = blockIds,
            metadata = metadata,
            biomes = biomes,
        )
    }

    private fun readEntity(
        input: DataInput,
    ): ReplayEntitySnapshot {
        val entityId =
            input.readInt()

        val entityType =
            if (
                input.readBoolean()
            ) {
                input.readUTF()
            } else {
                null
            }

        return ReplayEntitySnapshot(
            entityId = entityId,
            entityType = entityType,
            entityClass = input.readUTF(),
            playerProfileId = readNullableString(
                input,
            ),
            playerProfileName = readNullableString(
                input,
            ),
            serverPosX = input.readInt(),
            serverPosY = input.readInt(),
            serverPosZ = input.readInt(),
            x = input.readDouble(),
            y = input.readDouble(),
            z = input.readDouble(),
            yaw = input.readFloat(),
            pitch = input.readFloat(),
            motionX = input.readDouble(),
            motionY = input.readDouble(),
            motionZ = input.readDouble(),
            nbt = readNbt(
                input,
            ),
        )
    }

    private fun readNullableString(input: DataInput): String? = if (
        input.readBoolean()
    ) {
        input.readUTF()
    } else {
        null
    }

    private fun readNbt(
        input: DataInput,
    ): NBTTagCompound {
        val length =
            input.readInt()

        require(
            length in 1..MAX_NBT_BYTES,
        ) {
            "Invalid snapshot NBT length: $length"
        }

        val bytes =
            ByteArray(
                length,
            )

        input.readFully(
            bytes,
        )

        return CompressedStreamTools.readCompressed(
            ByteArrayInputStream(
                bytes,
            ),
        )
    }
}
