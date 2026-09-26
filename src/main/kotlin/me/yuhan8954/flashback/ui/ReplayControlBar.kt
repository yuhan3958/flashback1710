package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayControlBar : ParentWidget<ReplayControlBar>() {

    init {
        background(
            ReplayUiStyle.panelBackground(),
            ReplayUiStyle.panelBorder(),
        )

        child(
            ReplayTextWidget(
                "TIMELINE",
            ).left(8)
                .top(7)
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        child(
            transportButton(
                "X",
                70,
            ) {
                ReplayPlayer.stop()
            },
        )

        child(
            transportButton(
                "|<",
                92,
            ) {
                ReplayUiController.skipBackward()
            },
        )

        child(
            transportButton(
                "<<",
                114,
            ) {
                ReplayUiController.decreaseSpeed()
            },
        )

        child(
            transportButton(
                label = null,
                left = 136,
                dynamicLabel = {
                    if (ReplayPlayer.paused) {
                        ">"
                    } else {
                        "||"
                    }
                },
            ) {
                ReplayPlayer.togglePause()
            },
        )

        child(
            transportButton(
                ">>",
                158,
            ) {
                ReplayUiController.increaseSpeed()
            },
        )

        child(
            transportButton(
                ">|",
                180,
            ) {
                ReplayUiController.skipForward()
            },
        )

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    ReplayTimeFormatter.format(
                        ReplayPlayer.currentTimeNanos,
                    )
                },
            ).left(210)
                .top(7)
                .width(88)
                .height(10)
                .textAlign(
                    Alignment.CenterRight,
                )
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    " / " +
                        ReplayTimeFormatter.format(
                            ReplayPlayer.totalDurationNanos,
                        )
                },
            ).left(298)
                .top(7)
                .width(94)
                .height(10)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    speedPrefix() +
                        ReplayTimeFormatter.formatSpeed(
                            ReplayPlayer.speed,
                        )
                },
            ).left(400)
                .top(7)
                .width(48)
                .height(10)
                .textAlign(
                    Alignment.CenterRight,
                )
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        child(
            ReplayTextWidget(
                "Wheel: zoom  Shift+wheel / MMB drag: pan",
            ).right(8)
                .top(7)
                .width(220)
                .height(10)
                .textAlign(
                    Alignment.CenterRight,
                )
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            ReplayTimelineWidget()
                .left(8)
                .right(8)
                .top(28)
                .bottom(8),
        )
    }

    private fun transportButton(
        label: String?,
        left: Int,
        dynamicLabel: (() -> String)? = null,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(4)
        .size(18, 18)
        .background(
            ReplayUiStyle.buttonBackground(),
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

    private fun speedPrefix(): String = when {
        ReplayPlayer.speed < 0.0 ->
            "\u00A76"

        ReplayPlayer.speed > 1.0 ->
            "\u00A7a"

        else ->
            "\u00A7f"
    }
}
