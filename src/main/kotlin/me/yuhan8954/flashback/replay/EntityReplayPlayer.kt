package me.yuhan8954.flashback.replay

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.stats.StatFileWriter
import net.minecraft.util.Session
import net.minecraft.world.World

class EntityReplayPlayer(
    minecraft: Minecraft,
    world: World,
    session: Session,
    netHandler: NetHandlerPlayClient,
    statFileWriter: StatFileWriter,
    private val replayProfile: GameProfile,
) : EntityClientPlayerMP(
    minecraft,
    world,
    session,
    netHandler,
    statFileWriter,
) {

    init {
        noClip = true
    }

    override fun getGameProfile(): GameProfile = replayProfile

    override fun moveEntity(
        x: Double,
        y: Double,
        z: Double,
    ) {
        // Recorded C03 state is authoritative.
    }

    override fun addVelocity(
        x: Double,
        y: Double,
        z: Double,
    ) {
        // Ignore local physics.
    }
}
