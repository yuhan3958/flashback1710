package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.screen.CustomModularScreen
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import me.yuhan8954.flashback.Flashback1710
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution

class ReplayMainPanel :
    CustomModularScreen(
        Flashback1710.MODID,
    ) {

    init {
        drawDarkBackground(
            false,
        )
    }

    override fun buildUI(context: ModularGuiContext): ModularPanel {
        val minecraft = Minecraft.getMinecraft()
        val scaled = ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight)
        val inspectorWidth = (scaled.scaledWidth * 0.23f).toInt().coerceIn(110, 220)
        val workspaceHeight = (scaled.scaledHeight - TOOLBAR_HEIGHT).coerceAtLeast(0)
        val timelineHeight = (workspaceHeight * 0.28f).toInt().coerceIn(MIN_TIMELINE_HEIGHT, 210)
            .coerceAtMost((workspaceHeight * 0.6f).toInt())
        val gameViewHeight = workspaceHeight - timelineHeight
        return ModularPanel(PANEL_NAME).fullScreenInvisible()
            .child(
                ReplayEditorToolbar().left(0).right(0).top(0).height(TOOLBAR_HEIGHT),
            )
            .child(
                ReplayContainerWidget()
                    .left(0)
                    .top(TOOLBAR_HEIGHT)
                    .right(inspectorWidth)
                    .height(gameViewHeight)
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
                    .top(TOOLBAR_HEIGHT)
                    .width(inspectorWidth)
                    .height(gameViewHeight),
            ).child(
                ReplayControlBar()
                    .left(0)
                    .right(0)
                    .bottom(0)
                    .height(timelineHeight),
            )
    }

    companion object {

        private const val TOOLBAR_HEIGHT = 24
        private const val MIN_TIMELINE_HEIGHT = 124

        const val PANEL_NAME =
            "replay_main"
    }
}
