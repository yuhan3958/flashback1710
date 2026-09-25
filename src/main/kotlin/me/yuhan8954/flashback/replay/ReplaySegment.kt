package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotDelta

data class ReplaySegment(
    val startTimeNanos: Long,
    val endTimeNanos: Long,
    val packetIndex: Int,
    val snapshot: ReplaySnapshot,
)

class ReplaySegmentIndex(
    initialSnapshot: ReplaySnapshot,
    checkpoints: List<ReplayCheckpoint>,
    durationNanos: Long,
) {

    private val segments:
        List<ReplaySegment>

    init {
        val resolved =
            mutableListOf(
                ResolvedReplayCheckpoint(
                    timestampNanos = 0L,
                    packetIndex = 0,
                    snapshot = initialSnapshot,
                ),
            )

        var currentSnapshot =
            initialSnapshot

        checkpoints
            .sortedBy {
                it.timestampNanos
            }.forEach { checkpoint ->
                when (checkpoint) {
                    is ReplayCheckpoint.Full -> {
                        currentSnapshot =
                            checkpoint.snapshot
                    }

                    is ReplayCheckpoint.Delta -> {
                        currentSnapshot =
                            SnapshotDelta.apply(
                                currentSnapshot,
                                checkpoint.delta,
                            )
                    }
                }

                resolved +=
                    ResolvedReplayCheckpoint(
                        timestampNanos =
                        checkpoint.timestampNanos,
                        packetIndex =
                        checkpoint.packetIndex,
                        snapshot =
                        currentSnapshot,
                    )
            }

        segments =
            resolved.mapIndexed {
                    index,
                    checkpoint,
                ->
                ReplaySegment(
                    startTimeNanos =
                    checkpoint.timestampNanos,
                    endTimeNanos =
                    resolved.getOrNull(
                        index + 1,
                    )?.timestampNanos
                        ?: durationNanos,
                    packetIndex =
                    checkpoint.packetIndex,
                    snapshot =
                    checkpoint.snapshot,
                )
            }
    }

    fun find(
        targetTimeNanos: Long,
    ): ReplaySegment {
        var low =
            0

        var high =
            segments.lastIndex

        while (low <= high) {
            val middle =
                (low + high)
                    .ushr(
                        1,
                    )

            val segment =
                segments[middle]

            if (
                segment.startTimeNanos <=
                targetTimeNanos
            ) {
                if (
                    middle ==
                    segments.lastIndex ||
                    segments[middle + 1]
                        .startTimeNanos >
                    targetTimeNanos
                ) {
                    return segment
                }

                low =
                    middle + 1
            } else {
                high =
                    middle - 1
            }
        }

        return segments.first()
    }

    val size: Int
        get() =
            segments.size
}
