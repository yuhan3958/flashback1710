package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.type.CameraTrackType
import me.yuhan8954.flashback.editor.track.type.FovTrackType
import me.yuhan8954.flashback.editor.track.type.SpeedTrackType

data class ReplayTrackDescriptor(val id: String, val displayName: String, val keyframed: Boolean)

object ReplayEditorTracks {
    val rows = listOf(
        ReplayTrackDescriptor(CameraTrackType.id, "Camera", true),
        ReplayTrackDescriptor(FovTrackType.id, "FOV", true),
        ReplayTrackDescriptor(SpeedTrackType.id, "Speed", true),
        ReplayTrackDescriptor("markers", "Markers", false),
    )

    fun find(id: String): ReplayTrackDescriptor? = rows.firstOrNull { it.id == id }
}
