package me.yuhan8954.flashback.recording

import cpw.mods.fml.common.network.internal.FMLProxyPacket
import io.netty.buffer.Unpooled
import me.yuhan8954.flashback.config.ReplayConfig
import me.yuhan8954.flashback.io.ReplayLibrary
import me.yuhan8954.flashback.io.ReplayWriter
import me.yuhan8954.flashback.replay.PacketFlow
import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotCapture
import me.yuhan8954.flashback.snapshot.SnapshotDelta
import net.minecraft.client.Minecraft
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import java.io.File

object ReplayRecorder {

    private var writer:
        ReplayWriter? =
        null

    var currentFile:
        File? =
        null
        private set

    private var startTime =
        0L

    private var packetCount =
        0

    private var checkpointSnapshot:
        ReplaySnapshot? =
        null

    private var nextDeltaCheckpointNanos =
        Long.MAX_VALUE

    private var nextFullCheckpointNanos =
        Long.MAX_VALUE

    @JvmStatic
    @Synchronized
    fun startNew(
        directory: File,
    ): File {
        val file =
            ReplayLibrary.createRecordingFile(
                directory,
            )

        start(
            file,
        )

        return file
    }

    @JvmStatic
    @Synchronized
    fun start(file: File) {
        stop()

        val snapshot =
            SnapshotCapture.capture(
                Minecraft.getMinecraft(),
            )

        writer =
            ReplayWriter(
                file,
                snapshot,
            )

        currentFile =
            file

        checkpointSnapshot =
            snapshot

        packetCount = 0

        startTime =
            System.nanoTime()

        nextDeltaCheckpointNanos =
            intervalNanos(
                ReplayConfig.checkpointIntervalSeconds,
            )

        nextFullCheckpointNanos =
            intervalNanos(
                ReplayConfig.checkpointAnchorIntervalSeconds,
            )
    }

    @JvmStatic
    @Synchronized
    fun stop() {
        writer?.close()
        writer = null
        currentFile = null
        checkpointSnapshot = null
        packetCount = 0
        nextDeltaCheckpointNanos = Long.MAX_VALUE
        nextFullCheckpointNanos = Long.MAX_VALUE
    }

    @JvmStatic
    @Synchronized
    fun tick() {
        val currentWriter =
            writer ?: return

        val elapsedNanos =
            System.nanoTime() -
                startTime

        val writeFullCheckpoint =
            elapsedNanos >=
                nextFullCheckpointNanos

        val writeDeltaCheckpoint =
            elapsedNanos >=
                nextDeltaCheckpointNanos

        if (
            !writeFullCheckpoint &&
            !writeDeltaCheckpoint
        ) {
            return
        }

        val snapshot =
            SnapshotCapture.capture(
                Minecraft.getMinecraft(),
            )

        if (writeFullCheckpoint) {
            currentWriter.writeFullCheckpoint(
                elapsedNanos,
                packetCount,
                snapshot,
            )

            checkpointSnapshot =
                snapshot

            nextFullCheckpointNanos =
                nextCheckpointTime(
                    elapsedNanos,
                    ReplayConfig
                        .checkpointAnchorIntervalSeconds,
                )

            nextDeltaCheckpointNanos =
                nextCheckpointTime(
                    elapsedNanos,
                    ReplayConfig
                        .checkpointIntervalSeconds,
                )

            return
        }

        val previousSnapshot =
            checkpointSnapshot
                ?: snapshot

        currentWriter.writeDeltaCheckpoint(
            elapsedNanos,
            packetCount,
            SnapshotDelta.create(
                previousSnapshot,
                snapshot,
            ),
        )

        checkpointSnapshot =
            snapshot

        nextDeltaCheckpointNanos =
            nextCheckpointTime(
                elapsedNanos,
                ReplayConfig
                    .checkpointIntervalSeconds,
            )
    }

    @JvmStatic
    @Synchronized
    fun record(packet: Packet) {
        record(
            packet,
            PacketFlow.CLIENTBOUND,
        )
    }

    @JvmStatic
    @Synchronized
    fun recordOutbound(packet: Packet) {
        if (
            packet !is C03PacketPlayer &&
            packet !is C09PacketHeldItemChange &&
            packet !is C0APacketAnimation &&
            packet !is C0BPacketEntityAction
        ) {
            return
        }

        record(
            packet,
            PacketFlow.SERVERBOUND,
        )
    }

    private fun record(
        packet: Packet,
        flow: PacketFlow,
    ) {
        val currentWriter =
            writer ?: return

        val byteBuf =
            Unpooled.buffer()

        val buffer =
            PacketBuffer(
                byteBuf,
            )

        try {
            val payload =
                if (
                    packet is FMLProxyPacket
                ) {
                    val packetPayload =
                        packet.payload()

                    ByteArray(
                        packetPayload.readableBytes(),
                    ).also {
                        packetPayload.getBytes(
                            packetPayload.readerIndex(),
                            it,
                        )
                    }
                } else {
                    packet.writePacketData(
                        buffer,
                    )

                    ByteArray(
                        buffer.readableBytes(),
                    ).also {
                        buffer.getBytes(
                            buffer.readerIndex(),
                            it,
                        )
                    }
                }

            val channel =
                if (
                    packet is FMLProxyPacket
                ) {
                    packet.channel()
                } else {
                    null
                }

            currentWriter.write(
                RecordedPacket(
                    timestampNanos =
                    System.nanoTime() -
                        startTime,
                    packetClass =
                    packet.javaClass.name,
                    payload =
                    payload,
                    channel =
                    channel,
                    flow =
                    flow,
                ),
            )

            packetCount++
        } catch (
            throwable: Throwable,
        ) {
            System.err.println(
                "[Flashback] Failed to record packet: " +
                    packet.javaClass.name,
            )

            throwable.printStackTrace()
        } finally {
            byteBuf.release()
        }
    }

    private fun intervalNanos(seconds: Int): Long = if (seconds <= 0) {
        Long.MAX_VALUE
    } else {
        seconds * 1_000_000_000L
    }

    private fun nextCheckpointTime(
        currentTimeNanos: Long,
        intervalSeconds: Int,
    ): Long {
        val intervalNanos =
            intervalNanos(
                intervalSeconds,
            )

        if (intervalNanos == Long.MAX_VALUE) {
            return Long.MAX_VALUE
        }

        return (
            currentTimeNanos /
                intervalNanos +
                1L
            ) *
            intervalNanos
    }
}
