package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.snapshot.ReplayChunkSnapshot
import me.yuhan8954.flashback.snapshot.ReplayTileEntitySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotRestorer
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class ReplayMutationJournal {

    private val timeline =
        ReplayMutationTimeline<ReplayMutation>(
            HISTORY_DURATION_NANOS,
        )

    private var suspendedDepth =
        0

    private var enabled =
        false

    private val tileEntityState =
        mutableMapOf<Long, NBTTagCompound>()

    private val entityState =
        mutableMapOf<Int, ReplayReverseEntityState>()

    private val dirtyTileEntities =
        mutableSetOf<Long>()

    private val dirtyEntities =
        mutableSetOf<Int>()

    var timestampNanos =
        0L

    val recording: Boolean
        get() =
            enabled &&
                suspendedDepth ==
                0

    val coverage: ReplayReverseCoverage?
        get() =
            if (enabled) {
                timeline.coverage
            } else {
                null
            }

    fun clear() {
        timeline.clear()
        tileEntityState.clear()
        entityState.clear()
        dirtyTileEntities.clear()
        dirtyEntities.clear()
    }

    fun start(
        world: ReplayWorld,
        timestampNanos: Long,
    ) {
        clear()

        this.timestampNanos =
            timestampNanos

        timeline.start(
            timestampNanos,
        )

        enabled =
            true

        snapshotTileEntities(
            world,
        )

        snapshotEntities(
            world,
        )
    }

    fun suspend() {
        enabled =
            false
    }

    fun advanceTo(
        world: ReplayWorld,
        timestampNanos: Long,
    ) {
        if (!enabled) {
            this.timestampNanos =
                timestampNanos
            return
        }

        this.timestampNanos =
            timestampNanos

        timeline.advanceTo(
            timestampNanos,
        )

        captureDirtyTileEntityMutations(
            world,
        )

        captureDirtyEntityMutations(
            world,
        )
    }

    fun markTileEntityDirty(
        tileEntity: TileEntity,
    ) {
        if (!recording) {
            return
        }

        dirtyTileEntities +=
            tileEntityKey(
                tileEntity.xCoord,
                tileEntity.yCoord,
                tileEntity.zCoord,
            )
    }

    fun markEntityDirty(
        entity: Entity,
    ) {
        if (
            !recording ||
            entity is EntityReplayPlayer ||
            entity is EntityReplaySpectator
        ) {
            return
        }

        dirtyEntities +=
            entity.entityId
    }

    fun rememberTileEntity(
        tileEntity: TileEntity,
    ) {
        val key =
            tileEntityKey(
                tileEntity.xCoord,
                tileEntity.yCoord,
                tileEntity.zCoord,
            )

        captureTileEntity(
            tileEntity,
        )?.let {
            tileEntityState[
                key,
            ] =
                it
        }

        dirtyTileEntities -=
            key
    }

    fun forgetTileEntity(
        x: Int,
        y: Int,
        z: Int,
    ) {
        val key =
            tileEntityKey(
                x,
                y,
                z,
            )

        tileEntityState.remove(
            key,
        )
        dirtyTileEntities -=
            key
    }

    fun rememberEntity(
        entity: Entity,
    ) {
        if (
            entity is EntityReplayPlayer ||
            entity is EntityReplaySpectator
        ) {
            return
        }

        ReplayReverseEntityState.capture(
            entity,
        )?.let {
            entityState[
                entity.entityId,
            ] =
                it
        }

        dirtyEntities -=
            entity.entityId
    }

    fun forgetEntity(
        entityId: Int,
    ) {
        entityState.remove(
            entityId,
        )
        dirtyEntities -=
            entityId
    }

    fun record(
        mutation: ReplayMutation,
    ) {
        if (!recording) {
            return
        }

        timeline.append(
            timestampNanos,
            mutation,
        )
    }

    fun undoTo(
        session: ReplaySession,
        targetTimeNanos: Long,
    ) {
        val mutationsToUndo =
            timeline.removeAfter(
                targetTimeNanos,
            )

        withoutRecording {
            mutationsToUndo.forEach {
                it.undo(
                    session,
                )
            }
        }

        timestampNanos =
            targetTimeNanos

        snapshotTileEntities(
            session.world,
        )

        snapshotEntities(
            session.world,
        )
    }

    fun recordPacketState(
        session: ReplaySession,
        beforePlayer: ReplayReversePlayerState,
        beforeWorld: ReplayWorldMutationState,
    ) {
        if (!recording) {
            return
        }

        val afterPlayer =
            ReplayReversePlayerState.capture(
                session.recordedPlayer,
            )

        if (
            !beforePlayer.sameJournalState(
                afterPlayer,
            )
        ) {
            record(
                ReplayPlayerStateMutation(
                    beforePlayer,
                ),
            )
        }

        val afterWorld =
            ReplayWorldMutationState.capture(
                session.world,
            )

        if (
            beforeWorld !=
            afterWorld
        ) {
            record(
                ReplayWorldStateMutation(
                    beforeWorld,
                ),
            )
        }
    }

    fun <T> withoutRecording(
        action: () -> T,
    ): T {
        suspendedDepth++

        return try {
            action()
        } finally {
            suspendedDepth--
        }
    }

    private fun captureDirtyTileEntityMutations(
        world: ReplayWorld,
    ) {
        val dirty =
            dirtyTileEntities.toList()

        dirtyTileEntities.clear()

        dirty.forEach { key ->
            val x =
                unpackSigned26(
                    key ushr 38,
                )

            val y =
                (
                    key and
                        0xfffL
                    ).toInt()

            val z =
                unpackSigned26(
                    key ushr 12,
                )

            val previous =
                tileEntityState[
                    key,
                ]

            val current =
                captureTileEntity(
                    world.getTileEntity(
                        x,
                        y,
                        z,
                    ),
                )

            if (
                previous != null &&
                current != null &&
                previous != current
            ) {
                record(
                    ReplayTileEntityMutation(
                        x =
                        x,
                        y =
                        y,
                        z =
                        z,
                        beforeNbt =
                        previous,
                    ),
                )
            }

            if (current == null) {
                tileEntityState.remove(
                    key,
                )
            } else {
                tileEntityState[
                    key,
                ] =
                    current
            }
        }
    }

    private fun captureDirtyEntityMutations(
        world: ReplayWorld,
    ) {
        val dirty =
            dirtyEntities.toList()

        dirtyEntities.clear()

        dirty.forEach { entityId ->
            val previous =
                entityState[
                    entityId,
                ]

            val current =
                world.getEntityByID(
                    entityId,
                )?.let {
                    ReplayReverseEntityState.capture(
                        it,
                    )
                }

            if (
                previous != null &&
                current != null &&
                !previous.sameJournalState(
                    current,
                )
            ) {
                record(
                    ReplayEntityStateMutation(
                        previous,
                    ),
                )
            }

            if (current == null) {
                entityState.remove(
                    entityId,
                )
            } else {
                entityState[
                    entityId,
                ] =
                    current
            }
        }
    }

    private fun snapshotTileEntities(
        world: ReplayWorld,
    ) {
        tileEntityState.clear()

        world.loadedTileEntityList
            .filterIsInstance<TileEntity>()
            .forEach { tileEntity ->
                captureTileEntity(
                    tileEntity,
                )?.let {
                    tileEntityState[
                        tileEntityKey(
                            tileEntity.xCoord,
                            tileEntity.yCoord,
                            tileEntity.zCoord,
                        ),
                    ] =
                        it
                }
            }
    }

    private fun snapshotEntities(
        world: ReplayWorld,
    ) {
        entityState.clear()

        world.loadedEntityList
            .filterIsInstance<Entity>()
            .filter {
                it !is EntityReplayPlayer &&
                    it !is EntityReplaySpectator
            }.forEach { entity ->
                ReplayReverseEntityState.capture(
                    entity,
                )?.let {
                    entityState[
                        entity.entityId,
                    ] =
                        it
                }
            }
    }

    private fun unpackSigned26(
        value: Long,
    ): Int {
        val masked =
            value.toInt() and
                0x3ffffff

        return if (
            masked and
            0x2000000 !=
            0
        ) {
            masked or
                -0x4000000
        } else {
            masked
        }
    }

    private fun tileEntityKey(
        x: Int,
        y: Int,
        z: Int,
    ): Long = (
        x.toLong() and
            0x3ffffffL
        ) shl 38 xor
        (
            z.toLong() and
                0x3ffffffL
            ) shl 12 xor
        (
            y.toLong() and
                0xfffL
            )

    companion object {

        const val HISTORY_DURATION_NANOS =
            ReplayReverseHistory.HISTORY_DURATION_NANOS
    }
}

