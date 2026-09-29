package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft

/** Coordinates editor commands while replay runtime stays in [ReplayPlayer]. */
object ReplayEditorController {
    val activeTrackId: String
        get() = ReplayPlayer.currentEditorState?.activeTrackId ?: "camera"

    fun selectTrack(trackId: String): Boolean {
        if (ReplayEditorTracks.find(trackId) == null) return false
        ReplayPlayer.currentEditorState?.activeTrackId = trackId
        clearKeyframeSelection()
        return true
    }

    fun clearKeyframeSelection() {
        ReplayPlayer.currentEditorState?.clearKeyframeSelection()
    }

    fun selectKeyframe(trackId: String, timestampNanos: Long): Boolean {
        if (ReplayPlayer.currentEditorState?.selectKeyframe(trackId, timestampNanos) != true) return false
        ReplayPlayer.currentEditorState?.activeTrackId = trackId
        return true
    }

    fun addKeyframeToActiveTrack(): Boolean = when (activeTrackId) {
        "camera" -> addCameraKeyframe()
        "fov" -> addFovKeyframe(ReplayPlayer.editorFov() ?: Minecraft.getMinecraft().gameSettings.fovSetting)
        "speed" -> addSpeedKeyframe(ReplayPlayer.automationSpeed.toFloat())
        "markers" -> addMarker()
        else -> false
    }

    fun moveSelectedKeyframe(timestampNanos: Long): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        if (!editor.moveSelectedKeyframe(timestampNanos.coerceIn(0L, ReplayPlayer.totalDurationNanos))) return false
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun deleteSelectedKeyframe(): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        if (!editor.deleteSelectedKeyframe()) return false
        ReplayPlayer.saveEditorEdits(editor)
        ReplayPlayer.refreshCameraTrack()
        return true
    }

    fun setSelectedFloatValue(value: Float): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        if (!value.isFinite()) return false
        if (!runCatching { editor.setSelectedFloatValue(value) }.getOrDefault(false)) return false
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun setSelectedCameraField(field: String, value: Double): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        val keyframe = editor.selectedCameraKeyframe() ?: return false
        if (!value.isFinite()) return false
        val updated = when (field) {
            "X" -> keyframe.copy(x = value)
            "Y" -> keyframe.copy(y = value)
            "Z" -> keyframe.copy(z = value)
            "Yaw" -> keyframe.copy(yaw = value.toFloat())
            "Pitch" -> keyframe.copy(pitch = value.toFloat())
            else -> return false
        }
        if (!updated.yaw.isFinite() || !updated.pitch.isFinite()) return false
        editor.addCameraKeyframe(updated)
        ReplayPlayer.saveEditorEdits(editor)
        ReplayPlayer.refreshCameraTrack()
        return true
    }

    fun addSpeedKeyframe(value: Float): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        if (!ReplayPlayer.playing || !runCatching { editor.addSpeedKeyframe(ReplayPlayer.currentTimeNanos, value) }.isSuccess) return false
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun addCameraKeyframe(): Boolean {
        val pose = ReplayPlayer.currentCameraPose() ?: return false
        val editor = ReplayPlayer.currentEditorState ?: return false
        editor.addCameraKeyframe(ReplayCameraKeyframe(ReplayPlayer.currentTimeNanos, pose.x, pose.y, pose.z, pose.yaw, pose.pitch))
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun addFovKeyframe(fov: Float): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        if (!ReplayPlayer.playing || !fov.isFinite() || fov !in 1.0f..179.0f) return false
        editor.addFovKeyframe(ReplayPlayer.currentTimeNanos, fov)
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun updateSelectedCameraKeyframePose(): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        val timestamp = editor.selectedKeyframeTimeNanos ?: return false
        val pose = ReplayPlayer.currentCameraPose() ?: return false
        editor.addCameraKeyframe(ReplayCameraKeyframe(timestamp, pose.x, pose.y, pose.z, pose.yaw, pose.pitch))
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun addMarker(): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        editor.addMarker(ReplayPlayer.currentTimeNanos, "Marker ${editor.markerCount() + 1}")
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun setInPoint(): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        editor.setInPoint(ReplayPlayer.currentTimeNanos)
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun setOutPoint(): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        editor.setOutPoint(ReplayPlayer.currentTimeNanos)
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun clearInOutRange(): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        editor.clearRange()
        ReplayPlayer.saveEditorEdits(editor)
        return true
    }

    fun toggleTimelineFilter(type: ReplayTimelineEventType): Boolean {
        val editor = ReplayPlayer.currentEditorState ?: return false
        when (type) {
            ReplayTimelineEventType.PACKET -> editor.showPacketEvents = !editor.showPacketEvents
            ReplayTimelineEventType.CHECKPOINT -> editor.showCheckpointEvents = !editor.showCheckpointEvents
            ReplayTimelineEventType.EVENT -> editor.showMarkers = !editor.showMarkers
            ReplayTimelineEventType.CAMERA_KEYFRAME -> editor.showCameraKeyframes = !editor.showCameraKeyframes
            ReplayTimelineEventType.FOV_KEYFRAME -> editor.showFovKeyframes = !editor.showFovKeyframes
        }
        return true
    }
}
