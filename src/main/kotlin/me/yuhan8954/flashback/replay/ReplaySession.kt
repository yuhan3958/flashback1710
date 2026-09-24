package me.yuhan8954.flashback.replay

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.stats.StatFileWriter
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType

class ReplaySession(
    val minecraft: Minecraft,
) {

    lateinit var networkManager: ReplayNetworkManager
        private set

    lateinit var handler: ReplayNetHandler
        private set

    lateinit var world: ReplayWorld
        private set

    lateinit var player: EntityClientPlayerMP
        private set

    var active = false
        private set

    private val statFileWriter = StatFileWriter()

    fun open(
        dimension: Int = 0,
        seed: Long = 0L,
    ) {
        close()

        networkManager = ReplayNetworkManager()

        handler = ReplayNetHandler(
            minecraft,
            networkManager,
        )

        val settings = WorldSettings(
            seed,
            WorldSettings.GameType.CREATIVE,
            false,
            false,
            WorldType.DEFAULT,
        )

        world = ReplayWorld(
            handler = handler,
            settings = settings,
            dimension = dimension,
            difficulty = EnumDifficulty.NORMAL,
            profiler = minecraft.mcProfiler,
        )

        minecraft.loadWorld(
            world,
            "Loading replay...",
        )

        player = EntityClientPlayerMP(
            minecraft,
            world,
            minecraft.session,
            handler,
            statFileWriter,
        )

        minecraft.thePlayer = player
        minecraft.renderViewEntity = player

        world.addEntityToWorld(
            player.entityId,
            player,
        )

        active = true
    }

    fun close() {
        if (!active) {
            return
        }

        active = false

        minecraft.renderViewEntity = null
        minecraft.thePlayer = null
        minecraft.loadWorld(null)
    }
}
