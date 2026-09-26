package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.screen.CustomModularScreen
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import me.yuhan8954.flashback.Flashback1710

class ReplayMainPanel :
    CustomModularScreen(
        Flashback1710.MODID,
    ) {

    init {
        drawDarkBackground(
            false,
        )
    }

    override fun buildUI(
        context: ModularGuiContext,
    ): ModularPanel = ModularPanel(
        PANEL_NAME,
    ).fullScreenInvisible()
        .child(
            ReplayControlBar()
                .left(8)
                .right(8)
                .bottom(4),
        ).child(
            ReplayCameraPanel()
                .right(6)
                .top(6),
        )

    companion object {

        const val PANEL_NAME =
            "replay_main"
    }
}