sealed interface ReplayMutation {

    fun undo(
        session: ReplaySession,
    )
}

data class ReplayChunkStateMutation(
    val chunk: ReplayChunkSnapshot,
    val tileEntities: List<ReplayTileEntitySnapshot>,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        SnapshotRestorer.restoreChunk(
            world,
            chunk,
        )

        tileEntities.forEach {
            SnapshotRestorer.restoreTileEntity(
                world,
                it,
            )
        }
    }
}

data class ReplayChunkLoadedMutation(
    val chunkX: Int,
    val chunkZ: Int,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        world.doPreChunk(
            chunkX,
            chunkZ,
            false,
        )
    }
}

data class ReplayChunkUnloadedMutation(
    val chunk: ReplayChunkSnapshot,
    val tileEntities: List<ReplayTileEntitySnapshot>,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        SnapshotRestorer.restoreChunk(
            world,
            chunk,
        )

        tileEntities.forEach {
            SnapshotRestorer.restoreTileEntity(
                world,
                it,
            )
        }
    }
}

data class ReplayBlockMutation(
    val x: Int,
    val y: Int,
    val z: Int,
    val before: ReplayBlockState,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        world.setBlock(
            x,
            y,
            z,
            before.block,
            before.metadata,
            3,
        )

        restoreTileEntity(
            world,
            x,
            y,
            z,
            before.tileEntityNbt,
        )
    }
}

