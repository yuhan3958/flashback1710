package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.screen.UISettings
import me.yuhan8954.flashback.io.ReplayLibrary
import me.yuhan8954.flashback.io.ReplayLibraryEntry
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft
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
        val panel =
            ReplayLibraryPanel(
                ReplayLibrary.list(
                    replayDirectory,
                ),
                ::play,
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

        minecraft.displayGuiScreen(
            null,
        )

        ReplayPlayer.play(
            entry.file,
        )
    }
}
