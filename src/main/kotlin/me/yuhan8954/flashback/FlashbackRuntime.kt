package me.yuhan8954.flashback

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.network.FMLNetworkEvent
import me.yuhan8954.flashback.command.CommandFlashback
import me.yuhan8954.flashback.config.ReplayConfig
import me.yuhan8954.flashback.recording.ReplayRecorder
import me.yuhan8954.flashback.replay.ReplayPlayer
import me.yuhan8954.flashback.ui.ReplayLibraryController
import me.yuhan8954.flashback.ui.ReplayUiController
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMainMenu
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge

object FlashbackRuntime {

    private var pendingAutoRecord = false

    @JvmStatic
    fun initialize() {
        println("[Flashback1710] Flashback1710 Kotlin runtime initialized")

        MinecraftForge.EVENT_BUS.register(this)
        FMLCommonHandler.instance().bus().register(this)
    }

    @JvmStatic
    fun onInitialized() {
        ClientCommandHandler.instance.registerCommand(
            CommandFlashback(),
        )

        println("[Flashback1710] Bootstrap complete")
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        when (event.phase) {
            TickEvent.Phase.START ->
                ReplayPlayer.beginTick()

            TickEvent.Phase.END ->
                {
                    if (pendingAutoRecord) {
                        val mc = Minecraft.getMinecraft()
                        if (mc.theWorld != null && mc.thePlayer != null && !ReplayPlayer.playing) {
                            pendingAutoRecord = false
                            if (ReplayRecorder.currentFile == null) {
                                ReplayRecorder.startNew(java.io.File(mc.mcDataDir, "replays"))
                            }
                        }
                    }
                    ReplayPlayer.tick()
                    ReplayRecorder.tick()
                    ReplayUiController.syncHudVisibility()
                }
        }
    }

    @SubscribeEvent
    fun onConnected(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        pendingAutoRecord = ReplayConfig.autoRecordOnJoin
    }

    @SubscribeEvent
    fun onDisconnected(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        pendingAutoRecord = false
        if (ReplayConfig.autoStopOnLeave) {
            ReplayRecorder.stop()
        }
    }

    @SubscribeEvent
    fun onMouseInput(event: MouseEvent) {
        ReplayPlayer.handleMouseInput(
            event.dx,
            event.dy,
            event.dwheel,
        )
    }

    @SubscribeEvent
    fun onHudRender(event: RenderGameOverlayEvent.Pre) {
        if (ReplayPlayer.playing && !ReplayPlayer.visualOverrides.renderHud) event.isCanceled = true
    }

    @SubscribeEvent
    fun onMenuInitialized(event: GuiScreenEvent.InitGuiEvent.Post) {
        if (event.gui !is GuiMainMenu) return
        event.buttonList.add(
            GuiButton(
                REPLAY_BUTTON_ID,
                event.gui.width / 2 - 100,
                event.gui.height / 4 + 158,
                200,
                20,
                ReplayLang.text("menu.replays"),
            ),
        )
    }

    @SubscribeEvent
    fun onMenuButton(event: GuiScreenEvent.ActionPerformedEvent.Post) {
        if (event.gui is GuiMainMenu && event.button.id == REPLAY_BUTTON_ID) {
            ReplayLibraryController.open()
        }
    }

    private const val REPLAY_BUTTON_ID = 171010
}
