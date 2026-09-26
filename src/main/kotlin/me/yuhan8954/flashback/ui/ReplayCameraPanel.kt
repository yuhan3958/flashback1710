package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayCameraPanel : ParentWidget<ReplayCameraPanel>() {

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
                "CAM",
            ).left(5)
                .top(4)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            cameraButton(
                "Player",
                5,
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
                "Free",
                45,
                active = {
                    ReplayPlayer.freeCameraActive
                },
                action = {
                    ReplayPlayer.enableFreeCamera()
                },
            ),
        )

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    if (ReplayPlayer.freeCameraActive) {
                        "x" +
                            ReplayPlayer.cameraSpeed
                    } else {
                        "follow"
                    }
                },
            ).left(5)
                .top(37)
                .width(78)
                .height(8)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )
    }

    private fun cameraButton(
        label: String,
        left: Int,
        active: () -> Boolean,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(16)
        .size(36, 16)
        .background(
            ReplayUiStyle.buttonBackground(),
        )
        .overlay(
            IKey.dynamic {
                if (active()) {
                    "\u00A7b$label"
                } else {
                    label
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

    companion object {

        const val WIDTH =
            86

        const val HEIGHT =
            48
    }
}
