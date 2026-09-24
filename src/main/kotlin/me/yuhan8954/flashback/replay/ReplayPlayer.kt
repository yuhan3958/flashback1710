package me.yuhan8954.flashback.replay

import cpw.mods.fml.common.network.internal.FMLProxyPacket
import cpw.mods.fml.relauncher.Side
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

    private val clock =
        ReplayClock()

    private var session:
        ReplaySession? =
        null

    private var stopRequested =
        false

    var playing =
        false
        private set

    val paused: Boolean
        get() =
            playing &&
                clock.paused

    val speed: Double
        get() = clock.speed

    val currentTimeNanos: Long
        get() = clock.currentTimeNanos

    val processedPacketCount: Int
        get() = index

    val totalPacketCount: Int
        get() = packets.size

    val freeCameraActive: Boolean
        get() =
            session?.cameraController
                ?.state
                ?.active == true

    val cameraSpeed: Double?
        get() =
            session?.cameraController
                ?.state
                ?.movementSpeed

    fun play(file: File) {
        stop()

        val reader =
            ReplayReader(
                file,
            )

        packets =
            reader.packets

        session =
            ReplaySession(
                Minecraft.getMinecraft(),
            ).also {
                it.open(
                    reader.snapshot,
                )
            }

        index = 0

        clock.reset()

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

        currentSession.cameraController.tick()

        clock.update()

        while (
            index < packets.size &&
            packets[index]
                .timestampNanos <=
            clock.currentTimeNanos
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

    fun beginTick() {
        if (!playing) {
            return
        }

        session?.world?.beginReplayTick(
            clock.paused,
        )

        session?.cameraController?.beginTick()
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

        clock.stop()
    }

    fun pause(): Boolean {
        if (!playing) {
            return false
        }

        clock.pause()
        return true
    }

    fun resume(): Boolean {
        if (!playing) {
            return false
        }

        clock.resume()
        return true
    }

    fun togglePause(): Boolean {
        if (!playing) {
            return false
        }

        clock.togglePause()
        return true
    }

    fun setSpeed(speed: Double): Boolean {
        if (
            !playing ||
            !ReplayClock.isSupportedSpeed(
                speed,
            )
        ) {
            return false
        }

        clock.setSpeed(
            speed,
        )

        return true
    }

    fun step(): Boolean {
        if (
            !playing ||
            !clock.paused
        ) {
            return false
        }

        clock.step()

        session?.world?.requestStep()

        return true
    }

    fun enableFreeCamera(): Boolean {
        if (!playing) {
            return false
        }

        return session?.cameraController
            ?.enable() == true
    }

    fun disableFreeCamera(): Boolean {
        if (!playing) {
            return false
        }

        return session?.cameraController
            ?.disable() == true
    }

    fun setCameraSpeed(speed: Double): Boolean {
        if (!playing) {
            return false
        }

        return session?.cameraController
            ?.setMovementSpeed(
                speed,
            ) == true
    }

    fun handleMouseInput(
        deltaX: Int,
        deltaY: Int,
        wheelDelta: Int,
    ): Boolean =
        session?.cameraController
            ?.handleMouseInput(
                deltaX,
                deltaY,
                wheelDelta,
            ) == true

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
        ).apply {
            setTarget(
                Side.CLIENT,
            )
        }
    }
}
