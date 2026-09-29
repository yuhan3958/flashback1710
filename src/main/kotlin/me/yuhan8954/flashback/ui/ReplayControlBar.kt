package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayControlBar : ParentWidget<ReplayControlBar>() {

    init {
        background(
            ReplayUiStyle.panelBackground(),
            ReplayUiStyle.panelBorder(),
        )

        child(
            ReplayTextWidget(
                "TRANSPORT",
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
            ReplayButtonWidget().left(210).top(4).size(64, 18)
                .background(ReplayUiStyle.buttonBackground())
                .overlay(IKey.dynamic { "Add ${ReplayEditorController.activeTrackId}" })
                .onMousePressed { it == 0 && ReplayEditorController.addKeyframeToActiveTrack() },
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
            ).left(8)
                .top(27)
                .width(210)
                .height(10)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    "${ReplayTimeFormatter.formatSpeed(ReplayPlayer.speed)} x " +
                        "${ReplayTimeFormatter.formatSpeed(ReplayPlayer.automationSpeed)} = " +
                        ReplayTimeFormatter.formatSpeed(ReplayPlayer.effectiveSpeed)
                },
            ).right(8)
                .top(27)
                .width(130)
                .height(10)
                .textAlign(
                    Alignment.CenterRight,
                )
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        child(ReplayTrackList().left(8).top(42).bottom(8).width(82))
        child(ReplayTimelineWidget().left(90).right(8).top(42).bottom(8))
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
