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
                "CAMERA",
            ).left(8)
                .top(7)
                .color(
                    ReplayUiStyle.TEXT_COLOR,
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
                61,
                active = {
                    ReplayPlayer.freeCameraActive
                },
                action = {
                    ReplayPlayer.enableFreeCamera()
                },
            ),
        )

        child(
            ReplayButtonWidget()
                .left(8)
                .top(47)
                .size(106, 18)
                .background(
                    GuiTextures.BUTTON_CLEAN,
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

        child(
            ReplayTextWidget(
                IKey.dynamic {
                    if (ReplayPlayer.freeCameraActive) {
                        "Free speed: " +
                            ReplayPlayer.cameraSpeed
                    } else {
                        "Following replay player"
                    }
                },
            ).left(8)
                .top(70)
                .width(106)
                .height(10)
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
        .top(24)
        .size(49, 18)
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

        const val WIDTH = 122
        const val HEIGHT = 88
    }
}
