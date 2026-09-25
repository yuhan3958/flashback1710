package me.yuhan8954.flashback.snapshot

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.multiplayer.ChunkProviderClient
import net.minecraft.entity.EntityList
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MathHelper
import net.minecraft.world.chunk.Chunk

object SnapshotCapture {

    private const val CHUNK_WIDTH = 16
    private const val CHUNK_HEIGHT = 256
    private const val BLOCKS_PER_CHUNK =
        CHUNK_WIDTH *
            CHUNK_WIDTH *
            CHUNK_HEIGHT

    fun capture(minecraft: Minecraft): ReplaySnapshot {
        val world =
            requireNotNull(
                minecraft.theWorld,
            ) {
                "Cannot record without a client world"
            }

        val player =
            requireNotNull(
                minecraft.thePlayer,
            ) {
                "Cannot record without a client player"
            }

        val chunks =
            captureChunks(
                minecraft,
            )

        val chunkPositions =
            chunks.mapTo(
                mutableSetOf(),
            ) {
                chunkKey(
                    it.chunkX,
                    it.chunkZ,
                )
            }

        val tileEntities =
            world.loadedTileEntityList
                .asSequence()
                .filter {
                    chunkKey(
                        it.xCoord shr 4,
                        it.zCoord shr 4,
                    ) in chunkPositions
                }.mapNotNull { tileEntity ->
                    try {
                        val nbt =
                            NBTTagCompound()

                        tileEntity.writeToNBT(
                            nbt,
                        )

                        ReplayTileEntitySnapshot(
                            x = tileEntity.xCoord,
                            y = tileEntity.yCoord,
                            z = tileEntity.zCoord,
                            nbt = nbt,
                        )
                    } catch (
                        throwable: Throwable,
                    ) {
                        System.err.println(
                            "[Flashback] Failed to snapshot tile entity: " +
                                tileEntity.javaClass.name,
                        )

                        throwable.printStackTrace()
                        null
                    }
                }.toList()

        val entities =
            world.loadedEntityList
                .asSequence()
                .filter {
                    it !== player
                }.mapNotNull { entity ->
                    try {
                        val nbt =
                            NBTTagCompound()

                        entity.writeToNBT(
                            nbt,
                        )

                        val entityType =
                            EntityList.getEntityString(
                                entity,
                            )

                        if (entityType != null) {
                            nbt.setString(
                                "id",
                                entityType,
                            )
                        }

                        ReplayEntitySnapshot(
                            entityId = entity.entityId,
                            entityType = entityType,
                            entityClass = entity.javaClass.name,
                            playerProfileId =
                            if (entity is EntityOtherPlayerMP) {
                                entity.gameProfile.id
                                    ?.toString()
                            } else {
                                null
                            },
                            playerProfileName =
                            if (entity is EntityOtherPlayerMP) {
                                entity.gameProfile.name
                            } else {
                                null
                            },
                            serverPosX = entity.serverPosX,
                            serverPosY = entity.serverPosY,
                            serverPosZ = entity.serverPosZ,
                            x = entity.posX,
                            y = entity.posY,
                            z = entity.posZ,
                            yaw = entity.rotationYaw,
                            pitch = entity.rotationPitch,
                            motionX = entity.motionX,
                            motionY = entity.motionY,
                            motionZ = entity.motionZ,
                            nbt = nbt,
                        )
                    } catch (
                        throwable: Throwable,
                    ) {
                        System.err.println(
                            "[Flashback] Failed to snapshot entity: " +
                                entity.javaClass.name,
                        )

                        throwable.printStackTrace()
                        null
                    }
                }.toList()

        return ReplaySnapshot(
            dimensionId = world.provider.dimensionId,
            seed = world.seed,
            worldTime = world.worldTime,
            totalWorldTime = world.totalWorldTime,
            raining = world.worldInfo.isRaining,
            thundering = world.worldInfo.isThundering,
            rainStrength = world.rainingStrength,
            thunderStrength = world.thunderingStrength,
            player = ReplayPlayerSnapshot(
                entityId = player.entityId,
                x = player.posX,
                y = player.posY,
                z = player.posZ,
                yaw = player.rotationYaw,
                pitch = player.rotationPitch,
                motionX = player.motionX,
                motionY = player.motionY,
                motionZ = player.motionZ,
            ),
            chunks = chunks,
            tileEntities = tileEntities,
            entities = entities,
        )
    }

    private fun captureChunks(
        minecraft: Minecraft,
    ): List<ReplayChunkSnapshot> {
        val world =
            requireNotNull(
                minecraft.theWorld,
            )

        val player =
            requireNotNull(
                minecraft.thePlayer,
            )

        val provider =
            world.chunkProvider as
                ChunkProviderClient

        val centerX =
            MathHelper.floor_double(
                player.posX,
            ) shr 4

        val centerZ =
            MathHelper.floor_double(
                player.posZ,
            ) shr 4

        val radius =
            minecraft.gameSettings
                .renderDistanceChunks

        val result =
            mutableListOf<ReplayChunkSnapshot>()

        for (
        chunkX in
        centerX - radius..centerX + radius
        ) {
            for (
            chunkZ in
            centerZ - radius..centerZ + radius
            ) {
                if (
                    !provider.chunkExists(
                        chunkX,
                        chunkZ,
                    )
                ) {
                    continue
                }

                result +=
                    captureChunk(
                        provider.provideChunk(
                            chunkX,
                            chunkZ,
                        ),
                    )
            }
        }

        return result
    }

    private fun captureChunk(
        chunk: Chunk,
    ): ReplayChunkSnapshot {
        val blockIds =
            IntArray(
                BLOCKS_PER_CHUNK,
            )

        val metadata =
            ByteArray(
                BLOCKS_PER_CHUNK,
            )

        chunk.blockStorageArray
            .filterNotNull()
            .forEach { storage ->
                for (localY in 0 until CHUNK_WIDTH) {
                    val y =
                        storage.yLocation +
                            localY

                    for (z in 0 until CHUNK_WIDTH) {
                        for (x in 0 until CHUNK_WIDTH) {
                            val index =
                                blockIndex(
                                    x,
                                    y,
                                    z,
                                )

                            blockIds[index] =
                                Block.getIdFromBlock(
                                    storage.getBlockByExtId(
                                        x,
                                        localY,
                                        z,
                                    ),
                                )

                            metadata[index] =
                                storage.getExtBlockMetadata(
                                    x,
                                    localY,
                                    z,
                                ).toByte()
                        }
                    }
                }
            }

        return ReplayChunkSnapshot(
            chunkX = chunk.xPosition,
            chunkZ = chunk.zPosition,
            blockIds = blockIds,
            metadata = metadata,
            biomes = chunk.biomeArray.copyOf(),
        )
    }

    private fun blockIndex(
        x: Int,
        y: Int,
        z: Int,
    ): Int = (y * CHUNK_WIDTH + z) *
        CHUNK_WIDTH +
        x

    private fun chunkKey(
        chunkX: Int,
        chunkZ: Int,
    ): Long = (chunkX.toLong() shl 32) xor
        (chunkZ.toLong() and 0xffffffffL)
}
