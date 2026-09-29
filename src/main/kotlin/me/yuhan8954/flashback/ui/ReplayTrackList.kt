package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayTrackList : ParentWidget<ReplayTrackList>() {
    init {
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget("TRACKS").left(6).top(4).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        listOf("Camera", "FOV", "Speed", "Markers").forEachIndexed { row, label ->
            child(
                ReplayButtonWidget().left(4).top(22 + row * 14).size(74, 14)
                    .background(ReplayUiStyle.buttonBackground()).overlay(
                        IKey.dynamic {
                            val count = when (row) {
                                0 -> ReplayPlayer.cameraKeyframeCount
                                1 -> ReplayPlayer.fovKeyframeCount
                                2 -> ReplayPlayer.speedKeyframeCount
                                else -> ReplayPlayer.markerCount
                            }
                            "${if (ReplayPlayer.activeTrackId == label.lowercase()) "§b" else ""}$label $count"
                        },
                    ).onMousePressed { it == 0 && ReplayPlayer.selectTrack(label.lowercase()) },
            )
        }
    }
}
