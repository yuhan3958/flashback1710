package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
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
            ReplayContainerWidget()
                .left(4)
                .top(4)
                .widthRel(VIEW_WIDTH)
                .heightRel(WORKSPACE_HEIGHT)
                .background(
                    ReplayUiStyle.panelBorder(),
                ).child(
                    ReplayTextWidget(
                        IKey.str(
                            "VIEW",
                        ),
                    ).left(8)
                        .top(7)
                        .color(
                            ReplayUiStyle.MUTED_TEXT_COLOR,
                        ),
                ),
        ).child(
            ReplayCameraPanel()
                .right(4)
                .top(4)
                .widthRel(SETTINGS_WIDTH)
                .heightRel(WORKSPACE_HEIGHT),
        ).child(
            ReplayControlBar()
                .left(4)
                .right(4)
                .bottom(4)
                .heightRel(TIMELINE_HEIGHT),
        )

    companion object {

        const val PANEL_NAME =
            "replay_main"

        private const val VIEW_WIDTH =
            0.66f

        private const val SETTINGS_WIDTH =
            0.33f

        private const val WORKSPACE_HEIGHT =
            0.60f

        private const val TIMELINE_HEIGHT =
            0.37f
    }
}