data class ReplayTileEntityMutation(
    val x: Int,
    val y: Int,
    val z: Int,
    val beforeNbt: NBTTagCompound?,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        restoreTileEntity(
            world,
            x,
            y,
            z,
            beforeNbt,
        )
    }
}

data class ReplayEntityStateMutation(
    val before: ReplayReverseEntityState,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        val current =
            world.getEntityByID(
                before.entityId,
            )

        val target =
            if (
                current != null &&
                current.javaClass.name ==
                before.entityClass
            ) {
                current
            } else {
                if (current != null) {
                    world.removeEntityFromWorld(
                        before.entityId,
                    )
                }

                before.create(
                    world,
                )?.also {
                    world.addEntityToWorld(
                        before.entityId,
                        it,
                    )
                }
            }

        if (target != null) {
            before.restore(
                target,
                newerState = null,
                interpolation = 0.0,
            )
        }
    }
}

data class ReplayEntityAddedMutation(
    val entityId: Int,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        world.removeEntityFromWorld(
            entityId,
        )
    }
}

data class ReplayEntityRemovedMutation(
    val state: ReplayReverseEntityState,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        val world =
            session.world

        val existing =
            world.getEntityByID(
                state.entityId,
            )

        if (existing != null) {
            world.removeEntityFromWorld(
                state.entityId,
            )
        }

        val restored =
            state.create(
                world,
            ) ?: return

        state.restore(
            restored,
            newerState = null,
            interpolation = 0.0,
        )

        world.addEntityToWorld(
            state.entityId,
            restored,
        )
    }
}

data class ReplayPlayerStateMutation(
    val before: ReplayReversePlayerState,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        before.restore(
            session.recordedPlayer,
            newerState = null,
            interpolation = 0.0,
        )
    }
}

data class ReplayWorldStateMutation(
    val before: ReplayWorldMutationState,
) : ReplayMutation {

    override fun undo(
        session: ReplaySession,
    ) {
        before.restore(
            session.world,
        )
    }
}

data class ReplayWorldMutationState(
    val worldTime: Long,
    val totalWorldTime: Long,
    val raining: Boolean,
    val thundering: Boolean,
    val rainStrength: Float,
    val thunderStrength: Float,
) {

    fun restore(
        world: ReplayWorld,
    ) {
        world.setWorldTime(
            worldTime,
        )
        world.func_82738_a(
            totalWorldTime,
        )
        world.worldInfo.setRaining(
            raining,
        )
        world.worldInfo.setThundering(
            thundering,
        )
        world.setRainStrength(
            rainStrength,
        )
        world.setThunderStrength(
            thunderStrength,
        )
    }

    companion object {

        fun capture(
            world: ReplayWorld,
        ): ReplayWorldMutationState = ReplayWorldMutationState(
            worldTime =
            world.worldTime,
            totalWorldTime =
            world.totalWorldTime,
            raining =
            world.worldInfo.isRaining,
            thundering =
            world.worldInfo.isThundering,
            rainStrength =
            world.rainingStrength,
            thunderStrength =
            world.thunderingStrength,
        )
    }
}

data class ReplayBlockState(
    val block: Block,
    val metadata: Int,
    val tileEntityNbt: NBTTagCompound?,
) {

    companion object {

        fun capture(
            world: ReplayWorld,
            x: Int,
            y: Int,
            z: Int,
        ): ReplayBlockState = ReplayBlockState(
            block =
            world.getBlock(
                x,
                y,
                z,
            ),
            metadata =
            world.getBlockMetadata(
                x,
                y,
                z,
            ),
            tileEntityNbt =
            captureTileEntity(
                world.getTileEntity(
                    x,
                    y,
                    z,
                ),
            ),
        )
    }
}

fun captureTileEntity(
    tileEntity: TileEntity?,
): NBTTagCompound? {
    if (tileEntity == null) {
        return null
    }

    return try {
        NBTTagCompound()
            .also {
                tileEntity.writeToNBT(
                    it,
                )
            }
    } catch (
        throwable: Throwable,
    ) {
        System.err.println(
            "[Flashback] Failed to journal tile entity: " +
                tileEntity.javaClass.name,
        )

        throwable.printStackTrace()
        null
    }
}

private fun restoreTileEntity(
    world: ReplayWorld,
    x: Int,
    y: Int,
    z: Int,
    nbt: NBTTagCompound?,
) {
    if (nbt == null) {
        world.removeTileEntity(
            x,
            y,
            z,
        )

        return
    }

    val restored =
        TileEntity.createAndLoadEntity(
            nbt.copy() as
                NBTTagCompound,
        ) ?: return

    world.setTileEntity(
        x,
        y,
        z,
        restored,
    )
}
