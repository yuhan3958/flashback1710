package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.ReplaySnapshotDelta
import me.yuhan8954.flashback.snapshot.SnapshotDelta

sealed class ReplayCheckpoint {

    abstract val timestampNanos: Long
    abstract val packetIndex: Int

    data class Full(
        override val timestampNanos: Long,
        override val packetIndex: Int,
        val snapshot: ReplaySnapshot,
    ) : ReplayCheckpoint()

    data class Delta(
        override val timestampNanos: Long,
        override val packetIndex: Int,
        val delta: ReplaySnapshotDelta,
    ) : ReplayCheckpoint()
}

data class ResolvedReplayCheckpoint(
    val timestampNanos: Long,
    val packetIndex: Int,
    val snapshot: ReplaySnapshot,
)

object ReplayCheckpointResolver {

    fun resolve(
        initialSnapshot: ReplaySnapshot,
        checkpoints: List<ReplayCheckpoint>,
        targetTimeNanos: Long,
    ): ResolvedReplayCheckpoint {
        val anchor =
            checkpoints.asSequence()
                .filterIsInstance<ReplayCheckpoint.Full>()
                .filter {
                    it.timestampNanos <= targetTimeNanos
                }.lastOrNull()

        var snapshot =
            anchor?.snapshot
                ?: initialSnapshot

        var timestampNanos =
            anchor?.timestampNanos
                ?: 0L

        var packetIndex =
            anchor?.packetIndex
                ?: 0

        checkpoints.asSequence()
            .filterIsInstance<ReplayCheckpoint.Delta>()
            .filter {
                it.timestampNanos > timestampNanos &&
                    it.timestampNanos <= targetTimeNanos
            }.forEach {
                snapshot =
                    SnapshotDelta.apply(
                        snapshot,
                        it.delta,
                    )

                timestampNanos =
                    it.timestampNanos

                packetIndex =
                    it.packetIndex
            }

        return ResolvedReplayCheckpoint(
            timestampNanos,
            packetIndex,
            snapshot,
        )
    }
}
