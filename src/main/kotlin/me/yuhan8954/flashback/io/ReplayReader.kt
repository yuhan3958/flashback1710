package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.PacketFlow
import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
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

    init {
        val result =
            mutableListOf<RecordedPacket>()

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
                version == ReplayWriter.FORMAT_VERSION,
            ) {
                "Unsupported replay version: $version"
            }

            loadedSnapshot =
                SnapshotReader.read(
                    input,
                )

            while (true) {
                try {
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

                    result +=
                        RecordedPacket(
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
                } catch (_: EOFException) {
                    break
                }
            }
        }

        snapshot =
            loadedSnapshot

        packets = result
    }
}
