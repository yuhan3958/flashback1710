package me.yuhan8954.flashback.snapshot

import com.mojang.authlib.GameProfile
import me.yuhan8954.flashback.replay.ReplayWorld
import net.minecraft.block.Block
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityList
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import java.util.UUID

object SnapshotRestorer {

    private const val CHUNK_WIDTH = 16
    private const val CHUNK_HEIGHT = 256

    fun restore(
        world: ReplayWorld,
        player: EntityClientPlayerMP,
        snapshot: ReplaySnapshot,
    ) {
        restoreWorldState(
            world,
            snapshot,
        )

        snapshot.chunks.forEach {
            restoreChunk(
                world,
                it,
            )
        }

        snapshot.tileEntities.forEach {
            restoreTileEntity(
                world,
                it,
            )
        }

        snapshot.entities.forEach {
            restoreEntity(
                world,
                it,
            )
        }

        restorePlayer(
            world,
            player,
            snapshot.player,
        )
    }

    private fun restoreWorldState(
        world: ReplayWorld,
        snapshot: ReplaySnapshot,
    ) {
        world.initializeReplayTime(
            snapshot.worldTime,
            snapshot.totalWorldTime,
        )

        world.worldInfo.setRaining(
            snapshot.raining,
        )

        world.worldInfo.setThundering(
            snapshot.thundering,
        )

        world.setRainStrength(
            snapshot.rainStrength,
        )

        world.setThunderStrength(
            snapshot.thunderStrength,
        )
    }

    private fun restoreChunk(
        world: ReplayWorld,
        snapshot: ReplayChunkSnapshot,
    ) {
        world.doPreChunk(
            snapshot.chunkX,
            snapshot.chunkZ,
            true,
        )

        val chunk =
            world.getChunkFromChunkCoords(
                snapshot.chunkX,
                snapshot.chunkZ,
            )

        for (y in 0 until CHUNK_HEIGHT) {
            for (z in 0 until CHUNK_WIDTH) {
                for (x in 0 until CHUNK_WIDTH) {
                    val index =
                        blockIndex(
                            x,
                            y,
                            z,
                        )

                    val blockId =
                        snapshot.blockIds[index]

                    if (blockId == 0) {
                        continue
                    }

                    val block =
                        Block.getBlockById(
                            blockId,
                        ) ?: continue

                    chunk.func_150807_a(
                        x,
                        y,
                        z,
                        block,
                        snapshot.metadata[index]
                            .toInt() and 0xf,
                    )
                }
            }
        }

        chunk.setBiomeArray(
            snapshot.biomes.copyOf(),
        )

        chunk.generateHeightMap()
        chunk.generateSkylightMap()

        chunk.isTerrainPopulated =
            true

        chunk.isLightPopulated =
            true

        world.markBlockRangeForRenderUpdate(
            snapshot.chunkX shl 4,
            0,
            snapshot.chunkZ shl 4,
            (snapshot.chunkX shl 4) + 15,
            255,
            (snapshot.chunkZ shl 4) + 15,
        )
    }

    private fun restoreTileEntity(
        world: ReplayWorld,
        snapshot: ReplayTileEntitySnapshot,
    ) {
        try {
            val tileEntity =
                TileEntity.createAndLoadEntity(
                    snapshot.nbt.copy() as
                        NBTTagCompound,
                ) ?: return

            tileEntity.setWorldObj(
                world,
            )

            world.setTileEntity(
                snapshot.x,
                snapshot.y,
                snapshot.z,
                tileEntity,
            )
        } catch (
            throwable: Throwable,
        ) {
            System.err.println(
                "[Flashback] Failed to restore tile entity at " +
                    "${snapshot.x},${snapshot.y},${snapshot.z}",
            )

            throwable.printStackTrace()
        }
    }

