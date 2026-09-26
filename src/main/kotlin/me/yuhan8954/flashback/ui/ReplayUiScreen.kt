package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.ModularUIConfig
import com.cleanroommc.modularui.screen.GuiScreenWrapper
import me.yuhan8954.flashback.replay.ReplayPlayer
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class ReplayUiScreen(
    screen: ReplayMainPanel,
) : GuiScreenWrapper(
    screen,
) {

    private var previousDebugMode:
        Boolean? =
        null

    override fun initGui() {
        if (previousDebugMode == null) {
            previousDebugMode =
                ModularUIConfig.guiDebugMode
        }

        ModularUIConfig.guiDebugMode =
            false

        super.initGui()
    }

    override fun onGuiClosed() {
        super.onGuiClosed()

        previousDebugMode?.let {
            ModularUIConfig.guiDebugMode =
                it
        }

        previousDebugMode =
            null
    }

    override fun doesGuiPauseGame(): Boolean = false

    fun initializeLayout() {
        screen.onResize(
            width,
            height,
        )
    }

    override fun keyTyped(
        character: Char,
        keyCode: Int,
    ) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE ->
                return

            Keyboard.KEY_F1 -> {
                ReplayUiController.toggleHudVisibility()
                return
            }

            Keyboard.KEY_SPACE -> {
                ReplayPlayer.togglePause()
                return
            }

            Keyboard.KEY_LEFT -> {
                ReplayUiController.skipBackward()
                return
            }

            Keyboard.KEY_RIGHT -> {
                ReplayUiController.skipForward()
                return
            }
        }

        super.keyTyped(
            character,
            keyCode,
        )
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        if (
            ReplayPlayer.freeCameraActive &&
            Mouse.isButtonDown(
                CAMERA_LOOK_BUTTON,
            )
        ) {
            ReplayPlayer.handleUiCameraInput(
                Mouse.getEventDX(),
                Mouse.getEventDY(),
            )
        }
    }

    companion object {

        private const val CAMERA_LOOK_BUTTON = 1
    }
}
