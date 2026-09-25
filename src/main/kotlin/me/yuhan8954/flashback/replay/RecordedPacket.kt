package me.yuhan8954.flashback.replay

data class RecordedPacket(
    val timestampNanos: Long,
    val packetClass: String,
    val payload: ByteArray,
    val channel: String? = null,
    val flow: PacketFlow = PacketFlow.CLIENTBOUND,
)

enum class PacketFlow {
    CLIENTBOUND,
    SERVERBOUND,
}
