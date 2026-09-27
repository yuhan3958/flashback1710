package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.editor.ReplayTimelineEventType
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
                56,
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
                        "Speed: " +
                            ReplayPlayer.cameraSpeed
                    } else {
                        "Player camera"
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
                "RMB: look",
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
                .width(100)
                .height(18)
                .background(
                    ReplayUiStyle.buttonBackground(),
                )
                .overlay(
                    IKey.dynamic {
                        "Add Key (" +
                            ReplayPlayer
                                .cameraKeyframeCount +
                            ")"
                    },
                ).onMousePressed {
                    it == 0 &&
                        ReplayPlayer
                            .addCameraKeyframe()
                },
        )

        child(editorButton("Update pose", 112, 103, 90) {
            ReplayPlayer.updateSelectedCameraKeyframePose()
        })

        child(
            ReplayButtonWidget()
                .left(8)
                .top(125)
                .width(54)
                .height(18)
                .background(
                    ReplayUiStyle.buttonBackground(),
                )
                .overlay(
                    IKey.dynamic {
                        "Mark " +
                            ReplayPlayer
                                .markerCount
                    },
                ).onMousePressed {
                    it == 0 &&
                        ReplayPlayer
                            .addMarker()
                },
        )

        child(
            ReplayButtonWidget()
                .left(66)
                .top(125)
                .width(42)
                .height(18)
                .background(
                    ReplayUiStyle.buttonBackground(),
                )
                .overlay(
                    IKey.str(
                        "Set In",
                    ),
                ).onMousePressed {
                    it == 0 &&
                        ReplayPlayer
                            .setInPoint()
                },
        )

        child(
            ReplayButtonWidget()
                .left(112)
                .top(125)
                .width(46)
                .height(18)
                .background(
                    ReplayUiStyle.buttonBackground(),
                )
                .overlay(
                    IKey.str(
                        "Set Out",
                    ),
                ).onMousePressed {
                    it == 0 &&
                        ReplayPlayer
                            .setOutPoint()
                },
        )

        child(editorButton("Clear", 162, 125, 44) {
            ReplayPlayer.clearInOutRange()
        })

        child(
            ReplayTextWidget(IKey.dynamic {
                ReplayPlayer.selectedKeyframeTimeNanos?.let {
                    "Selected: " + ReplayTimeFormatter.format(it)
                } ?: "Selected: none"
            }).left(8).right(8).top(151).height(10)
                .color(ReplayUiStyle.MUTED_TEXT_COLOR),
        )
        child(editorButton("Go to", 8, 166, 48) {
            ReplayPlayer.seekToSelectedCameraKeyframe()
        })
        child(editorButton("Move here", 60, 166, 68) {
            ReplayPlayer.moveSelectedCameraKeyframeToPlayhead()
        })
        child(editorButton("Delete", 132, 166, 50) {
            ReplayPlayer.deleteSelectedCameraKeyframe()
        })
        child(
            ReplayTextWidget("Timeline filters")
                .left(8).top(185).color(ReplayUiStyle.MUTED_TEXT_COLOR),
        )
        child(filterButton("Packets", ReplayTimelineEventType.PACKET, 8, 199, 56))
        child(filterButton("Checkpoints", ReplayTimelineEventType.CHECKPOINT, 68, 199, 90))
        child(filterButton("Markers", ReplayTimelineEventType.EVENT, 8, 219, 56))
        child(filterButton("Keyframes", ReplayTimelineEventType.CAMERA_KEYFRAME, 68, 219, 90))
    }

    private fun editorButton(
        label: String,
        left: Int,
        top: Int,
        width: Int,
        action: () -> Boolean,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left).top(top).size(width, 18)
        .background(ReplayUiStyle.buttonBackground())
        .overlay(IKey.str(label))
        .onMousePressed { it == 0 && action() }

    private fun filterButton(
        label: String,
        type: ReplayTimelineEventType,
        left: Int,
        top: Int,
        width: Int,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left).top(top).size(width, 18)
        .background(ReplayUiStyle.buttonBackground())
        .overlay(IKey.dynamic {
            (if (ReplayPlayer.timelineFilterEnabled(type)) "\u00A7b" else "\u00A77") + label
        })
        .onMousePressed { it == 0 && ReplayPlayer.toggleTimelineFilter(type) }

    private fun cameraButton(
        label: String,
        left: Int,
        active: () -> Boolean,
        action: () -> Unit,
    ): ButtonWidget<*> = ReplayButtonWidget()
        .left(left)
        .top(40)
        .size(44, 20)
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
