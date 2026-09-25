package me.yuhan8954.flashback.replay

import com.mojang.authlib.GameProfile
import me.yuhan8954.flashback.camera.ReplayCameraController
import me.yuhan8954.flashback.mixin.AccessorNetHandlerPlayClient
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import me.yuhan8954.flashback.snapshot.SnapshotRestorer
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.stats.StatFileWriter
import net.minecraft.util.MovementInput
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
import java.util.UUID

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

    lateinit var recordedPlayer: EntityReplayPlayer
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

        val recordedProfile =
            GameProfile(
                snapshot.player.profileId
                    ?.let(
                        UUID::fromString,
                    ),
                snapshot.player.profileName,
            )

        recordedPlayer =
            EntityReplayPlayer(
                minecraft,
                world,
                minecraft.session,
                handler,
                statFileWriter,
                recordedProfile,
            )

        recordedPlayer.movementInput = MovementInput()

        player =
            EntityReplaySpectator(
                minecraft,
                world,
                minecraft.session,
                handler,
                statFileWriter,
            )

        player.setPositionAndRotation(
            snapshot.player.x,
            snapshot.player.y,
            snapshot.player.z,
            snapshot.player.yaw,
            snapshot.player.pitch,
        )

        try {
            minecraft.thePlayer =
                player

            minecraft.loadWorld(
                world,
                "Loading replay...",
            )

            SnapshotRestorer.restore(
                world,
                recordedPlayer,
                snapshot,
            )

            cameraController =
                ReplayCameraController(
                    this,
                )

            check(
                cameraController.enable(),
            ) {
                "Replay free camera could not be enabled"
            }

            active =
                true
        } catch (
            throwable: Throwable,
        ) {
            restoreLiveWorld()
            throw throwable
        }
    }

    fun reset(snapshot: ReplaySnapshot) {
        check(active) {
            "Replay session is not active"
        }

        val currentScreen = minecraft.currentScreen
        val freeCameraActive = cameraController.state.active
        val currentView = minecraft.renderViewEntity
        val cameraX = currentView?.posX ?: snapshot.player.x
        val cameraY = currentView?.posY ?: snapshot.player.y
        val cameraZ = currentView?.posZ ?: snapshot.player.z
        val cameraYaw = currentView?.rotationYaw ?: snapshot.player.yaw
        val cameraPitch = currentView?.rotationPitch ?: snapshot.player.pitch

        cameraController.close()

        networkManager = ReplayNetworkManager()

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

        val recordedProfile =
            GameProfile(
                snapshot.player.profileId
                    ?.let(
                        UUID::fromString,
                    ),
                snapshot.player.profileName,
            )

        recordedPlayer =
            EntityReplayPlayer(
                minecraft,
                world,
                minecraft.session,
                handler,
                statFileWriter,
                recordedProfile,
            )

        recordedPlayer.movementInput = MovementInput()

        player =
            EntityReplaySpectator(
                minecraft,
                world,
                minecraft.session,
                handler,
                statFileWriter,
            )

        if (freeCameraActive) {
            player.setPositionAndRotation(
                cameraX,
                cameraY - player.eyeHeight,
                cameraZ,
                cameraYaw,
                cameraPitch,
            )
        } else {
            player.setPositionAndRotation(
                snapshot.player.x,
                snapshot.player.y,
                snapshot.player.z,
                snapshot.player.yaw,
                snapshot.player.pitch,
            )
        }

        minecraft.thePlayer =
            player

        minecraft.loadWorld(
            world,
            "Seeking replay...",
        )

        SnapshotRestorer.restore(
            world,
            recordedPlayer,
            snapshot,
        )

        cameraController =
            ReplayCameraController(
                this,
            )

        if (freeCameraActive) {
            minecraft.renderViewEntity = player
            cameraController.enable()
        } else {
            minecraft.renderViewEntity = recordedPlayer
        }

        if (
            currentScreen != null &&
            minecraft.currentScreen !== currentScreen
        ) {
            minecraft.displayGuiScreen(
                currentScreen,
            )
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

    fun processClientboundPacket(packet: Packet) {
        try {
            minecraft.thePlayer = recordedPlayer
            packet.processPacket(handler)
        } finally {
            minecraft.thePlayer = player
        }
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
