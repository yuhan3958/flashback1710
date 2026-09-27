package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.ModularUIConfig
import com.cleanroommc.modularui.screen.UISettings
import me.yuhan8954.flashback.io.ReplayLibrary
import me.yuhan8954.flashback.io.ReplayLibraryEntry
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMainMenu
import java.io.File

object ReplayLibraryController {

    private val minecraft:
        Minecraft
        get() = Minecraft.getMinecraft()

    val replayDirectory: File
        get() =
            File(
                minecraft.mcDataDir,
                "replays",
            )

    fun open() {
        ModularUIConfig.guiDebugMode = false

        val panel =
            ReplayLibraryPanel(
                ReplayLibrary.list(
                    replayDirectory,
                ),
                ::play,
                ::close,
            )

        panel.context.setSettings(
            UISettings(),
        )

        minecraft.displayGuiScreen(
            ReplayLibraryUiScreen(
                panel,
            ),
        )
    }

    private fun play(
        entry: ReplayLibraryEntry,
    ) {
        if (!entry.playable) {
            return
        }

        runCatching {
            ReplayPlayer.play(entry.file)
            minecraft.displayGuiScreen(null)
            ReplayUiController.open()
        }
            .onFailure {
                ReplayPlayer.stop()
                me.yuhan8954.flashback.Flashback1710.LOG.error(
                    "Could not open replay {}", entry.file.name, it,
                )
                open()
            }
    }

    private fun close() {
        minecraft.displayGuiScreen(
            if (minecraft.theWorld == null) GuiMainMenu() else null,
        )
    }
}
