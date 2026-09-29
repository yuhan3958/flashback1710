package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.screen.GuiScreenWrapper
import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.replay.ReplayPlayer
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class ReplayUiScreen(
    screen: ReplayMainPanel,
) : GuiScreenWrapper(
    screen,
) {

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
        if (ReplayValueInput.text != null) {
            when (keyCode) {
                Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> ReplayValueInput.commit()
                Keyboard.KEY_ESCAPE -> ReplayValueInput.cancel()
                Keyboard.KEY_BACK -> ReplayValueInput.backspace()
                else -> ReplayValueInput.type(character)
            }
            return
        }
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
            Keyboard.KEY_DELETE -> {
                ReplayEditorController.deleteSelectedKeyframe()
                return
            }
            Keyboard.KEY_LEFT -> {
                ReplayPlayer.seek(ReplayPlayer.currentTimeNanos - seekStep())
                return
            }
            Keyboard.KEY_RIGHT -> {
                ReplayPlayer.seek(ReplayPlayer.currentTimeNanos + seekStep())
                return
            }
            Keyboard.KEY_I -> {
                ReplayEditorController.setInPoint()
                return
            }
            Keyboard.KEY_O -> {
                ReplayEditorController.setOutPoint()
                return
            }
            Keyboard.KEY_M -> {
                ReplayEditorController.addMarker()
                return
            }
            Keyboard.KEY_K -> {
                ReplayEditorController.addKeyframeToActiveTrack()
                return
            }
        }

        super.keyTyped(
            character,
            keyCode,
        )
    }

    private fun seekStep(): Long = if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
        Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
    ) {
        1_000_000_000L
    } else {
        50_000_000L
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
