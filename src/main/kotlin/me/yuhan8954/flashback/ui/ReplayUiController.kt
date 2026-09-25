package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.replay.ReplayClock
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft

object ReplayUiController {

    private var hiddenByHud =
        false

    private var openRequested =
        false

    private val minecraft:
        Minecraft
        get() = Minecraft.getMinecraft()

    fun open(): Boolean {
        if (
            !ReplayPlayer.playing ||
            minecraft.gameSettings.hideGUI
        ) {
            return false
        }

        if (
            minecraft.currentScreen is
                ReplayUiScreen
        ) {
            openRequested = false
            return true
        }

        if (minecraft.currentScreen != null) {
            openRequested = true
            return true
        }

        openRequested = false

        val replayScreen =
            ReplayUiScreen(
                ReplayMainPanel(),
            )

        minecraft.displayGuiScreen(
            replayScreen,
        )

        replayScreen.initializeLayout()

        return true
    }

    fun close() {
        hiddenByHud = false
        openRequested = false

        if (
            minecraft.currentScreen is
                ReplayUiScreen
        ) {
            minecraft.displayGuiScreen(
                null,
            )
        }
    }

    fun toggleHudVisibility() {
        minecraft.gameSettings.hideGUI =
            !minecraft.gameSettings.hideGUI

        syncHudVisibility()
    }

    fun syncHudVisibility() {
        if (!ReplayPlayer.playing) {
            hiddenByHud = false
            openRequested = false
            return
        }

        if (minecraft.gameSettings.hideGUI) {
            if (
                minecraft.currentScreen is
                    ReplayUiScreen
            ) {
                hiddenByHud = true
                minecraft.displayGuiScreen(
                    null,
                )
            }

            return
        }

        if (
            (hiddenByHud || openRequested) &&
            minecraft.currentScreen == null
        ) {
            hiddenByHud = false
            open()
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
