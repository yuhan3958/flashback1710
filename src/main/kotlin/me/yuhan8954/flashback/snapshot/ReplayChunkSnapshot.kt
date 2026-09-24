package me.yuhan8954.flashback.snapshot

data class ReplayChunkSnapshot(
    val chunkX: Int,
    val chunkZ: Int,
    val blockIds: IntArray,
    val metadata: ByteArray,
    val biomes: ByteArray,
)
