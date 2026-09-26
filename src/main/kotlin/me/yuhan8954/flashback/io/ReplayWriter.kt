package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.ReplaySnapshotDelta
import me.yuhan8954.flashback.snapshot.SnapshotDeltaWriter
import me.yuhan8954.flashback.snapshot.SnapshotWriter
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32

class ReplayWriter(
    file: File,
    snapshot: ReplaySnapshot,
    metadata: ReplayMetadata = ReplayMetadata.current(),
) : Closeable {

    private val output: DataOutputStream

    private var closed = false

    init {
        file.parentFile?.mkdirs()

        output =
            DataOutputStream(
                BufferedOutputStream(
                    FileOutputStream(file),
                ),
            )

        output.writeInt(MAGIC)
        output.writeInt(FORMAT_VERSION)

        writeMetadata(
            metadata,
        )

        SnapshotWriter.write(
            output,
            snapshot,
        )
    }

    @Synchronized
    fun write(packet: RecordedPacket) {
        ensureOpen()

        writeRecord(
            RECORD_PACKET,
        ) { record ->
            writePacket(
                record,
                packet,
            )
        }
    }

    @Synchronized
    fun writeFullCheckpoint(
        timestampNanos: Long,
        packetIndex: Int,
        snapshot: ReplaySnapshot,
    ) {
        ensureOpen()

        writeRecord(
            RECORD_FULL_CHECKPOINT,
        ) { record ->
            record.writeLong(
                timestampNanos,
            )
            record.writeInt(
                packetIndex,
            )
            SnapshotWriter.write(
                record,
                snapshot,
            )
        }
    }

    @Synchronized
    fun writeDeltaCheckpoint(
        timestampNanos: Long,
        packetIndex: Int,
        delta: ReplaySnapshotDelta,
    ) {
        ensureOpen()

        writeRecord(
            RECORD_DELTA_CHECKPOINT,
        ) { record ->
            record.writeLong(
                timestampNanos,
            )
            record.writeInt(
                packetIndex,
            )
            SnapshotDeltaWriter.write(
                record,
                delta,
            )
        }
    }

    private fun writeRecord(
        type: Int,
        writer: (DataOutputStream) -> Unit = {},
    ) {
        val payload =
            ByteArrayOutputStream()
                .use { bytes ->
                    DataOutputStream(
                        bytes,
                    ).use {
                        writer(
                            it,
                        )
                        it.flush()
                    }
                    bytes.toByteArray()
                }

        require(
            payload.size <= MAX_RECORD_BYTES,
        ) {
            "Replay record is too large: ${payload.size}"
        }

        val crc =
            CRC32().apply {
                update(
                    type,
                )
                update(
                    payload,
                )
            }.value.toInt()

        output.writeByte(
            type,
        )
        output.writeInt(
            payload.size,
        )
        output.writeInt(
            crc,
        )
        output.write(
            payload,
        )
    }

    private fun writePacket(
        record: DataOutputStream,
        packet: RecordedPacket,
    ) {
        val classBytes =
            packet.packetClass.toByteArray(
                Charsets.UTF_8,
            )

        require(
            classBytes.size in 1..MAX_TEXT_BYTES,
        ) {
            "Invalid packet class length: ${classBytes.size}"
        }

        record.writeLong(
            packet.timestampNanos,
        )
        record.writeByte(
            packet.flow.ordinal,
        )
        record.writeInt(
            classBytes.size,
        )
        record.write(
            classBytes,
        )

        val channel =
            packet.channel

        if (channel == null) {
            record.writeInt(
                -1,
            )
        } else {
            val channelBytes =
                channel.toByteArray(
                    Charsets.UTF_8,
                )

            require(
                channelBytes.size <= MAX_TEXT_BYTES,
            ) {
                "Invalid packet channel length: ${channelBytes.size}"
            }

            record.writeInt(
                channelBytes.size,
            )
            record.write(
                channelBytes,
            )
        }

        require(
            packet.payload.size <= MAX_PACKET_BYTES,
        ) {
            "Packet payload is too large: ${packet.payload.size}"
        }

        record.writeInt(
            packet.payload.size,
        )
        record.write(
            packet.payload,
        )
    }

    private fun writeMetadata(
        metadata: ReplayMetadata,
    ) {
        output.writeLong(
            metadata.createdAtEpochMillis,
        )
        writeString(
            output,
            metadata.minecraftVersion,
        )
        writeString(
            output,
            metadata.flashbackVersion,
        )
        writeNullableString(
            output,
            metadata.gtnhVersion,
        )
        writeNullableString(
            output,
            metadata.modFingerprint,
        )
    }

    override fun close() {
        synchronized(this) {
            if (closed) {
                return
            }

            writeRecord(
                RECORD_CLEAN_CLOSE,
            )
            output.flush()
            output.close()
            closed = true
        }
    }

    private fun ensureOpen() {
        check(
            !closed,
        ) {
            "ReplayWriter is already closed"
        }
    }

    companion object {

        const val MAGIC = 0x46425231

        const val FORMAT_VERSION = 8
        const val LEGACY_FORMAT_VERSION = 7

        const val RECORD_PACKET = 0
        const val RECORD_FULL_CHECKPOINT = 1
        const val RECORD_DELTA_CHECKPOINT = 2
        const val RECORD_CLEAN_CLOSE = 3

        const val MAX_RECORD_BYTES = 64 * 1024 * 1024
        const val MAX_PACKET_BYTES = 64 * 1024 * 1024
        const val MAX_TEXT_BYTES = 4096

        internal fun writeString(
            output: DataOutputStream,
            value: String,
        ) {
            val bytes =
                value.toByteArray(
                    Charsets.UTF_8,
                )

            require(
                bytes.size <= MAX_TEXT_BYTES,
            ) {
                "Replay text field is too large: ${bytes.size}"
            }

            output.writeInt(
                bytes.size,
            )
            output.write(
                bytes,
            )
        }

        internal fun writeNullableString(
            output: DataOutputStream,
            value: String?,
        ) {
            if (value == null) {
                output.writeInt(
                    -1,
                )
            } else {
                writeString(
                    output,
                    value,
                )
            }
        }
    }
}
