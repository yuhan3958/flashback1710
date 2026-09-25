package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.ReplaySnapshotDelta
import me.yuhan8954.flashback.snapshot.SnapshotDeltaWriter
import me.yuhan8954.flashback.snapshot.SnapshotWriter
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class ReplayWriter(
    file: File,
    snapshot: ReplaySnapshot,
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

        SnapshotWriter.write(
            output,
            snapshot,
        )
    }

    @Synchronized
    fun write(packet: RecordedPacket) {
        output.writeByte(
            RECORD_PACKET,
        )

        writePacket(
            packet,
        )
    }

    @Synchronized
    fun writeFullCheckpoint(
        timestampNanos: Long,
        packetIndex: Int,
        snapshot: ReplaySnapshot,
    ) {
        output.writeByte(
            RECORD_FULL_CHECKPOINT,
        )

        output.writeLong(
            timestampNanos,
        )

        output.writeInt(
            packetIndex,
        )

        SnapshotWriter.write(
            output,
            snapshot,
        )
    }

    @Synchronized
    fun writeDeltaCheckpoint(
        timestampNanos: Long,
        packetIndex: Int,
        delta: ReplaySnapshotDelta,
    ) {
        output.writeByte(
            RECORD_DELTA_CHECKPOINT,
        )

        output.writeLong(
            timestampNanos,
        )

        output.writeInt(
            packetIndex,
        )

        SnapshotDeltaWriter.write(
            output,
            delta,
        )
    }

    private fun writePacket(packet: RecordedPacket) {
        val classBytes =
            packet.packetClass.toByteArray(
                Charsets.UTF_8,
            )

        output.writeLong(
            packet.timestampNanos,
        )

        output.writeByte(
            packet.flow.ordinal,
        )

        output.writeInt(
            classBytes.size,
        )

        output.write(
            classBytes,
        )

        val channel = packet.channel

        if (channel == null) {
            output.writeInt(-1)
        } else {
            val channelBytes =
                channel.toByteArray(
                    Charsets.UTF_8,
                )

            output.writeInt(
                channelBytes.size,
            )

            output.write(
                channelBytes,
            )
        }

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

        const val FORMAT_VERSION = 7

        const val RECORD_PACKET = 0
        const val RECORD_FULL_CHECKPOINT = 1
        const val RECORD_DELTA_CHECKPOINT = 2
    }
}
