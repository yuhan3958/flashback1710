package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.mixin.AccessorNetHandlerPlayClient
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotRestorer
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.stats.StatFileWriter
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType

class ReplaySession(
    val minecraft: Minecraft,
) {

    lateinit var networkManager:
        ReplayNetworkManager
        private set

    lateinit var handler:
        ReplayNetHandler
        private set

    lateinit var world:
        ReplayWorld
        private set

    lateinit var player:
        EntityClientPlayerMP
        private set

    private val statFileWriter =
        StatFileWriter()

    var active =
        false
        private set

    fun open(
        snapshot: ReplaySnapshot,
    ) {
        close()

        networkManager =
            ReplayNetworkManager()

        handler =
            ReplayNetHandler(
                minecraft,
                networkManager,
            )

        networkManager.setNetHandler(
            handler,
        )

        val settings =
            WorldSettings(
                snapshot.seed,
                WorldSettings.GameType.CREATIVE,
                false,
                false,
                WorldType.DEFAULT,
            )

        world =
            ReplayWorld(
                handler =
                handler,
                settings =
                settings,
                dimension =
                snapshot.dimensionId,
                difficulty =
                EnumDifficulty.NORMAL,
                profiler =
                minecraft.mcProfiler,
            )

        (
            handler as
                AccessorNetHandlerPlayClient
            ).setReplayWorld(
            world,
        )

        minecraft.loadWorld(
            world,
            "Loading replay...",
        )

        player =
            EntityClientPlayerMP(
                minecraft,
                world,
                minecraft.session,
                handler,
                statFileWriter,
            )

        minecraft.thePlayer =
            player

        minecraft.renderViewEntity =
            player

        SnapshotRestorer.restore(
            world,
            player,
            snapshot,
        )

        active =
            true
    }

    fun close() {
        if (!active) {
            return
        }

        active =
            false

        minecraft.loadWorld(
            null,
        )

        minecraft.displayGuiScreen(
            GuiMainMenu(),
        )
    }
}
