package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.drawable.GuiTextures
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayControlBar : ParentWidget<ReplayControlBar>() {

    init {
        size(
            WIDTH,
            HEIGHT,
        )
        background(
            ReplayUiStyle.panelBackground(),
            ReplayUiStyle.panelBorder(),
        )

        child(
            ReplayTimelineWidget()
                .left(PADDING)
                .right(PADDING)
                .top(8)
                .height(12),
        )

        child(
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
            ).left(PADDING)
                .top(21)
                .width(150)
                .height(10)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            controlButton(
                "- Speed",
                58,
            ) {
                ReplayUiController.decreaseSpeed()
            },
        )

        child(
            controlButton(
                "Step",
                120,
            ) {
                ReplayPlayer.step()
            },
        )

        child(
            controlButton(
                label = null,
                left = 166,
                dynamicLabel = {
                    if (ReplayPlayer.paused) {
                        "Play"
                    } else {
                        "Pause"
                    }
                },
            ) {
                ReplayPlayer.togglePause()
            },
        )

        child(
            controlButton(
                "+ Speed",
                212,
            ) {
                ReplayUiController.increaseSpeed()
            },
        )

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    ReplayTimeFormatter.formatSpeed(
                        ReplayPlayer.speed,
                    )
                },
            ).left(278)
                .top(39)
                .size(58, 16)
                .textAlign(
                    Alignment.Center,
                )
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )
    }

    private fun controlButton(
        label: String?,
        left: Int,
        dynamicLabel: (() -> String)? = null,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(37)
        .size(54, 20)
        .background(
            GuiTextures.BUTTON_CLEAN,
        )
        .overlay(
            if (dynamicLabel == null) {
                IKey.str(
                    requireNotNull(label),
                )
            } else {
                IKey.dynamic(
                    dynamicLabel,
                )
            },
        ).onMousePressed {
            if (it != 0) {
                false
            } else {
                action()
                true
            }
        }

    companion object {

        const val WIDTH = 344
        const val HEIGHT = 64

        private const val PADDING = 8
    }
}
