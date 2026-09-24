package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.replay.ReplayClock
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft

object ReplayUiController {

    private val minecraft:
        Minecraft
        get() = Minecraft.getMinecraft()

    fun open(): Boolean {
        if (
            !ReplayPlayer.playing ||
            minecraft.currentScreen is
                ReplayUiScreen
        ) {
            return false
        }

        minecraft.displayGuiScreen(
            ReplayUiScreen(
                ReplayMainPanel(),
            ),
        )

        return true
    }

    fun close() {
        if (
            minecraft.currentScreen is
                ReplayUiScreen
        ) {
            minecraft.displayGuiScreen(
                null,
            )
        }
    }

    fun decreaseSpeed(): Boolean = changeSpeed(
        -1,
    )

    fun increaseSpeed(): Boolean = changeSpeed(
        1,
    )

    private fun changeSpeed(direction: Int): Boolean {
        val speeds =
            ReplayClock.SUPPORTED_SPEEDS

        val currentIndex =
            speeds.indexOf(
                ReplayPlayer.speed,
            )

        if (currentIndex < 0) {
            return false
        }

        val nextIndex =
            (currentIndex + direction)
                .coerceIn(
                    0,
                    speeds.lastIndex,
                )

        if (nextIndex == currentIndex) {
            return false
        }

        return ReplayPlayer.setSpeed(
            speeds[nextIndex],
        )
    }
}
