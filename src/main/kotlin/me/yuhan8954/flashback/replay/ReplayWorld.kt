package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.snapshot.ReplayTileEntitySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotCapture
import net.minecraft.block.Block
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.profiler.Profiler
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.WorldSettings

class ReplayWorld(
    handler: ReplayNetHandler,
    settings: WorldSettings,
    dimension: Int,
    difficulty: EnumDifficulty,
    profiler: Profiler,
) : WorldClient(
    handler,
    settings,
    dimension,
    difficulty,
    profiler,
) {

    val mutationJournal =
        ReplayMutationJournal()

    private var replayTimeNanos =
        0L

    private var timeBaseReplayNanos =
        0L

    private var timeBaseWorldTime =
        0L

    private var timeBaseTotalWorldTime =
        0L

    private var daylightCycle =
        true

    private var completedSimulationTicks =
        0L

    private var simulationTicks =
        0

    fun initializeReplayTime(
        worldTime: Long,
        totalWorldTime: Long,
    ) {
        correctReplayTime(
            worldTime,
            totalWorldTime,
            0L,
        )
    }

    fun correctReplayTime(
        worldTime: Long,
        totalWorldTime: Long,
        currentReplayTimeNanos: Long,
    ) {
        replayTimeNanos = currentReplayTimeNanos
        timeBaseReplayNanos = currentReplayTimeNanos
        timeBaseWorldTime = normalizeWorldTime(worldTime)
        timeBaseTotalWorldTime = totalWorldTime
        daylightCycle = worldTime >= 0L
        applyReplayTime()
    }

    fun beginReplayTick(
        currentReplayTimeNanos: Long,
    ) {
        mutationJournal.advanceTo(
            this,
            currentReplayTimeNanos,
        )

        replayTimeNanos = currentReplayTimeNanos
        applyReplayTime()

        val targetSimulationTicks =
            currentReplayTimeNanos /
                ReplayClock.MINECRAFT_TICK_NANOS

        simulationTicks =
            (targetSimulationTicks - completedSimulationTicks)
                .coerceIn(
                    0L,
                    Int.MAX_VALUE.toLong(),
                ).toInt()

        completedSimulationTicks =
            targetSimulationTicks
    }

    fun seekReplayTime(currentReplayTimeNanos: Long) {
        replayTimeNanos = currentReplayTimeNanos
        completedSimulationTicks = currentReplayTimeNanos / ReplayClock.MINECRAFT_TICK_NANOS
        simulationTicks = 0
        applyReplayTime()
    }

    fun startMutationJournal(
        currentReplayTimeNanos: Long,
    ) {
        mutationJournal.start(
            this,
            currentReplayTimeNanos,
        )
    }

    fun suspendMutationJournal() {
        mutationJournal.suspend()
    }

    fun undoMutationsTo(
        targetTimeNanos: Long,
    ) {
        mutationJournal.undoTo(
            this,
            targetTimeNanos,
        )
    }

    override fun doPreChunk(
        chunkX: Int,
        chunkZ: Int,
        loadChunk: Boolean,
    ) {
        val wasLoaded =
            chunkProvider.chunkExists(
                chunkX,
                chunkZ,
            )

        val unloadedChunk =
            if (
                mutationJournal.recording &&
                wasLoaded &&
                !loadChunk
            ) {
                SnapshotCapture.captureChunk(
                    getChunkFromChunkCoords(
                        chunkX,
                        chunkZ,
                    ),
                )
            } else {
                null
            }

        val unloadedTileEntities =
            if (unloadedChunk != null) {
                captureChunkTileEntities(
                    chunkX,
                    chunkZ,
                )
            } else {
                emptyList()
            }

        if (
            mutationJournal.recording &&
            (
                unloadedChunk != null ||
                    loadChunk &&
                    !wasLoaded
                )
        ) {
            mutationJournal.withoutRecording {
                super.doPreChunk(
                    chunkX,
                    chunkZ,
                    loadChunk,
                )
            }
        } else {
            super.doPreChunk(
                chunkX,
                chunkZ,
                loadChunk,
            )
        }

        if (
            !mutationJournal.recording
        ) {
            return
        }

        if (
            loadChunk &&
            !wasLoaded &&
            chunkProvider.chunkExists(
                chunkX,
                chunkZ,
            )
        ) {
            mutationJournal.record(
                ReplayChunkLoadedMutation(
                    chunkX,
                    chunkZ,
                ),
            )

            return
        }

        if (
            !loadChunk &&
            unloadedChunk != null
        ) {
            mutationJournal.record(
                ReplayChunkUnloadedMutation(
                    chunk =
                    unloadedChunk,
                    tileEntities =
                    unloadedTileEntities,
                ),
            )
        }
    }

    override fun setBlock(
        x: Int,
        y: Int,
        z: Int,
        block: Block,
        metadata: Int,
        flags: Int,
    ): Boolean {
        val before =
            if (
                mutationJournal.recording
            ) {
                ReplayBlockState.capture(
                    this,
                    x,
                    y,
                    z,
                )
            } else {
                null
            }

        val changed =
            super.setBlock(
                x,
                y,
                z,
                block,
                metadata,
                flags,
            )

        if (
            changed &&
            before != null
        ) {
            mutationJournal.record(
                ReplayBlockMutation(
                    x,
                    y,
                    z,
                    before,
                ),
            )
        }

        return changed
    }

    override fun setBlockMetadataWithNotify(
        x: Int,
        y: Int,
        z: Int,
        metadata: Int,
        flags: Int,
    ): Boolean {
        val before =
            if (
                mutationJournal.recording
            ) {
                ReplayBlockState.capture(
                    this,
                    x,
                    y,
                    z,
                )
            } else {
                null
            }

        val changed =
            super.setBlockMetadataWithNotify(
                x,
                y,
                z,
                metadata,
                flags,
            )

        if (
            changed &&
            before != null
        ) {
            mutationJournal.record(
                ReplayBlockMutation(
                    x,
                    y,
                    z,
                    before,
                ),
            )
        }

        return changed
    }

    override fun setTileEntity(
        x: Int,
        y: Int,
        z: Int,
        tileEntity: TileEntity,
    ) {
        val before =
            if (
                mutationJournal.recording
            ) {
                captureTileEntity(
                    getTileEntity(
                        x,
                        y,
                        z,
                    ),
                )
            } else {
                null
            }

        super.setTileEntity(
            x,
            y,
            z,
            tileEntity,
        )

        if (
            mutationJournal.recording
        ) {
            mutationJournal.record(
                ReplayTileEntityMutation(
                    x,
                    y,
                    z,
                    before,
                ),
            )
        }
    }

    override fun removeTileEntity(
        x: Int,
        y: Int,
        z: Int,
    ) {
        val before =
            if (
                mutationJournal.recording
            ) {
                captureTileEntity(
                    getTileEntity(
                        x,
                        y,
                        z,
                    ),
                )
            } else {
                null
            }

        super.removeTileEntity(
            x,
            y,
            z,
        )

        if (
            before != null
        ) {
            mutationJournal.record(
                ReplayTileEntityMutation(
                    x,
                    y,
                    z,
                    before,
                ),
            )
        }
    }

    override fun addEntityToWorld(
        entityId: Int,
        entity: Entity,
    ) {
        super.addEntityToWorld(
            entityId,
            entity,
        )

        if (
            mutationJournal.recording
        ) {
            mutationJournal.record(
                ReplayEntityAddedMutation(
                    entityId,
                ),
            )
        }
    }

    override fun removeEntityFromWorld(
        entityId: Int,
    ): Entity? {
        val state =
            if (
                mutationJournal.recording
            ) {
                getEntityByID(
                    entityId,
                )?.let {
                    ReplayReverseEntityState.capture(
                        it,
                    )
                }
            } else {
                null
            }

        val removed =
            super.removeEntityFromWorld(
                entityId,
            )

        if (
            state != null
        ) {
            mutationJournal.record(
                ReplayEntityRemovedMutation(
                    state,
                ),
            )
        }

        return removed
    }

    override fun tick() {
        repeat(simulationTicks) {
            super.tick()
            applyReplayTime()
        }
    }

    override fun updateEntities() {
        repeat(simulationTicks) {
            super.updateEntities()
        }
    }

    override fun getCelestialAngle(partialTicks: Float): Float {
        val elapsedNanos =
            (replayTimeNanos - timeBaseReplayNanos)
                .coerceAtLeast(0L)

        val elapsedTicks =
            elapsedNanos /
                ReplayClock.MINECRAFT_TICK_NANOS

        val replayPartialTick =
            if (daylightCycle) {
                (
                    elapsedNanos %
                        ReplayClock.MINECRAFT_TICK_NANOS
                    ).toFloat() /
                    ReplayClock.MINECRAFT_TICK_NANOS
            } else {
                0.0f
            }

        return provider.calculateCelestialAngle(
            timeBaseWorldTime +
                if (daylightCycle) {
                    elapsedTicks
                } else {
                    0L
                },
            replayPartialTick,
        )
    }

    private fun captureChunkTileEntities(
        chunkX: Int,
        chunkZ: Int,
    ): List<ReplayTileEntitySnapshot> =
        loadedTileEntityList
            .filterIsInstance<TileEntity>()
            .asSequence()
            .filter {
                it.xCoord shr 4 ==
                    chunkX &&
                    it.zCoord shr 4 ==
                    chunkZ
            }.mapNotNull(
                SnapshotCapture::captureTileEntity,
            ).toList()

    private fun applyReplayTime() {
        val elapsedTicks =
            (replayTimeNanos - timeBaseReplayNanos) /
                ReplayClock.MINECRAFT_TICK_NANOS

        val worldTime =
            if (daylightCycle) {
                timeBaseWorldTime + elapsedTicks
            } else {
                timeBaseWorldTime
            }

        setWorldTime(
            worldTime,
        )

        func_82738_a(
            timeBaseTotalWorldTime + elapsedTicks,
        )
    }

    private fun normalizeWorldTime(worldTime: Long): Long = if (worldTime < 0L) {
        -worldTime
    } else {
        worldTime
    }
}
