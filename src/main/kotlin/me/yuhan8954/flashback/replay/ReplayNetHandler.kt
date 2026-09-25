package me.yuhan8954.flashback.replay

import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S40PacketDisconnect

class ReplayNetHandler(
    private val minecraft: Minecraft,
    networkManager: ReplayNetworkManager,
) : NetHandlerPlayClient(
    minecraft,
    null,
    networkManager,
) {

    override fun addToSendQueue(packet: Packet) {}

    override fun handleDisconnect(packet: S40PacketDisconnect) {}

    override fun handleTimeUpdate(packet: S03PacketTimeUpdate) {
        val replayWorld =
            minecraft.theWorld as?
                ReplayWorld ?: return

        replayWorld.correctReplayTime(
            packet.func_149365_d(),
            packet.func_149366_c(),
            ReplayPlayer.currentTimeNanos,
        )
    }
}
