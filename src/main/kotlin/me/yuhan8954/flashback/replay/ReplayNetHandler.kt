package me.yuhan8954.flashback.replay

import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S40PacketDisconnect

class ReplayNetHandler(
    minecraft: Minecraft,
    networkManager: ReplayNetworkManager,
) : NetHandlerPlayClient(
    minecraft,
    null,
    networkManager,
) {

    override fun addToSendQueue(packet: Packet) {
        // 서버가 없으므로 outbound 패킷 폐기
    }

    override fun handleDisconnect(packet: S40PacketDisconnect) {
        // 녹화 파일 끝에 disconnect가 있어도
        // 실제 Minecraft 연결 종료 화면으로 보내지 않음
    }
}
