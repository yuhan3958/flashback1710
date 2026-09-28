package me.yuhan8954.flashback.replay

import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import java.net.InetSocketAddress
import java.net.SocketAddress

class ReplayNetworkManager : NetworkManager(false) {

    override fun isChannelOpen(): Boolean = true

    override fun processReceivedPackets() {}

    override fun getSocketAddress(): SocketAddress =
        InetSocketAddress.createUnresolved("replay", 0)

    override fun scheduleOutboundPacket(
        packet: Packet,
        vararg listeners: io.netty.util.concurrent.GenericFutureListener<*>,
    ) {
        // Replay 세계에는 실제 서버가 없다.
        // client -> server 패킷은 삭제.
    }
}
