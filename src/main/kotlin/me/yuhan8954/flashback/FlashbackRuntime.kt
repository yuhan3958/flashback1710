package me.yuhan8954.flashback

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import me.yuhan8954.flashback.command.CommandFlashback
import me.yuhan8954.flashback.recording.ReplayRecorder
import me.yuhan8954.flashback.replay.ReplayPlayer
import me.yuhan8954.flashback.ui.ReplayUiController
import me.yuhan8954.flashback.ui.ReplayLibraryController
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMainMenu
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.common.MinecraftForge

object FlashbackRuntime {

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
                    ReplayPlayer.tick()
                    ReplayRecorder.tick()
                    ReplayUiController.syncHudVisibility()
                }
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
    fun onMenuInitialized(event: GuiScreenEvent.InitGuiEvent.Post) {
        if (event.gui !is GuiMainMenu) return
        event.buttonList.add(
            GuiButton(
                REPLAY_BUTTON_ID,
                event.gui.width / 2 - 100,
                event.gui.height / 4 + 158,
                200,
                20,
                "Replays",
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
