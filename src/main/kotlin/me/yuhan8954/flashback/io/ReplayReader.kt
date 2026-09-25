package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.PacketFlow
import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.replay.ReplayCheckpoint
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotDeltaReader
import me.yuhan8954.flashback.snapshot.SnapshotReader
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream

class ReplayReader(
    file: File,
) {

    val snapshot: ReplaySnapshot

    val packets: List<RecordedPacket>

    val checkpoints: List<ReplayCheckpoint>

    val durationNanos: Long

    init {
        val loadedPackets =
            mutableListOf<RecordedPacket>()

        val loadedCheckpoints =
            mutableListOf<ReplayCheckpoint>()

        lateinit var loadedSnapshot:
            ReplaySnapshot

        DataInputStream(
            BufferedInputStream(
                FileInputStream(file),
            ),
        ).use { input ->

            val magic =
                input.readInt()

            require(
                magic == ReplayWriter.MAGIC,
            ) {
                "Invalid Flashback replay"
            }

            val version =
                input.readInt()

            require(
                version == 6 ||
                    version == ReplayWriter.FORMAT_VERSION,
            ) {
                "Unsupported replay version: $version"
            }

            loadedSnapshot =
                SnapshotReader.read(
                    input,
                )

            if (version == 6) {
                while (true) {
                    try {
                        loadedPackets +=
                            readPacket(
                                input,
                            )
                    } catch (_: EOFException) {
                        break
                    }
                }
            } else {
                while (true) {
                    try {
                        when (
                            input.readUnsignedByte()
                        ) {
                            ReplayWriter.RECORD_PACKET ->
                                loadedPackets +=
                                    readPacket(
                                        input,
                                    )

                            ReplayWriter.RECORD_FULL_CHECKPOINT ->
                                loadedCheckpoints +=
                                    ReplayCheckpoint.Full(
                                        timestampNanos =
                                        input.readLong(),
                                        packetIndex =
                                        input.readInt(),
                                        snapshot =
                                        SnapshotReader.read(
                                            input,
                                        ),
                                    )

                            ReplayWriter.RECORD_DELTA_CHECKPOINT ->
                                loadedCheckpoints +=
                                    ReplayCheckpoint.Delta(
                                        timestampNanos =
                                        input.readLong(),
                                        packetIndex =
                                        input.readInt(),
                                        delta =
                                        SnapshotDeltaReader.read(
                                            input,
                                        ),
                                    )

                            else ->
                                error(
                                    "Unknown replay record type",
                                )
                        }
                    } catch (_: EOFException) {
                        break
                    }
                }
            }
        }

        snapshot =
            loadedSnapshot

        packets =
            loadedPackets

        checkpoints =
            loadedCheckpoints

        durationNanos =
            maxOf(
                packets.lastOrNull()
                    ?.timestampNanos ?: 0L,
                checkpoints.lastOrNull()
                    ?.timestampNanos ?: 0L,
            )
    }

    private fun readPacket(
        input: DataInputStream,
    ): RecordedPacket {
        val timestamp =
            input.readLong()

        val flow =
            PacketFlow.entries[
                input.readUnsignedByte(),
            ]

        val classLength =
            input.readInt()

        require(
            classLength in 1..4096,
        )

        val classBytes =
            ByteArray(
                classLength,
            )

        input.readFully(
            classBytes,
        )

        val channelLength =
            input.readInt()

        val channel =
            if (channelLength < 0) {
                null
            } else {
                require(
                    channelLength <= 4096,
                )

                val channelBytes =
                    ByteArray(
                        channelLength,
                    )

                input.readFully(
                    channelBytes,
                )

                channelBytes.toString(
                    Charsets.UTF_8,
                )
            }

        val payloadLength =
            input.readInt()

        require(
            payloadLength in
                0..64 * 1024 * 1024,
        )

        val payload =
            ByteArray(
                payloadLength,
            )

        input.readFully(
            payload,
        )

        return RecordedPacket(
            timestampNanos =
            timestamp,
            packetClass =
            classBytes.toString(
                Charsets.UTF_8,
            ),
            payload =
            payload,
            channel =
            channel,
            flow =
            flow,
        )
    }
}
