package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.RecordedPacket
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class ReplayWriter(
    file: File,
) : Closeable {

    private val output =
        DataOutputStream(
            BufferedOutputStream(
                FileOutputStream(file),
            ),
        )

    init {
        file.parentFile?.mkdirs()

        output.writeInt(MAGIC)
        output.writeInt(FORMAT_VERSION)
    }

    @Synchronized
    fun write(packet: RecordedPacket) {
        val classBytes =
            packet.packetClass.toByteArray(
                Charsets.UTF_8,
            )

        output.writeLong(
            packet.timestampNanos,
        )

        output.writeInt(
            classBytes.size,
        )

        output.write(
            classBytes,
        )

        output.writeInt(
            packet.payload.size,
        )

        output.write(
            packet.payload,
        )
    }

    override fun close() {
        output.flush()
        output.close()
    }

    companion object {
        const val MAGIC = 0x46425231
        const val FORMAT_VERSION = 1
    }
}
