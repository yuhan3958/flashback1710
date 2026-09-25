package me.yuhan8954.flashback.replay

import cpw.mods.fml.common.network.internal.FMLProxyPacket
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotDelta

data class ReplayBootstrapBundle(
    val startPacketIndex: Int,
    val endPacketIndex: Int,
)

data class ReplaySegment(
    val startTimeNanos: Long,
    val endTimeNanos: Long,
    val packetIndex: Int,
    val snapshot: ReplaySnapshot,
    val bootstrap: ReplayBootstrapBundle,
)

class ReplaySegmentIndex(
    initialSnapshot: ReplaySnapshot,
    checkpoints: List<ReplayCheckpoint>,
    packets: List<RecordedPacket>,
    durationNanos: Long,
) {

    private val segments:
        List<ReplaySegment>

    init {
        val resolved =
            mutableListOf(
                ResolvedSegmentState(
                    timestampNanos = 0L,
                    packetIndex = 0,
                    snapshot = initialSnapshot,
                    bootstrapStartPacketIndex = 0,
                ),
            )

        var currentSnapshot =
            initialSnapshot

        var bootstrapStartPacketIndex =
            0

        checkpoints
            .sortedBy {
                it.timestampNanos
            }.forEach { checkpoint ->
                when (checkpoint) {
                    is ReplayCheckpoint.Full -> {
                        currentSnapshot =
                            checkpoint.snapshot

                        bootstrapStartPacketIndex =
                            checkpoint.packetIndex
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
                    ResolvedSegmentState(
                        timestampNanos =
                        checkpoint.timestampNanos,
                        packetIndex =
                        checkpoint.packetIndex,
                        snapshot =
                        currentSnapshot,
                        bootstrapStartPacketIndex =
                        bootstrapStartPacketIndex,
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
                    bootstrap =
                    ReplayBootstrapBundle(
                        startPacketIndex =
                        checkpoint.bootstrapStartPacketIndex
                            .coerceIn(
                                0,
                                packets.size,
                            ),
                        endPacketIndex =
                        checkpoint.packetIndex
                            .coerceIn(
                                0,
                                packets.size,
                            ),
                    ),
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

    private data class ResolvedSegmentState(
        val timestampNanos: Long,
        val packetIndex: Int,
        val snapshot: ReplaySnapshot,
        val bootstrapStartPacketIndex: Int,
    )
}

object ReplayBootstrapPolicy {

    fun shouldReplay(
        packet: RecordedPacket,
    ): Boolean = packet.flow ==
        PacketFlow.CLIENTBOUND &&
        packet.packetClass ==
        FMLProxyPacket::class.java.name
}
