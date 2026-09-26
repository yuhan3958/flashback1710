package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
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
            ReplayTextWidget(
                IKey.dynamic {
                    ReplayTimeFormatter.format(
                        ReplayPlayer.currentTimeNanos,
                    )
                },
            ).left(8)
                .top(6)
                .width(96)
                .height(10)
                .color(
                    ReplayUiStyle.TEXT_COLOR,
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
            ).left(104)
                .top(6)
                .width(36)
                .height(10)
                .textAlign(
                    Alignment.CenterRight,
                )
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        child(
            transportButton(
                "|<",
                8,
            ) {
                ReplayUiController.skipBackward()
            },
        )

        child(
            transportButton(
                "<<",
                34,
            ) {
                ReplayUiController.decreaseSpeed()
            },
        )

        child(
            transportButton(
                label = null,
                left = 60,
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
                86,
            ) {
                ReplayUiController.increaseSpeed()
            },
        )

        child(
            transportButton(
                ">|",
                112,
            ) {
                ReplayUiController.skipForward()
            },
        )

        child(
            smallButton(
                "X",
                8,
                48,
            ) {
                ReplayPlayer.stop()
            },
        )

        child(
            ReplayTextWidget(
                "5s",
            ).left(34)
                .top(52)
                .width(24)
                .height(10)
                .textAlign(
                    Alignment.Center,
                )
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    ReplayTimeFormatter.format(
                        ReplayPlayer.totalDurationNanos,
                    )
                },
            ).left(60)
                .top(52)
                .width(80)
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
                .left(TIMELINE_LEFT)
                .right(8)
                .top(6)
                .bottom(6),
        )
    }

    private fun transportButton(
        label: String?,
        left: Int,
        dynamicLabel: (() -> String)? = null,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(22)
        .size(24, 22)
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

    private fun smallButton(
        label: String,
        left: Int,
        top: Int,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(top)
        .size(20, 16)
        .background(
            ReplayUiStyle.buttonBackground(),
        )
        .overlay(
            IKey.str(
                label,
            ),
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

    companion object {

        const val WIDTH =
            560

        const val HEIGHT =
            70

        private const val TIMELINE_LEFT =
            148
    }
}
