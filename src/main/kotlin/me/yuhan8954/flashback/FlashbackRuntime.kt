package me.yuhan8954.flashback

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import me.yuhan8954.flashback.command.CommandFlashback
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.common.MinecraftForge

object FlashbackRuntime {

    @JvmStatic
    fun initialize() {
        println("[Flashback1710] Kotlin runtime initialized")

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
                ReplayPlayer.tick()
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
}
