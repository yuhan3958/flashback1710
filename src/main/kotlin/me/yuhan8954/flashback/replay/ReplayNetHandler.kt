package me.yuhan8954.flashback.replay

import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S07PacketRespawn
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

    override fun handleRespawn(packet: S07PacketRespawn) {
        val recordedPlayer = minecraft.thePlayer as? EntityReplayPlayer ?: return

        // Vanilla replaces Minecraft.thePlayer here, which is the replay's spectator.
        // Keep the recorded player in place so replay playback and its camera survive death.
        recordedPlayer.isDead = false
        recordedPlayer.deathTime = 0
        recordedPlayer.hurtTime = 0
        recordedPlayer.motionX = 0.0
        recordedPlayer.motionY = 0.0
        recordedPlayer.motionZ = 0.0
        recordedPlayer.setHealth(recordedPlayer.maxHealth)
        recordedPlayer.foodStats.foodLevel = 20
        recordedPlayer.foodStats.setFoodSaturationLevel(5.0f)
        recordedPlayer.inventory.clearInventory(null, -1)
        recordedPlayer.setXPStats(0.0f, 0, 0)
        recordedPlayer.clearActivePotions()
        recordedPlayer.dimension = packet.func_149082_c()

        val replayWorld = minecraft.theWorld as? ReplayWorld ?: return
        if (!replayWorld.loadedEntityList.contains(recordedPlayer)) {
            replayWorld.spawnEntityInWorld(recordedPlayer)
        }
    }

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
