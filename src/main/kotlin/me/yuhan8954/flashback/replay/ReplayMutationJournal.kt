package me.yuhan8954.flashback.replay

import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import java.util.ArrayDeque

class ReplayMutationJournal {

    private val mutations =
        ArrayDeque<TimestampedReplayMutation>()

    private var suspendedDepth =
        0

    private var enabled =
        false

    private val tileEntityState =
        mutableMapOf<Long, NBTTagCompound>()

    var timestampNanos =
        0L

    val recording: Boolean
        get() =
            enabled &&
                suspendedDepth ==
                0

    fun clear() {
        mutations.clear()
        tileEntityState.clear()
    }

    fun start(
        world: ReplayWorld,
        timestampNanos: Long,
    ) {
        clear()

        this.timestampNanos =
            timestampNanos

        enabled =
            true

        snapshotTileEntities(
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

        captureTileEntityMutations(
            world,
        )

        trim()
    }

    fun record(
        mutation: ReplayMutation,
    ) {
        if (!recording) {
            return
        }

        mutations.addLast(
            TimestampedReplayMutation(
                timestampNanos =
                timestampNanos,
                mutation =
                mutation,
            ),
        )

        trim()
    }

    fun undoTo(
        world: ReplayWorld,
        targetTimeNanos: Long,
    ) {
        withoutRecording {
            while (
                mutations.isNotEmpty() &&
                mutations.peekLast()
                    .timestampNanos >
                targetTimeNanos
            ) {
                mutations.removeLast()
                    .mutation
                    .undo(
                        world,
                    )
            }
        }

        timestampNanos =
            targetTimeNanos

        snapshotTileEntities(
            world,
        )
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

    private fun captureTileEntityMutations(
        world: ReplayWorld,
    ) {
        val current =
            mutableMapOf<Long, NBTTagCompound>()

        world.loadedTileEntityList
            .filterIsInstance<TileEntity>()
            .forEach { tileEntity ->
                val nbt =
                    captureTileEntity(
                        tileEntity,
                    ) ?: return@forEach

                val key =
                    tileEntityKey(
                        tileEntity.xCoord,
                        tileEntity.yCoord,
                        tileEntity.zCoord,
                    )

                current[key] =
                    nbt

                val previous =
                    tileEntityState[
                        key,
                    ]

                if (
                    previous != null &&
                    previous != nbt
                ) {
                    record(
                        ReplayTileEntityMutation(
                            x =
                            tileEntity.xCoord,
                            y =
                            tileEntity.yCoord,
                            z =
                            tileEntity.zCoord,
                            beforeNbt =
                            previous,
                        ),
                    )
                }
            }

        tileEntityState.clear()
        tileEntityState.putAll(
            current,
        )
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

    private fun trim() {
        val minimumTimeNanos =
            (
                timestampNanos -
                    HISTORY_DURATION_NANOS
                ).coerceAtLeast(
                0L,
            )

        while (
            mutations.isNotEmpty() &&
            mutations.peekFirst()
                .timestampNanos <
            minimumTimeNanos
        ) {
            mutations.removeFirst()
        }
    }

    companion object {

        const val HISTORY_DURATION_NANOS =
            ReplayReverseHistory.HISTORY_DURATION_NANOS
    }
}

data class TimestampedReplayMutation(
    val timestampNanos: Long,
    val mutation: ReplayMutation,
)

sealed interface ReplayMutation {

    fun undo(
        world: ReplayWorld,
    )
}

data class ReplayBlockMutation(
    val x: Int,
    val y: Int,
    val z: Int,
    val before: ReplayBlockState,
) : ReplayMutation {

    override fun undo(
        world: ReplayWorld,
    ) {
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
        world: ReplayWorld,
    ) {
        restoreTileEntity(
            world,
            x,
            y,
            z,
            beforeNbt,
        )
    }
}

data class ReplayEntityAddedMutation(
    val entityId: Int,
) : ReplayMutation {

    override fun undo(
        world: ReplayWorld,
    ) {
        world.removeEntityFromWorld(
            entityId,
        )
    }
}

data class ReplayEntityRemovedMutation(
    val state: ReplayReverseEntityState,
) : ReplayMutation {

    override fun undo(
        world: ReplayWorld,
    ) {
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
        ): ReplayBlockState =
            ReplayBlockState(
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
