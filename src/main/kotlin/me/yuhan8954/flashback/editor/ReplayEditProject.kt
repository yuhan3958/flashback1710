package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayTrack
import me.yuhan8954.flashback.editor.track.type.CameraTrackType
import me.yuhan8954.flashback.editor.track.type.FovTrackType

class ReplayEditProject {
    val cameraTrack = ReplayTrack(CameraTrackType.id, CameraTrackType)
    val fovTrack = ReplayTrack(FovTrackType.id, FovTrackType)

    fun tracks(): List<ReplayTrack<*>> = listOf(cameraTrack, fovTrack)

    fun track(id: String): ReplayTrack<*>? = tracks().firstOrNull { it.id == id }

    fun clear() {
        tracks().forEach { it.clear() }
    }
}
