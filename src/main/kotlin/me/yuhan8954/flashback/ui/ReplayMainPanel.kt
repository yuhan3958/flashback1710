package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.screen.CustomModularScreen
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widget.ParentWidget
import me.yuhan8954.flashback.Flashback1710
import me.yuhan8954.flashback.replay.ReplayPlayer

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
            topStatusPanel(),
        ).child(
            ReplayControlBar()
                .horizontalCenter()
                .bottom(8),
        ).child(
            ReplayCameraPanel()
                .right(8)
                .verticalCenter(),
        )

    private fun topStatusPanel(): ParentWidget<*> = ReplayContainerWidget()
        .size(
            280,
            32,
        ).horizontalCenter()
        .top(8)
        .background(
            ReplayUiStyle.panelBackground(),
            ReplayUiStyle.panelBorder(),
        ).child(
            ReplayTextWidget(
                IKey.dynamic {
                    ReplayTimeFormatter.format(
                        ReplayPlayer.currentTimeNanos,
                    ) +
                        " / " +
                        ReplayTimeFormatter.format(
                            ReplayPlayer.totalDurationNanos,
                        )
                },
            ).left(8)
                .top(5)
                .right(8)
                .height(10)
                .textAlign(
                    Alignment.Center,
                ).color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        ).child(
            ReplayTextWidget(
                IKey.dynamic {
                    val state =
                        if (ReplayPlayer.paused) {
                            "Paused"
                        } else {
                            "Playing"
                        }

                    "$state  -  ${ReplayTimeFormatter.formatSpeed(ReplayPlayer.speed)}"
                },
            ).left(8)
                .top(17)
                .right(8)
                .height(10)
                .textAlign(
                    Alignment.Center,
                ).color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

    companion object {

        const val PANEL_NAME =
            "replay_main"
    }
}
