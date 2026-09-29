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
            ReplayEditorToolbar().left(0).right(0).top(0).height(24),
        )
        .child(
            ReplayContainerWidget()
                .left(0)
                .top(24)
                .widthRel(0.70f)
                .heightRel(0.35f)
                .background(
                    ReplayUiStyle.panelBorder(),
                ).child(
                    ReplayTextWidget(
                        IKey.str(
                            "GAME VIEW",
                        ),
                    ).left(8)
                        .top(7)
                        .color(
                            ReplayUiStyle.MUTED_TEXT_COLOR,
                        ),
                ),
        ).child(
            ReplayInspectorPanel()
                .right(0)
                .top(24)
                .widthRel(0.30f)
                .heightRel(0.35f),
        ).child(
            ReplayControlBar()
                .left(0)
                .right(0)
                .bottom(0)
                .heightRel(0.55f),
        )

    companion object {

        const val PANEL_NAME =
            "replay_main"
    }
}
