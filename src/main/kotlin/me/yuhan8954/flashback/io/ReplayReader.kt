package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.RecordedPacket
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream

class ReplayReader(
    file: File,
) {

    val packets: List<RecordedPacket>

    init {
        val result = mutableListOf<RecordedPacket>()

        DataInputStream(
            BufferedInputStream(
                FileInputStream(file),
            ),
        ).use { input ->

            val magic = input.readInt()

            require(magic == ReplayWriter.MAGIC) {
                "Invalid Flashback replay"
            }

            val version = input.readInt()

            require(version == ReplayWriter.FORMAT_VERSION) {
                "Unsupported replay version: $version"
            }

            while (true) {
                try {
                    val timestamp = input.readLong()

                    val classLength = input.readInt()
                    require(classLength in 1..4096)

                    val classBytes = ByteArray(classLength)
                    input.readFully(classBytes)

                    val payloadLength = input.readInt()

                    require(
                        payloadLength in 0..64 * 1024 * 1024
                    )

                    val payload = ByteArray(payloadLength)
                    input.readFully(payload)

                    result += RecordedPacket(
                        timestampNanos = timestamp,
                        packetClass = classBytes.toString(
                            Charsets.UTF_8,
                        ),
                        payload = payload,
                    )
                } catch (_: EOFException) {
                    break
                }
            }
        }

        packets = result
    }
}
