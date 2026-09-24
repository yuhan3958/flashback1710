package me.yuhan8954.flashback.replay

import net.minecraft.network.Packet

object ReplayNetwork {

    lateinit var handler: ReplayNetHandler

    fun process(packet: Packet) {
        packet.processPacket(handler)
    }
}
