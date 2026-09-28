package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayTrack
import me.yuhan8954.flashback.editor.track.type.CameraTrackType

class ReplayEditProject {
    val cameraTrack = ReplayTrack(CameraTrackType.id, CameraTrackType)

    fun tracks(): List<ReplayTrack<*>> = listOf(cameraTrack)

    fun track(id: String): ReplayTrack<*>? = tracks().firstOrNull { it.id == id }
}
