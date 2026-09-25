package me.yuhan8954.flashback.snapshot

import java.io.DataInput

object SnapshotDeltaReader {

    private const val MAX_REMOVED_CHUNKS = 16384
    private const val MAX_REMOVED_TILE_ENTITIES = 1_000_000
    private const val MAX_REMOVED_ENTITIES = 1_000_000

    fun read(input: DataInput): ReplaySnapshotDelta {
        val snapshot =
            SnapshotReader.read(
                input,
            )

        val removedChunkCount =
            input.readInt()

        require(
            removedChunkCount in 0..MAX_REMOVED_CHUNKS,
        )

        val removedChunks =
            List(
                removedChunkCount,
            ) {
                ReplayChunkPosition(
                    input.readInt(),
                    input.readInt(),
                )
            }

        val removedTileEntityCount =
            input.readInt()

        require(
            removedTileEntityCount in
                0..MAX_REMOVED_TILE_ENTITIES,
        )

        val removedTileEntities =
            List(
                removedTileEntityCount,
            ) {
                ReplayBlockPosition(
                    input.readInt(),
                    input.readInt(),
                    input.readInt(),
                )
            }

        val removedEntityCount =
            input.readInt()

        require(
            removedEntityCount in
                0..MAX_REMOVED_ENTITIES,
        )

        val removedEntityIds =
            IntArray(
                removedEntityCount,
            ) {
                input.readInt()
            }

        return ReplaySnapshotDelta(
            snapshot = snapshot,
            removedChunks = removedChunks,
            removedTileEntities = removedTileEntities,
            removedEntityIds = removedEntityIds,
        )
    }
}
