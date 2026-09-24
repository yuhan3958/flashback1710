package me.yuhan8954.flashback.recording

import cpw.mods.fml.common.network.internal.FMLProxyPacket
import io.netty.buffer.Unpooled
import me.yuhan8954.flashback.io.ReplayWriter
import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.snapshot.SnapshotCapture
import net.minecraft.client.Minecraft
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import java.io.File

object ReplayRecorder {

    private var writer:
        ReplayWriter? =
        null

    private var startTime =
        0L

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

        startTime =
            System.nanoTime()
    }

    @JvmStatic
    @Synchronized
    fun stop() {
        writer?.close()

        writer = null
    }

    @JvmStatic
    @Synchronized
    fun record(packet: Packet) {
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
                ),
            )
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
}
