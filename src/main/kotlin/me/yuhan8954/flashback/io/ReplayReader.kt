package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.PacketFlow
import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.replay.ReplayCheckpoint
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotDeltaReader
import me.yuhan8954.flashback.snapshot.SnapshotReader
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.util.zip.CRC32

class ReplayReader(
    file: File,
) {

    val snapshot: ReplaySnapshot

    val packets: List<RecordedPacket>

    val checkpoints: List<ReplayCheckpoint>

    val durationNanos: Long

    val metadata: ReplayMetadata?

    val status: ReplayReadStatus

    val cleanClose: Boolean
        get() =
            status ==
                ReplayReadStatus.CLEAN

    init {
        val loadedPackets =
            mutableListOf<RecordedPacket>()

        val loadedCheckpoints =
            mutableListOf<ReplayCheckpoint>()

        lateinit var loadedSnapshot:
            ReplaySnapshot

        var loadedMetadata:
            ReplayMetadata? =
            null

        var readStatus =
            ReplayReadStatus.LEGACY

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
                    version ==
                    ReplayWriter.LEGACY_FORMAT_VERSION ||
                    version ==
                    ReplayWriter.FORMAT_VERSION,
            ) {
                "Unsupported replay version: $version"
            }

            if (
                version ==
                ReplayWriter.FORMAT_VERSION
            ) {
                loadedMetadata =
                    readMetadata(
                        input,
                    )
            }

            loadedSnapshot =
                SnapshotReader.read(
                    input,
                )

            when (version) {
                6 ->
                    readVersion6(
                        input,
                        loadedPackets,
                    )

                ReplayWriter.LEGACY_FORMAT_VERSION ->
                    readVersion7(
                        input,
                        loadedPackets,
                        loadedCheckpoints,
                    )

                ReplayWriter.FORMAT_VERSION ->
                    readStatus =
                        readVersion8(
                            input,
                            loadedPackets,
                            loadedCheckpoints,
                        )
            }
        }

        snapshot =
            loadedSnapshot

        packets =
            loadedPackets

        checkpoints =
            loadedCheckpoints

        metadata =
            loadedMetadata

        status =
            readStatus

        durationNanos =
            maxOf(
                packets.lastOrNull()
                    ?.timestampNanos ?: 0L,
                checkpoints.lastOrNull()
                    ?.timestampNanos ?: 0L,
            )
    }

    private fun readVersion6(
        input: DataInputStream,
        packets: MutableList<RecordedPacket>,
    ) {
        while (true) {
            try {
                packets +=
                    readPacket(
                        input,
                    )
            } catch (_: EOFException) {
                break
            }
        }
    }

    private fun readVersion7(
        input: DataInputStream,
        packets: MutableList<RecordedPacket>,
        checkpoints: MutableList<ReplayCheckpoint>,
    ) {
        while (true) {
            try {
                when (
                    input.readUnsignedByte()
                ) {
                    ReplayWriter.RECORD_PACKET ->
                        packets +=
                            readPacket(
                                input,
                            )

                    ReplayWriter.RECORD_FULL_CHECKPOINT ->
                        checkpoints +=
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
                        checkpoints +=
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

    private fun readVersion8(
        input: DataInputStream,
        packets: MutableList<RecordedPacket>,
        checkpoints: MutableList<ReplayCheckpoint>,
    ): ReplayReadStatus {
        while (true) {
            val type =
                try {
                    input.readUnsignedByte()
                } catch (_: EOFException) {
                    return ReplayReadStatus.TRUNCATED
                }

            val payloadLength =
                try {
                    input.readInt()
                } catch (_: EOFException) {
                    return ReplayReadStatus.TRUNCATED
                }

            val expectedCrc =
                try {
                    input.readInt()
                } catch (_: EOFException) {
                    return ReplayReadStatus.TRUNCATED
                }

            if (
                payloadLength !in
                0..ReplayWriter.MAX_RECORD_BYTES
            ) {
                return ReplayReadStatus.CORRUPT
            }

            val payload =
                ByteArray(
                    payloadLength,
                )

            try {
                input.readFully(
                    payload,
                )
            } catch (_: EOFException) {
                return ReplayReadStatus.TRUNCATED
            }

            val actualCrc =
                CRC32().apply {
                    update(
                        type,
                    )
                    update(
                        payload,
                    )
                }.value.toInt()

            if (
                actualCrc !=
                expectedCrc
            ) {
                return ReplayReadStatus.CORRUPT
            }

            if (
                type ==
                ReplayWriter.RECORD_CLEAN_CLOSE
            ) {
                return if (
                    payload.isEmpty()
                ) {
                    ReplayReadStatus.CLEAN
                } else {
                    ReplayReadStatus.CORRUPT
                }
            }

            val record =
                DataInputStream(
                    ByteArrayInputStream(
                        payload,
                    ),
                )

            try {
                when (type) {
                    ReplayWriter.RECORD_PACKET ->
                        packets +=
                            readPacket(
                                record,
                            )

                    ReplayWriter.RECORD_FULL_CHECKPOINT ->
                        checkpoints +=
                            ReplayCheckpoint.Full(
                                timestampNanos =
                                record.readLong(),
                                packetIndex =
                                record.readInt(),
                                snapshot =
                                SnapshotReader.read(
                                    record,
                                ),
                            )

                    ReplayWriter.RECORD_DELTA_CHECKPOINT ->
                        checkpoints +=
                            ReplayCheckpoint.Delta(
                                timestampNanos =
                                record.readLong(),
                                packetIndex =
                                record.readInt(),
                                delta =
                                SnapshotDeltaReader.read(
                                    record,
                                ),
                            )

                    else ->
                        return ReplayReadStatus.CORRUPT
                }
            } catch (
                throwable: Throwable,
            ) {
                if (
                    throwable is
                    VirtualMachineError
                ) {
                    throw throwable
                }

                return ReplayReadStatus.CORRUPT
            }

            if (
                record.available() !=
                0
            ) {
                return ReplayReadStatus.CORRUPT
            }
        }
    }

    private fun readMetadata(
        input: DataInputStream,
    ): ReplayMetadata = ReplayMetadata(
        createdAtEpochMillis =
        input.readLong(),
        minecraftVersion =
        readString(
            input,
        ),
        flashbackVersion =
        readString(
            input,
        ),
        gtnhVersion =
        readNullableString(
            input,
        ),
        modFingerprint =
        readNullableString(
            input,
        ),
    )

    private fun readPacket(
        input: DataInputStream,
    ): RecordedPacket {
        val timestamp =
            input.readLong()

        val flowIndex =
            input.readUnsignedByte()

        require(
            flowIndex <
                PacketFlow.entries.size,
        ) {
            "Invalid packet flow: $flowIndex"
        }

        val flow =
            PacketFlow.entries[
                flowIndex,
            ]

        val classLength =
            input.readInt()

        require(
            classLength in
                1..ReplayWriter.MAX_TEXT_BYTES,
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
                    channelLength <=
                        ReplayWriter.MAX_TEXT_BYTES,
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
                0..ReplayWriter.MAX_PACKET_BYTES,
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

    private fun readString(
        input: DataInputStream,
    ): String {
        val length =
            input.readInt()

        require(
            length in
                0..ReplayWriter.MAX_TEXT_BYTES,
        ) {
            "Invalid replay text length: $length"
        }

        val bytes =
            ByteArray(
                length,
            )

        input.readFully(
            bytes,
        )

        return bytes.toString(
            Charsets.UTF_8,
        )
    }

    private fun readNullableString(
        input: DataInputStream,
    ): String? {
        val length =
            input.readInt()

        if (length < 0) {
            return null
        }

        require(
            length <=
                ReplayWriter.MAX_TEXT_BYTES,
        ) {
            "Invalid replay text length: $length"
        }

        val bytes =
            ByteArray(
                length,
            )

        input.readFully(
            bytes,
        )

        return bytes.toString(
            Charsets.UTF_8,
        )
    }
}
