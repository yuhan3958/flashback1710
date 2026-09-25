package me.yuhan8954.flashback.replay

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.stats.StatFileWriter
import net.minecraft.util.Session
import net.minecraft.world.World

class EntityReplaySpectator(
    minecraft: Minecraft,
    world: World,
    session: Session,
    netHandler: NetHandlerPlayClient,
    statFileWriter: StatFileWriter,
) : EntityClientPlayerMP(
    minecraft,
    world,
    session,
    netHandler,
    statFileWriter,
) {

    init {
        noClip = true
        setInvisible(true)
        capabilities.allowFlying = true
        capabilities.isFlying = true
        capabilities.disableDamage = true
    }

    override fun moveEntity(
        x: Double,
        y: Double,
        z: Double,
    ) {}

    override fun addVelocity(
        x: Double,
        y: Double,
        z: Double,
    ) {}
}
