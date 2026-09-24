package me.yuhan8954.flashback.recording

import io.netty.buffer.Unpooled
import me.yuhan8954.flashback.io.ReplayWriter
import me.yuhan8954.flashback.replay.RecordedPacket
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import java.io.File

object ReplayRecorder {

    private var writer: ReplayWriter? = null
    private var startTime = 0L

    @JvmStatic
    @Synchronized
    fun start(file: File) {
        stop()

        startTime = System.nanoTime()
        writer = ReplayWriter(file)
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
        val writer =
            writer ?: return

        val byteBuf =
            Unpooled.buffer()

        val buffer =
            PacketBuffer(byteBuf)

        try {
            packet.writePacketData(
                buffer,
            )

            val payload =
                ByteArray(
                    buffer.readableBytes(),
                )

            buffer.getBytes(
                buffer.readerIndex(),
                payload,
            )

            writer.write(
                RecordedPacket(
                    timestampNanos =
                        System.nanoTime() -
                            startTime,
                    packetClass =
                        packet.javaClass.name,
                    payload =
                        payload,
                ),
            )
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        } finally {
            byteBuf.release()
        }
    }
}
