package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayTrack
import me.yuhan8954.flashback.editor.track.type.CameraTrackType
import me.yuhan8954.flashback.editor.track.type.CameraOrbitTrackType
import me.yuhan8954.flashback.editor.track.type.FovTrackType
import me.yuhan8954.flashback.editor.track.type.SpeedTrackType
import me.yuhan8954.flashback.editor.track.type.TimeOfDayTrackType

class ReplayEditProject {
    val cameraTrack = ReplayTrack(CameraTrackType.id, CameraTrackType)
    val cameraOrbitTrack = ReplayTrack(CameraOrbitTrackType.id, CameraOrbitTrackType)
    val fovTrack = ReplayTrack(FovTrackType.id, FovTrackType)
    val speedTrack = ReplayTrack(SpeedTrackType.id, SpeedTrackType)
    val timeOfDayTrack = ReplayTrack(TimeOfDayTrackType.id, TimeOfDayTrackType)

    fun tracks(): List<ReplayTrack<*>> = listOf(cameraTrack, cameraOrbitTrack, fovTrack, speedTrack, timeOfDayTrack)

    fun track(id: String): ReplayTrack<*>? = tracks().firstOrNull { it.id == id }

    fun keyframeTimes(id: String): List<Long> = track(id)?.keyframes()?.map { it.timestampNanos } ?: emptyList()

    fun clear() {
        tracks().forEach { it.clear() }
    }
}
