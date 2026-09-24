package me.yuhan8954.flashback.snapshot

data class ReplaySnapshot(
    val dimensionId: Int,
    val seed: Long,
    val worldTime: Long,
    val totalWorldTime: Long,
    val raining: Boolean,
    val thundering: Boolean,
    val rainStrength: Float,
    val thunderStrength: Float,
    val player: ReplayPlayerSnapshot,
    val chunks: List<ReplayChunkSnapshot>,
    val tileEntities: List<ReplayTileEntitySnapshot>,
    val entities: List<ReplayEntitySnapshot>,
)

data class ReplayPlayerSnapshot(
    val entityId: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val motionX: Double,
    val motionY: Double,
    val motionZ: Double,
)
