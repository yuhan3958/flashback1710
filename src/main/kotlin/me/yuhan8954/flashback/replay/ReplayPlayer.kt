package me.yuhan8954.flashback.replay

import cpw.mods.fml.common.network.internal.FMLProxyPacket
import cpw.mods.fml.relauncher.Side
import io.netty.buffer.Unpooled
import me.yuhan8954.flashback.io.ReplayReader
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.ui.ReplayUiController
import net.minecraft.client.Minecraft
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import java.io.File

object ReplayPlayer {

    private var packets:
        List<RecordedPacket> =
        emptyList()

    private var checkpoints:
        List<ReplayCheckpoint> =
        emptyList()

    private var snapshot: ReplaySnapshot? = null

    private var index =
        0

    private val clock =
        ReplayClock()

    private var session:
        ReplaySession? =
        null

    private var stopRequested =
        false

    private var durationNanos =
        0L

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

    val totalDurationNanos: Long
        get() = durationNanos

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

        snapshot = reader.snapshot

        packets =
            reader.packets

        checkpoints =
            reader.checkpoints

        durationNanos =
            reader.durationNanos

        clock.reset()

        session =
            ReplaySession(
                Minecraft.getMinecraft(),
            ).also {
                it.open(
                    reader.snapshot,
                )
            }

        index = 0

        stopRequested =
            false

        playing =
            true

        ReplayUiController.open()

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

        session?.cameraController?.tick()

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

        clock.update()
        processAvailablePackets()

        session?.world?.beginReplayTick(
            clock.currentTimeNanos,
        )

        session?.cameraController?.beginTick()
    }

    fun stop() {
        playing =
            false

        stopRequested =
            false

        ReplayUiController.close()

        session?.close()

        session =
            null

        snapshot = null

        packets =
            emptyList()

        checkpoints =
            emptyList()

        index =
            0

        durationNanos =
            0L

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

    fun seek(timeNanos: Long): Boolean {
        if (!playing) {
            return false
        }

        val currentSession = session ?: return false
        val initialSnapshot = snapshot ?: return false
        val targetTimeNanos = timeNanos.coerceIn(0L, durationNanos)

        val checkpoint =
            ReplayCheckpointResolver.resolve(
                initialSnapshot,
                checkpoints,
                targetTimeNanos,
            )

        if (
            targetTimeNanos < clock.currentTimeNanos ||
            checkpoint.timestampNanos >
            clock.currentTimeNanos
        ) {
            currentSession.reset(
                checkpoint.snapshot,
            )

            index =
                checkpoint.packetIndex
                    .coerceIn(
                        0,
                        packets.size,
                    )

            clock.seek(
                checkpoint.timestampNanos,
            )

            currentSession.world.seekReplayTime(
                checkpoint.timestampNanos,
            )
        }

        fastForwardTo(
            currentSession,
            targetTimeNanos,
        )

        stopRequested = false

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
    ): Boolean = session?.cameraController
        ?.handleMouseInput(
            deltaX,
            deltaY,
            wheelDelta,
        ) == true

    fun handleUiCameraInput(
        deltaX: Int,
        deltaY: Int,
    ): Boolean = session?.cameraController
        ?.handleUiMouseInput(
            deltaX,
            deltaY,
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

    private fun processAvailablePackets() {
        val currentSession =
            session ?: return

        while (
            index < packets.size &&
            packets[index]
                .timestampNanos <=
            clock.currentTimeNanos
        ) {
            processRecordedPacket(
                currentSession,
                packets[index],
            )

            index++
        }
    }

    private fun fastForwardTo(
        currentSession: ReplaySession,
        targetTimeNanos: Long,
    ) {
        while (
            index < packets.size &&
            packets[index].timestampNanos <= targetTimeNanos
        ) {
            val recorded = packets[index]

            clock.seek(
                recorded.timestampNanos,
            )

            processRecordedPacket(
                currentSession,
                recorded,
            )

            index++
        }

        clock.seek(
            targetTimeNanos,
        )

        currentSession.world.seekReplayTime(
            targetTimeNanos,
        )
    }

    private fun processRecordedPacket(
        currentSession: ReplaySession,
        recorded: RecordedPacket,
    ) {
        try {
            val packet =
                decode(
                    recorded,
                )

            if (
                recorded.flow ==
                PacketFlow.SERVERBOUND
            ) {
                applyServerboundPacket(
                    currentSession,
                    packet,
                )
            } else {
                currentSession.processClientboundPacket(
                    packet,
                )
            }
        } catch (
            throwable: Throwable,
        ) {
            System.err.println(
                "[Flashback] failed replay packet: " +
                    recorded.packetClass,
            )

            throwable.printStackTrace()
        }
    }

    private fun applyServerboundPacket(
        currentSession: ReplaySession,
        packet: Packet,
    ) {
        when (packet) {
            is C03PacketPlayer -> {
                val player =
                    currentSession.recordedPlayer

                player.prevPosX =
                    player.posX

                player.prevPosY =
                    player.posY

                player.prevPosZ =
                    player.posZ

                player.prevRotationYaw =
                    player.rotationYaw

                player.prevRotationPitch =
                    player.rotationPitch

                if (packet.func_149466_j()) {
                    player.setPosition(
                        packet.func_149464_c(),
                        packet.func_149471_f(),
                        packet.func_149472_e(),
                    )
                }

                if (packet.func_149463_k()) {
                    player.rotationYaw =
                        packet.func_149462_g()

                    player.rotationPitch =
                        packet.func_149470_h()
                }

                player.onGround =
                    packet.func_149465_i()
            }

            is C09PacketHeldItemChange -> {
                val slot =
                    packet.func_149614_c()

                if (slot in 0..8) {
                    currentSession.recordedPlayer
                        .inventory
                        .currentItem =
                        slot
                }
            }

            is C0APacketAnimation -> {
                currentSession.recordedPlayer
                    .swingItem()
            }

            is C0BPacketEntityAction -> {
                val player =
                    currentSession.recordedPlayer

                when (
                    packet.func_149513_d()
                ) {
                    1 ->
                        player.setSneaking(
                            true,
                        )

                    2 ->
                        player.setSneaking(
                            false,
                        )

                    4 ->
                        player.setSprinting(
                            true,
                        )

                    5 ->
                        player.setSprinting(
                            false,
                        )
                }
            }

            else ->
                return
        }
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
            target = Side.CLIENT
        }
    }
}
