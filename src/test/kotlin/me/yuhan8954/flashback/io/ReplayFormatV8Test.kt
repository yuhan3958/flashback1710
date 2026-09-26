package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.PacketFlow
import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.snapshot.ReplayInventorySnapshot
import me.yuhan8954.flashback.snapshot.ReplayPlayerSnapshot
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotReader
import me.yuhan8954.flashback.snapshot.SnapshotWriter
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayFormatV8Test {

    @Test
    fun `v8 round trip preserves packet metadata and clean close`() {
        val file =
            Files.createTempFile(
                "flashback-v8-roundtrip",
                ".fbr",
            ).toFile()

        val metadata =
            ReplayMetadata(
                createdAtEpochMillis = 123456789L,
                minecraftVersion = "1.7.10",
                flashbackVersion = "test-version",
                gtnhVersion = "2.8.0",
                modFingerprint = "abcdef",
            )

        ReplayWriter(
            file,
            snapshot(),
            metadata,
        ).use {
            it.write(
                RecordedPacket(
                    timestampNanos = 50_000_000L,
                    packetClass = "example.Packet",
                    payload = byteArrayOf(
                        1,
                        2,
                        3,
                        4,
                    ),
                    channel = "EXAMPLE",
                    flow = PacketFlow.CLIENTBOUND,
                ),
            )
        }

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.CLEAN,
            reader.status,
        )
        assertTrue(
            reader.cleanClose,
        )
        assertEquals(
            metadata,
            reader.metadata,
        )
        assertEquals(
            1,
            reader.packets.size,
        )

        val packet =
            reader.packets.single()

        assertEquals(
            50_000_000L,
            packet.timestampNanos,
        )
        assertEquals(
            "example.Packet",
            packet.packetClass,
        )
        assertEquals(
            "EXAMPLE",
            packet.channel,
        )
        assertEquals(
            PacketFlow.CLIENTBOUND,
            packet.flow,
        )
        assertContentEquals(
            byteArrayOf(
                1,
                2,
                3,
                4,
            ),
            packet.payload,
        )
    }

    @Test
    fun `null metadata fields round trip`() {
        val file =
            Files.createTempFile(
                "flashback-v8-null-metadata",
                ".fbr",
            ).toFile()

        ReplayWriter(
            file,
            snapshot(),
            ReplayMetadata(
                createdAtEpochMillis = 1L,
                minecraftVersion = "1.7.10",
                flashbackVersion = "test",
            ),
        ).close()

        val metadata =
            assertNotNull(
                ReplayReader(
                    file,
                ).metadata,
            )

        assertNull(
            metadata.gtnhVersion,
        )
        assertNull(
            metadata.modFingerprint,
        )
    }

    @Test
    fun `full and delta checkpoints round trip`() {
        val file =
            Files.createTempFile(
                "flashback-v8-checkpoints",
                ".fbr",
            ).toFile()

        val initial =
            snapshot(
                worldTime = 10L,
            )

        val full =
            snapshot(
                worldTime = 20L,
            )

        val target =
            snapshot(
                worldTime = 30L,
            )

        val delta =
            me.yuhan8954.flashback.snapshot.SnapshotDelta.create(
                full,
                target,
            )

        ReplayWriter(
            file,
            initial,
        ).use {
            it.writeFullCheckpoint(
                100L,
                4,
                full,
            )
            it.writeDeltaCheckpoint(
                200L,
                8,
                delta,
            )
        }

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.CLEAN,
            reader.status,
        )
        assertEquals(
            2,
            reader.checkpoints.size,
        )
        assertEquals(
            200L,
            reader.durationNanos,
        )
    }

    @Test
    fun `truncated record keeps preceding valid records`() {
        val file =
            Files.createTempFile(
                "flashback-v8-truncated",
                ".fbr",
            ).toFile()

        ReplayWriter(
            file,
            snapshot(),
        ).use {
            it.write(
                packet(
                    10L,
                    byteArrayOf(
                        1,
                    ),
                ),
            )
            it.write(
                packet(
                    20L,
                    ByteArray(
                        128,
                    ) {
                        it.toByte()
                    },
                ),
            )
        }

        val bytes =
            file.readBytes()

        val firstFrame =
            firstFrameOffset(
                bytes,
            )

        val firstFrameLength =
            frameTotalLength(
                bytes,
                firstFrame,
            )

        val secondFrame =
            firstFrame +
                firstFrameLength

        file.writeBytes(
            bytes.copyOf(
                secondFrame +
                    FRAME_HEADER_BYTES +
                    5,
            ),
        )

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.TRUNCATED,
            reader.status,
        )
        assertFalse(
            reader.cleanClose,
        )
        assertEquals(
            1,
            reader.packets.size,
        )
        assertEquals(
            10L,
            reader.packets.single()
                .timestampNanos,
        )
    }

    @Test
    fun `missing clean close is reported as truncated`() {
        val file =
            Files.createTempFile(
                "flashback-v8-no-close",
                ".fbr",
            ).toFile()

        ReplayWriter(
            file,
            snapshot(),
        ).use {
            it.write(
                packet(
                    10L,
                    byteArrayOf(
                        1,
                        2,
                    ),
                ),
            )
        }

        val bytes =
            file.readBytes()

        val firstFrame =
            firstFrameOffset(
                bytes,
            )

        file.writeBytes(
            bytes.copyOf(
                firstFrame +
                    frameTotalLength(
                        bytes,
                        firstFrame,
                    ),
            ),
        )

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.TRUNCATED,
            reader.status,
        )
        assertEquals(
            1,
            reader.packets.size,
        )
    }

    @Test
    fun `checksum mismatch stops at corrupted record`() {
        val file =
            Files.createTempFile(
                "flashback-v8-crc",
                ".fbr",
            ).toFile()

        ReplayWriter(
            file,
            snapshot(),
        ).use {
            it.write(
                packet(
                    10L,
                    byteArrayOf(
                        1,
                        2,
                        3,
                    ),
                ),
            )
        }

        val bytes =
            file.readBytes()

        val frame =
            firstFrameOffset(
                bytes,
            )

        bytes[
            frame +
                FRAME_HEADER_BYTES,
        ] =
            (
                bytes[
                    frame +
                        FRAME_HEADER_BYTES,
                ].toInt() xor
                    0x01
                ).toByte()

        file.writeBytes(
            bytes,
        )

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.CORRUPT,
            reader.status,
        )
        assertTrue(
            reader.packets.isEmpty(),
        )
    }

    @Test
    fun `oversized record length is rejected before allocation`() {
        val file =
            Files.createTempFile(
                "flashback-v8-length",
                ".fbr",
            ).toFile()

        ReplayWriter(
            file,
            snapshot(),
        ).use {
            it.write(
                packet(
                    10L,
                    byteArrayOf(
                        1,
                    ),
                ),
            )
        }

        val bytes =
            file.readBytes()

        val frame =
            firstFrameOffset(
                bytes,
            )

        writeInt(
            bytes,
            frame + 1,
            ReplayWriter.MAX_RECORD_BYTES +
                1,
        )

        file.writeBytes(
            bytes,
        )

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.CORRUPT,
            reader.status,
        )
        assertTrue(
            reader.packets.isEmpty(),
        )
    }
    @Test
    fun `v7 replay remains readable`() {
        val file =
            Files.createTempFile(
                "flashback-v7-compat",
                ".fbr",
            ).toFile()

        DataOutputStream(
            FileOutputStream(
                file,
            ),
        ).use { output ->
            output.writeInt(
                ReplayWriter.MAGIC,
            )
            output.writeInt(
                ReplayWriter.LEGACY_FORMAT_VERSION,
            )
            SnapshotWriter.write(
                output,
                snapshot(),
            )
            output.writeByte(
                ReplayWriter.RECORD_PACKET,
            )
            writeLegacyPacket(
                output,
                packet(
                    77L,
                    byteArrayOf(
                        7,
                    ),
                ),
            )
        }

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.LEGACY,
            reader.status,
        )
        assertNull(
            reader.metadata,
        )
        assertEquals(
            77L,
            reader.packets.single()
                .timestampNanos,
        )
    }

    @Test
    fun `v6 replay remains readable`() {
        val file =
            Files.createTempFile(
                "flashback-v6-compat",
                ".fbr",
            ).toFile()

        DataOutputStream(
            FileOutputStream(
                file,
            ),
        ).use { output ->
            output.writeInt(
                ReplayWriter.MAGIC,
            )
            output.writeInt(
                6,
            )
            SnapshotWriter.write(
                output,
                snapshot(),
            )
            writeLegacyPacket(
                output,
                packet(
                    88L,
                    byteArrayOf(
                        8,
                    ),
                ),
            )
        }

        val reader =
            ReplayReader(
                file,
            )

        assertEquals(
            ReplayReadStatus.LEGACY,
            reader.status,
        )
        assertEquals(
            88L,
            reader.packets.single()
                .timestampNanos,
        )
    }

    @Test
    fun `writer close is idempotent`() {
        val file =
            Files.createTempFile(
                "flashback-v8-close",
                ".fbr",
            ).toFile()

        val writer =
            ReplayWriter(
                file,
                snapshot(),
            )

        writer.close()
        writer.close()

        assertEquals(
            ReplayReadStatus.CLEAN,
            ReplayReader(
                file,
            ).status,
        )
    }

    @Test
    fun `writer rejects writes after close`() {
        val file =
            Files.createTempFile(
                "flashback-v8-closed-write",
                ".fbr",
            ).toFile()

        val writer =
            ReplayWriter(
                file,
                snapshot(),
            )

        writer.close()

        assertFailsWith<IllegalStateException> {
            writer.write(
                packet(
                    1L,
                    byteArrayOf(),
                ),
            )
        }
    }
    private fun writeLegacyPacket(
        output: DataOutputStream,
        packet: RecordedPacket,
    ) {
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

        val channel =
            packet.channel

        if (channel == null) {
            output.writeInt(
                -1,
            )
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

    private fun packet(
        timestampNanos: Long,
        payload: ByteArray,
    ): RecordedPacket = RecordedPacket(
        timestampNanos = timestampNanos,
        packetClass = "example.Packet",
        payload = payload,
    )

    private fun snapshot(
        worldTime: Long = 0L,
    ): ReplaySnapshot = ReplaySnapshot(
        dimensionId = 0,
        seed = 123L,
        worldTime = worldTime,
        totalWorldTime = worldTime,
        raining = false,
        thundering = false,
        rainStrength = 0.0f,
        thunderStrength = 0.0f,
        player = ReplayPlayerSnapshot(
            entityId = 1,
            profileId = null,
            profileName = "Tester",
            x = 0.0,
            y = 64.0,
            z = 0.0,
            yaw = 0.0f,
            pitch = 0.0f,
            motionX = 0.0,
            motionY = 0.0,
            motionZ = 0.0,
            inventory = ReplayInventorySnapshot(
                selectedSlot = 0,
                mainInventory = emptyList(),
                armorInventory = emptyList(),
            ),
        ),
        chunks = emptyList(),
        tileEntities = emptyList(),
        entities = emptyList(),
    )

    private fun firstFrameOffset(
        bytes: ByteArray,
    ): Int {
        val byteInput =
            ByteArrayInputStream(
                bytes,
            )

        val input =
            DataInputStream(
                byteInput,
            )

        assertEquals(
            ReplayWriter.MAGIC,
            input.readInt(),
        )
        assertEquals(
            ReplayWriter.FORMAT_VERSION,
            input.readInt(),
        )

        input.readLong()
        skipString(
            input,
        )
        skipString(
            input,
        )
        skipNullableString(
            input,
        )
        skipNullableString(
            input,
        )

        SnapshotReader.read(
            input,
        )

        return bytes.size -
            byteInput.available()
    }

    private fun skipString(
        input: DataInputStream,
    ) {
        val length =
            input.readInt()

        input.skipBytes(
            length,
        )
    }

    private fun skipNullableString(
        input: DataInputStream,
    ) {
        val length =
            input.readInt()

        if (length >= 0) {
            input.skipBytes(
                length,
            )
        }
    }

    private fun frameTotalLength(
        bytes: ByteArray,
        offset: Int,
    ): Int = FRAME_HEADER_BYTES +
        readInt(
            bytes,
            offset + 1,
        )

    private fun readInt(
        bytes: ByteArray,
        offset: Int,
    ): Int {
        val first =
            (
                bytes[offset].toInt() and
                    0xff
                ) shl 24

        val second =
            (
                bytes[offset + 1].toInt() and
                    0xff
                ) shl 16

        val third =
            (
                bytes[offset + 2].toInt() and
                    0xff
                ) shl 8

        val fourth =
            bytes[offset + 3].toInt() and
                0xff

        return first or
            second or
            third or
            fourth
    }

    private fun writeInt(
        bytes: ByteArray,
        offset: Int,
        value: Int,
    ) {
        bytes[offset] =
            (
                value ushr 24
                ).toByte()
        bytes[offset + 1] =
            (
                value ushr 16
                ).toByte()
        bytes[offset + 2] =
            (
                value ushr 8
                ).toByte()
        bytes[offset + 3] =
            value.toByte()
    }

    companion object {

        private const val FRAME_HEADER_BYTES =
            1 + 4 + 4
    }
}
