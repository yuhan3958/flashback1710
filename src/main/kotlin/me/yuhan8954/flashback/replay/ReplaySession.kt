package me.yuhan8954.flashback.replay

import me.yuhan8954.flashback.camera.ReplayCameraController
import me.yuhan8954.flashback.mixin.AccessorNetHandlerPlayClient
import me.yuhan8954.flashback.mixin.AccessorWorldClient
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotRestorer
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.EntityLivingBase
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

    lateinit var cameraController:
        ReplayCameraController
        private set

    private val statFileWriter =
        StatFileWriter()

    private var liveWorld:
        WorldClient? =
        null

    private var livePlayer:
        EntityClientPlayerMP? =
        null

    private var liveViewEntity:
        EntityLivingBase? =
        null

    var active =
        false
        private set

    fun open(
        snapshot: ReplaySnapshot,
    ) {
        close()

        liveWorld =
            minecraft.theWorld

        livePlayer =
            minecraft.thePlayer

        liveViewEntity =
            minecraft.renderViewEntity

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

        player =
            EntityClientPlayerMP(
                minecraft,
                world,
                minecraft.session,
                handler,
                statFileWriter,
            )

        detachLivePlayer()

        try {
            minecraft.thePlayer =
                player

            minecraft.loadWorld(
                world,
                "Loading replay...",
            )

            SnapshotRestorer.restore(
                world,
                player,
                snapshot,
            )

            cameraController =
                ReplayCameraController(
                    this,
                )

            active =
                true
        } catch (
            throwable: Throwable,
        ) {
            restoreLiveWorld()
            throw throwable
        }
    }

    fun close() {
        if (!active) {
            return
        }

        cameraController.close()

        active = false

        restoreLiveWorld()
    }

    private fun detachLivePlayer() {
        val currentWorld =
            liveWorld ?: return

        val currentPlayer =
            livePlayer ?: return

        currentWorld.playerEntities.remove(
            currentPlayer,
        )

        currentWorld.loadedEntityList.remove(
            currentPlayer,
        )

        (
            currentWorld as
                AccessorWorldClient
            ).replayEntityList.remove(
            currentPlayer,
        )

        if (
            currentPlayer.addedToChunk &&
            currentWorld.chunkProvider
                .chunkExists(
                    currentPlayer.chunkCoordX,
                    currentPlayer.chunkCoordZ,
                )
        ) {
            currentWorld.getChunkFromChunkCoords(
                currentPlayer.chunkCoordX,
                currentPlayer.chunkCoordZ,
            ).removeEntity(
                currentPlayer,
            )
        }

        currentPlayer.addedToChunk = false

        currentWorld.onEntityRemoved(
            currentPlayer,
        )
    }

    private fun restoreLiveWorld() {
        val restoredWorld =
            liveWorld

        val restoredPlayer =
            livePlayer

        if (
            restoredWorld != null &&
            restoredPlayer != null
        ) {
            minecraft.thePlayer =
                restoredPlayer

            minecraft.loadWorld(
                restoredWorld,
                "Returning to world...",
            )

            minecraft.renderViewEntity =
                liveViewEntity
                    ?: restoredPlayer

            minecraft.displayGuiScreen(
                null,
            )
        } else {
            minecraft.loadWorld(
                null,
            )

            minecraft.displayGuiScreen(
                GuiMainMenu(),
            )
        }

        liveWorld = null
        livePlayer = null
        liveViewEntity = null
    }
}
