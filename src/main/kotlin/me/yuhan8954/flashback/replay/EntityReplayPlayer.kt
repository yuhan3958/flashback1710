package me.yuhan8954.flashback.replay

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

    /**
     * Prevent accidental local movement from changing the replay player.
     */
    override fun moveEntity(
        x: Double,
        y: Double,
        z: Double,
    ) {
        // Packet-driven replay entity: ignore local physics movement.
    }

    /**
     * Prevent local knockback / physics from accumulating velocity.
     *
     * Recorded motion can still be assigned directly by the replay system
     * when needed.
     */
    override fun addVelocity(
        x: Double,
        y: Double,
        z: Double,
    ) {
        // Ignore local simulation velocity.
    }
}
