package me.yuhan8954.flashback.snapshot

import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayOutputStream
import java.io.DataOutput

object SnapshotWriter {

    fun write(
        output: DataOutput,
        snapshot: ReplaySnapshot,
    ) {
        output.writeInt(
            snapshot.dimensionId,
        )

        output.writeLong(
            snapshot.seed,
        )

        output.writeLong(
            snapshot.worldTime,
        )

        output.writeLong(
            snapshot.totalWorldTime,
        )

        output.writeBoolean(
            snapshot.raining,
        )

        output.writeBoolean(
            snapshot.thundering,
        )

        output.writeFloat(
            snapshot.rainStrength,
        )

        output.writeFloat(
            snapshot.thunderStrength,
        )

        writePlayer(
            output,
            snapshot.player,
        )

        output.writeInt(
            snapshot.chunks.size,
        )

        snapshot.chunks.forEach {
            writeChunk(
                output,
                it,
            )
        }

        output.writeInt(
            snapshot.tileEntities.size,
        )

        snapshot.tileEntities.forEach {
            output.writeInt(
                it.x,
            )

            output.writeInt(
                it.y,
            )

            output.writeInt(
                it.z,
            )

            writeNbt(
                output,
                it.nbt,
            )
        }

        output.writeInt(
            snapshot.entities.size,
        )

        snapshot.entities.forEach {
            writeEntity(
                output,
                it,
            )
        }
    }

    private fun writePlayer(
        output: DataOutput,
        player: ReplayPlayerSnapshot,
    ) {
        output.writeInt(
            player.entityId,
        )

        output.writeDouble(
            player.x,
        )

        output.writeDouble(
            player.y,
        )

        output.writeDouble(
            player.z,
        )

        output.writeFloat(
            player.yaw,
        )

        output.writeFloat(
            player.pitch,
        )

        output.writeDouble(
            player.motionX,
        )

        output.writeDouble(
            player.motionY,
        )

        output.writeDouble(
            player.motionZ,
        )
    }

    private fun writeChunk(
        output: DataOutput,
        chunk: ReplayChunkSnapshot,
    ) {
        output.writeInt(
            chunk.chunkX,
        )

        output.writeInt(
            chunk.chunkZ,
        )

        val runCount =
            countBlockRuns(
                chunk,
            )

        output.writeInt(
            runCount,
        )

        var index = 0

        while (index < chunk.blockIds.size) {
            val blockId =
                chunk.blockIds[index]

            val metadata =
                chunk.metadata[index]

            var runLength = 1

            while (
                index + runLength <
                chunk.blockIds.size &&
                chunk.blockIds[index + runLength] ==
                blockId &&
                chunk.metadata[index + runLength] ==
                metadata
            ) {
                runLength++
            }

            output.writeInt(
                blockId,
            )

            output.writeByte(
                metadata.toInt(),
            )

            output.writeInt(
                runLength,
            )

            index +=
                runLength
        }

        output.writeInt(
            chunk.biomes.size,
        )

        output.write(
            chunk.biomes,
        )
    }

    private fun countBlockRuns(
        chunk: ReplayChunkSnapshot,
    ): Int {
        var count = 0
        var index = 0

        while (index < chunk.blockIds.size) {
            val blockId =
                chunk.blockIds[index]

            val metadata =
                chunk.metadata[index]

            var runLength = 1

            while (
                index + runLength <
                chunk.blockIds.size &&
                chunk.blockIds[index + runLength] ==
                blockId &&
                chunk.metadata[index + runLength] ==
                metadata
            ) {
                runLength++
            }

            count++
            index +=
                runLength
        }

        return count
    }

    private fun writeEntity(
        output: DataOutput,
        entity: ReplayEntitySnapshot,
    ) {
        output.writeInt(
            entity.entityId,
        )

        output.writeBoolean(
            entity.entityType != null,
        )

        entity.entityType?.let {
            output.writeUTF(
                it,
            )
        }

        output.writeUTF(
            entity.entityClass,
        )

        output.writeDouble(
            entity.x,
        )

        output.writeDouble(
            entity.y,
        )

        output.writeDouble(
            entity.z,
        )

        output.writeFloat(
            entity.yaw,
        )

        output.writeFloat(
            entity.pitch,
        )

        output.writeDouble(
            entity.motionX,
        )

        output.writeDouble(
            entity.motionY,
        )

        output.writeDouble(
            entity.motionZ,
        )

        writeNbt(
            output,
            entity.nbt,
        )
    }

    private fun writeNbt(
        output: DataOutput,
        nbt: NBTTagCompound,
    ) {
        val bytes =
            ByteArrayOutputStream().use {
                CompressedStreamTools.writeCompressed(
                    nbt,
                    it,
                )

                it.toByteArray()
            }

        output.writeInt(
            bytes.size,
        )

        output.write(
            bytes,
        )
    }
}
