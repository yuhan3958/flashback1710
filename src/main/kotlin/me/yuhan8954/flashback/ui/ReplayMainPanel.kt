package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.screen.CustomModularScreen
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import me.yuhan8954.flashback.Flashback1710
import org.lwjgl.input.Keyboard

class ReplayMainPanel :
    CustomModularScreen(
        Flashback1710.MODID,
    ) {

    init {
        drawDarkBackground(
            false,
        )
    }

    override fun onKeyPressed(
        typedChar: Char,
        keyCode: Int,
    ): Boolean {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            return true
        }

        return super.onKeyPressed(
            typedChar,
            keyCode,
        )
    }

    override fun buildUI(
        context: ModularGuiContext,
    ): ModularPanel = ModularPanel(
        PANEL_NAME,
    ).fullScreenInvisible()
        .child(
            ReplayControlBar()
                .left(4)
                .right(4)
                .bottom(4),
        )

    companion object {

        const val PANEL_NAME =
            "replay_main"
    }
}
