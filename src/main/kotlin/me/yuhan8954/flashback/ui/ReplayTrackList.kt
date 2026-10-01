package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.editor.ReplayEditorTracks
import me.yuhan8954.flashback.replay.ReplayPlayer
import me.yuhan8954.flashback.ReplayLang

class ReplayTrackList : ParentWidget<ReplayTrackList>() {
    init {
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget(ReplayLang.key("ui.tracks")).left(6).top(4).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        ReplayEditorTracks.rows.forEachIndexed { row, track ->
            child(
                ReplayButtonWidget().left(4).top(ReplayEditorMetrics.TIMELINE_RULER_HEIGHT + 1 + row * ReplayEditorMetrics.TRACK_ROW_HEIGHT)
                    .size(74, ReplayEditorMetrics.TRACK_ROW_HEIGHT - 2)
                    .background(ReplayUiStyle.buttonBackground()).overlay(
                        IKey.dynamic {
                            val count = if (track.keyframed) ReplayPlayer.keyframeTimes(track.id).size else ReplayPlayer.markerCount
                            "${if (ReplayEditorController.activeTrackId == track.id) "§b" else ""}${ReplayLang.text(track.displayName)} $count"
                        },
                    ).onMousePressed { it == 0 && ReplayEditorController.selectTrack(track.id) },
            )
        }
    }
}
