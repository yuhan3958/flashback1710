package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.type.CameraTrackType
import me.yuhan8954.flashback.editor.track.type.FovTrackType
import me.yuhan8954.flashback.editor.track.type.SpeedTrackType
import me.yuhan8954.flashback.editor.track.type.TimeOfDayTrackType

data class ReplayTrackDescriptor(val id: String, val displayName: String, val keyframed: Boolean)

object ReplayEditorTracks {
    val rows = listOf(
        ReplayTrackDescriptor(CameraTrackType.id, "track.camera", true),
        ReplayTrackDescriptor(FovTrackType.id, "track.fov", true),
        ReplayTrackDescriptor(SpeedTrackType.id, "track.speed", true),
        ReplayTrackDescriptor(TimeOfDayTrackType.id, "track.time_of_day", true),
        ReplayTrackDescriptor("markers", "track.markers", false),
    )

    fun find(id: String): ReplayTrackDescriptor? = rows.firstOrNull { it.id == id }
}
