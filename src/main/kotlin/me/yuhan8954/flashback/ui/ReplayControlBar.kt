package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayControlBar : ParentWidget<ReplayControlBar>() {

    init {
        height(
            HEIGHT,
        )

        background(
            ReplayUiStyle.panelBackground(),
            ReplayUiStyle.panelBorder(),
        )

        child(
            transportButton(
                "X",
                4,
            ) {
                ReplayPlayer.stop()
            },
        )

        child(
            transportButton(
                "|<",
                24,
            ) {
                ReplayUiController.skipBackward()
            },
        )

        child(
            transportButton(
                "<<",
                44,
            ) {
                ReplayUiController.decreaseSpeed()
            },
        )

        child(
            transportButton(
                label = null,
                left = 64,
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
                84,
            ) {
                ReplayUiController.increaseSpeed()
            },
        )

        child(
            transportButton(
                ">|",
                104,
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
            ).left(126)
                .top(5)
                .width(74)
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
                    speedPrefix() +
                        ReplayTimeFormatter.formatSpeed(
                            ReplayPlayer.speed,
                        )
                },
            ).left(126)
                .top(19)
                .width(74)
                .height(10)
                .textAlign(
                    Alignment.CenterRight,
                )
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        child(
            cameraButton(
                "P",
                204,
                active = {
                    !ReplayPlayer.freeCameraActive
                },
                action = {
                    ReplayPlayer.disableFreeCamera()
                },
            ),
        )

        child(
            cameraButton(
                "F",
                224,
                active = {
                    ReplayPlayer.freeCameraActive
                },
                action = {
                    ReplayPlayer.enableFreeCamera()
                },
            ),
        )

        child(
            ReplayTimelineWidget()
                .left(TIMELINE_LEFT)
                .right(4)
                .top(3)
                .bottom(3),
        )
    }

    private fun transportButton(
        label: String?,
        left: Int,
        dynamicLabel: (() -> String)? = null,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(11)
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

    private fun cameraButton(
        label: String,
        left: Int,
        active: () -> Boolean,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(11)
        .size(18, 18)
        .background(
            ReplayUiStyle.buttonBackground(),
        )
        .overlay(
            IKey.dynamic {
                if (active()) {
                    "\u00A7b$label"
                } else {
                    "\u00A77$label"
                }
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

    companion object {

        const val HEIGHT =
            40

        private const val TIMELINE_LEFT =
            246
    }
}
