package me.yuhan8954.flashback.replay

import io.netty.buffer.Unpooled
import me.yuhan8954.flashback.io.ReplayReader
import net.minecraft.client.Minecraft
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import java.io.File

object ReplayPlayer {

    private var packets:
        List<RecordedPacket> =
        emptyList()

    private var index = 0

    private var startTime = 0L

    private var session:
        ReplaySession? =
        null

    var playing = false
        private set

    fun play(file: File) {
        stop()

        packets =
            ReplayReader(file).packets

        session =
            ReplaySession(
                Minecraft.getMinecraft(),
            ).also {
                it.open()
            }

        index = 0

        startTime =
            System.nanoTime()

        playing = true
    }

    fun tick() {
        if (!playing) {
            return
        }

        val currentSession =
            session ?: return

        val elapsed =
            System.nanoTime() -
                startTime

        while (
            index < packets.size &&
            packets[index].timestampNanos <= elapsed
        ) {
            val recorded =
                packets[index]

            try {
                val packet =
                    decode(recorded)

                packet.processPacket(
                    currentSession.handler,
                )
            } catch (
                throwable: Throwable
            ) {
                System.err.println(
                    "[Flashback] Failed replay packet: " +
                        recorded.packetClass,
                )

                throwable.printStackTrace()
            }

            index++
        }

        if (index >= packets.size) {
            stop()
        }
    }

    fun stop() {
        playing = false

        session?.close()
        session = null

        packets = emptyList()
        index = 0
    }

    private fun decode(
        recorded: RecordedPacket,
    ): Packet {

        val clazz =
            Class.forName(
                recorded.packetClass,
            )

        val constructor =
            clazz.getDeclaredConstructor()

        constructor.isAccessible = true

        val packet =
            constructor.newInstance()
                as Packet

        val byteBuf =
            Unpooled.wrappedBuffer(
                recorded.payload,
            )

        try {
            packet.readPacketData(
                PacketBuffer(
                    byteBuf,
                ),
            )
        } finally {
            byteBuf.release()
        }

        return packet
    }
}
