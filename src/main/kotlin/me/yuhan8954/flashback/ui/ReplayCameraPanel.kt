package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayCameraPanel : ParentWidget<ReplayCameraPanel>() {

    init {
        background(
            ReplayUiStyle.panelBackground(),
            ReplayUiStyle.panelBorder(),
        )

        child(
            ReplayTextWidget(
                "SETTINGS",
            ).left(8)
                .top(7)
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        child(
            ReplayTextWidget(
                "Camera",
            ).left(8)
                .top(24)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            cameraButton(
                "Player",
                8,
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
                70,
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
                        "Free camera speed: " +
                            ReplayPlayer.cameraSpeed
                    } else {
                        "Following replay player"
                    }
                },
            ).left(8)
                .right(8)
                .top(67)
                .height(10)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            ReplayTextWidget(
                "RMB drag: look",
            ).left(8)
                .top(83)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        child(
            ReplayButtonWidget()
                .left(8)
                .top(103)
                .size(124, 18)
                .background(
                    ReplayUiStyle.buttonBackground(),
                )
                .overlay(
                    IKey.str(
                        "Keyframes (Later)",
                    ),
                ).apply {
                    setEnabled(
                        false,
                    )
                },
        )
    }

    private fun cameraButton(
        label: String,
        left: Int,
        active: () -> Boolean,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(40)
        .size(56, 20)
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
}
