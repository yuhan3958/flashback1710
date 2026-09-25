package me.yuhan8954.flashback.snapshot

data class ReplaySnapshotDelta(
    val snapshot: ReplaySnapshot,
    val removedChunks: List<ReplayChunkPosition>,
    val removedTileEntities: List<ReplayBlockPosition>,
    val removedEntityIds: IntArray,
)

data class ReplayChunkPosition(
    val chunkX: Int,
    val chunkZ: Int,
)

data class ReplayBlockPosition(
    val x: Int,
    val y: Int,
    val z: Int,
)