    private fun restoreEntity(
        world: ReplayWorld,
        snapshot: ReplayEntitySnapshot,
    ) {
        try {
            val nbt =
                snapshot.nbt.copy() as
                    NBTTagCompound

            snapshot.entityType?.let {
                nbt.setString(
                    "id",
                    it,
                )
            }

            val entity =
                createEntity(
                    world,
                    snapshot,
                    nbt,
                ) ?: return

            entity.setEntityId(
                snapshot.entityId,
            )

            entity.serverPosX = snapshot.serverPosX
            entity.serverPosY = snapshot.serverPosY
            entity.serverPosZ = snapshot.serverPosZ

            entity.setPositionAndRotation(
                snapshot.x,
                snapshot.y,
                snapshot.z,
                snapshot.yaw,
                snapshot.pitch,
            )

            entity.prevPosX = snapshot.x
            entity.prevPosY = snapshot.y
            entity.prevPosZ = snapshot.z
            entity.lastTickPosX = snapshot.x
            entity.lastTickPosY = snapshot.y
            entity.lastTickPosZ = snapshot.z
            entity.prevRotationYaw = snapshot.yaw
            entity.prevRotationPitch = snapshot.pitch
            entity.motionX = snapshot.motionX
            entity.motionY = snapshot.motionY
            entity.motionZ = snapshot.motionZ

            world.addEntityToWorld(
                snapshot.entityId,
                entity,
            )
        } catch (
            throwable: Throwable,
        ) {
            System.err.println(
                "[Flashback] Failed to restore entity: " +
                    snapshot.entityClass,
            )

            throwable.printStackTrace()
        }
    }

    private fun createEntity(
        world: ReplayWorld,
        snapshot: ReplayEntitySnapshot,
        nbt: NBTTagCompound,
    ): Entity? {
        createOtherPlayer(
            world,
            snapshot,
            nbt,
        )?.let {
            return it
        }

        if (snapshot.entityType != null) {
            EntityList.createEntityFromNBT(
                nbt,
                world,
            )?.let {
                return it
            }
        }

        val entityClass =
            Class.forName(
                snapshot.entityClass,
            ).asSubclass(
                Entity::class.java,
            )

        val constructor =
            entityClass.getDeclaredConstructor(
                World::class.java,
            )

        constructor.isAccessible =
            true

        return constructor.newInstance(
            world,
        ).also {
            it.readFromNBT(
                nbt,
            )
        }
    }

    private fun createOtherPlayer(
        world: ReplayWorld,
        snapshot: ReplayEntitySnapshot,
        nbt: NBTTagCompound,
    ): EntityOtherPlayerMP? {
        val profileName =
            snapshot.playerProfileName ?: return null

        val profileId =
            snapshot.playerProfileId
                ?.let(
                    UUID::fromString,
                )

        return EntityOtherPlayerMP(
            world,
            GameProfile(
                profileId,
                profileName,
            ),
        ).also {
            it.readFromNBT(
                nbt,
            )
        }
    }

    private fun restorePlayer(
        world: ReplayWorld,
        player: EntityClientPlayerMP,
        snapshot: ReplayPlayerSnapshot,
    ) {
        player.setEntityId(
            snapshot.entityId,
        )

        player.setPositionAndRotation(
            snapshot.x,
            snapshot.y,
            snapshot.z,
            snapshot.yaw,
            snapshot.pitch,
        )

        player.prevPosX = snapshot.x
        player.prevPosY = snapshot.y
        player.prevPosZ = snapshot.z
        player.lastTickPosX = snapshot.x
        player.lastTickPosY = snapshot.y
        player.lastTickPosZ = snapshot.z
        player.prevRotationYaw = snapshot.yaw
        player.prevRotationPitch = snapshot.pitch
        player.motionX = snapshot.motionX
        player.motionY = snapshot.motionY
        player.motionZ = snapshot.motionZ

        check(
            world.spawnEntityInWorld(
                player,
            ),
        ) {
            "Replay player could not be added to its snapshot chunk"
        }
    }

    private fun blockIndex(
        x: Int,
        y: Int,
        z: Int,
    ): Int = (y * CHUNK_WIDTH + z) *
        CHUNK_WIDTH +
        x
}
