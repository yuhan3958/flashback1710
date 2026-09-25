package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.snapshot.ReplayChunkSnapshot
import me.yuhan8954.flashback.snapshot.ReplayTileEntitySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotCapture
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.chunk.Chunk
import java.util.ArrayDeque

object ReplayChunkMutationHooks {

    private val blockChanges =
        ThreadLocal.withInitial {
            ArrayDeque<ReplayBlockCapture>()
        }

    private val chunkChanges =
        ThreadLocal.withInitial {
            ArrayDeque<ReplayChunkCapture>()
        }

    @JvmStatic
    fun beforeBlockChange(
        chunk: Chunk,
        x: Int,
        y: Int,
        z: Int,
    ) {
        val world =
            chunk.worldObj as?
                ReplayWorld
                ?: return

        if (
            !world.mutationJournal
                .recording
        ) {
            return
        }

        val worldX =
            (chunk.xPosition shl 4) +
                x

        val worldZ =
            (chunk.zPosition shl 4) +
                z

        blockChanges.get()
            .addLast(
                ReplayBlockCapture(
                    world =
                    world,
                    x =
                    worldX,
                    y =
                    y,
                    z =
                    worldZ,
                    before =
                    ReplayBlockState.capture(
                        world,
                        worldX,
                        y,
                        worldZ,
                    ),
                ),
            )
    }

    @JvmStatic
    fun afterBlockChange(
        changed: Boolean,
    ) {
        val stack =
            blockChanges.get()

        if (stack.isEmpty()) {
            return
        }

        val capture =
            stack.removeLast()

        if (!changed) {
            return
        }

        capture.world
            .mutationJournal
            .record(
                ReplayBlockMutation(
                    capture.x,
                    capture.y,
                    capture.z,
                    capture.before,
                ),
            )
    }

    @JvmStatic
    fun beforeFillChunk(
        chunk: Chunk,
    ) {
        val world =
            chunk.worldObj as?
                ReplayWorld
                ?: return

        if (
            !world.mutationJournal
                .recording
        ) {
            return
        }

        chunkChanges.get()
            .addLast(
                ReplayChunkCapture(
                    world =
                    world,
                    chunk =
                    SnapshotCapture.captureChunk(
                        chunk,
                    ),
                    tileEntities =
                    captureChunkTileEntities(
                        world,
                        chunk.xPosition,
                        chunk.zPosition,
                    ),
                ),
            )
    }

    @JvmStatic
    fun afterFillChunk() {
        val stack =
            chunkChanges.get()

        if (stack.isEmpty()) {
            return
        }

        val capture =
            stack.removeLast()

        capture.world
            .mutationJournal
            .record(
                ReplayChunkStateMutation(
                    capture.chunk,
                    capture.tileEntities,
                ),
            )
    }

    private fun captureChunkTileEntities(
        world: ReplayWorld,
        chunkX: Int,
        chunkZ: Int,
    ): List<ReplayTileEntitySnapshot> = world.loadedTileEntityList
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

    private data class ReplayBlockCapture(
        val world: ReplayWorld,
        val x: Int,
        val y: Int,
        val z: Int,
        val before: ReplayBlockState,
    )

    private data class ReplayChunkCapture(
        val world: ReplayWorld,
        val chunk: ReplayChunkSnapshot,
        val tileEntities: List<ReplayTileEntitySnapshot>,
    )
}
