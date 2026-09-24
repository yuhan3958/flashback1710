package me.yuhan8954.flashback.replay

import cpw.mods.fml.common.network.internal.FMLProxyPacket
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

    private var index =
        0

    private var startTime =
        0L

    private var session:
        ReplaySession? =
        null

    private var stopRequested =
        false

    var playing =
        false
        private set

    fun play(file: File) {
        stop()

        packets =
            ReplayReader(
                file,
            ).packets

        session =
            ReplaySession(
                Minecraft.getMinecraft(),
            ).also {
                it.open()
            }

        index = 0

        startTime =
            System.nanoTime()

        stopRequested =
            false

        playing =
            true

        println(
            "[Flashback] Playback started: " +
                "${packets.size} packets",
        )
    }

    fun tick() {
        if (stopRequested) {
            stopRequested =
                false

            stop()

            return
        }

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
            packets[index]
                .timestampNanos <= elapsed
        ) {
            val recorded =
                packets[index]

            try {
                val packet =
                    decode(
                        recorded,
                    )

                packet.processPacket(
                    currentSession.handler,
                )
            } catch (
                throwable: Throwable,
            ) {
                System.err.println(
                    "[Flashback] Failed replay packet: " +
                        recorded.packetClass,
                )

                throwable.printStackTrace()
            }

            index++
        }

        if (
            index >= packets.size
        ) {
            stopRequested =
                true
        }
    }

    fun stop() {
        playing =
            false

        stopRequested =
            false

        session?.close()

        session =
            null

        packets =
            emptyList()

        index =
            0
    }

    private fun decode(
        recorded: RecordedPacket,
    ): Packet {
        if (
            recorded.packetClass ==
            FMLProxyPacket::class.java.name
        ) {
            return decodeFmlProxyPacket(
                recorded,
            )
        }

        val clazz =
            Class.forName(
                recorded.packetClass,
            )

        val constructor =
            clazz.getDeclaredConstructor()

        constructor.isAccessible =
            true

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

    private fun decodeFmlProxyPacket(
        recorded: RecordedPacket,
    ): Packet {
        val channel =
            requireNotNull(
                recorded.channel,
            ) {
                "FMLProxyPacket missing channel"
            }

        val payload =
            Unpooled.wrappedBuffer(
                recorded.payload,
            )

        return FMLProxyPacket(
            payload,
            channel,
        )
    }
}
