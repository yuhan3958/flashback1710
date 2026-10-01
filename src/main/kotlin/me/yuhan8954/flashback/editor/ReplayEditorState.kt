package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.type.CameraOrbitTrackType
import me.yuhan8954.flashback.editor.track.type.TimeOfDayTrackType

data class ReplayMarker(
    val timestampNanos: Long,
    val label: String,
)

data class ReplayCameraKeyframe(
    val timestampNanos: Long,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
)

data class ReplayCameraPose(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
)

enum class ReplayTimelineEventType {
    PACKET,
    CHECKPOINT,
    EVENT,
    CAMERA_KEYFRAME,
    FOV_KEYFRAME,
}

data class ReplayTimelineEvent(
    val timestampNanos: Long,
    val type: ReplayTimelineEventType,
)

data class ReplayKeyframeSelection(val trackId: String, val timestampNanos: Long)

class ReplayEditorState(
    packetTimes: List<Long>,
    checkpointTimes: List<Long>,
) {

    private val packetEvents =
        packetTimes.map {
            ReplayTimelineEvent(
                it,
                ReplayTimelineEventType.PACKET,
            )
        }

    private val checkpointEvents =
        checkpointTimes.map {
            ReplayTimelineEvent(
                it,
                ReplayTimelineEventType.CHECKPOINT,
            )
        }

    private val markers =
        mutableListOf<ReplayMarker>()

    val project = ReplayEditProject()

    var activeTrackId = "camera"

    var selectedKeyframe: ReplayKeyframeSelection? = null
        private set

    val selectedKeyframeTimeNanos: Long?
        get() = selectedKeyframe?.takeIf { it.trackId == "camera" }?.timestampNanos

    var showPacketEvents = false
    var showCheckpointEvents = false
    var showMarkers = true
    var showCameraKeyframes = true
    var showFovKeyframes = true

    var inPointNanos:
        Long? =
        null
        private set

    var outPointNanos:
        Long? =
        null
        private set

    fun clearPersistentEdits() {
        project.clear()
        markers.clear()
        clearRange()
        selectedKeyframe = null
    }

    fun setInPoint(
        timestampNanos: Long,
    ) {
        inPointNanos =
            timestampNanos.coerceAtLeast(
                0L,
            )

        normalizeRange()
    }

    fun setOutPoint(
        timestampNanos: Long,
    ) {
        outPointNanos =
            timestampNanos.coerceAtLeast(
                0L,
            )

        normalizeRange()
    }

    fun clearRange() {
        inPointNanos = null
        outPointNanos = null
    }

    fun addMarker(
        timestampNanos: Long,
        label: String =
            "",
    ) {
        markers +=
            ReplayMarker(
                timestampNanos.coerceAtLeast(
                    0L,
                ),
                label,
            )

        markers.sortBy {
            it.timestampNanos
        }
    }

    fun addCameraKeyframe(
        keyframe: ReplayCameraKeyframe,
    ) {
        val timestamp = keyframe.timestampNanos.coerceAtLeast(0L)
        val interpolation = project.cameraTrack.keyframeAt(timestamp)?.interpolation ?: ReplayInterpolation.LINEAR
        project.cameraTrack.put(ReplayKeyframe(timestamp, keyframe.toPose(), interpolation))
        selectedKeyframe = ReplayKeyframeSelection("camera", timestamp)
    }

    fun clearKeyframeSelection() {
        selectedKeyframe = null
    }

    fun selectedCameraKeyframe(): ReplayCameraKeyframe? = selectedKeyframeTimeNanos
        ?.let { project.cameraTrack.keyframeAt(it) }
        ?.toCameraKeyframe()

    fun cameraKeyframeTimes(): List<Long> = project.cameraTrack.keyframes().map { it.timestampNanos }

    fun cameraKeyframes(): List<ReplayCameraKeyframe> = project.cameraTrack.keyframes().map { it.toCameraKeyframe() }

    fun cameraPoseAt(
        timestampNanos: Long,
    ): ReplayCameraPose? = project.cameraTrack.evaluate(timestampNanos)

    fun addCameraOrbitKeyframe(timestampNanos: Long, orbit: ReplayCameraOrbit) {
        val timestamp = timestampNanos.coerceAtLeast(0L)
        val interpolation = project.cameraOrbitTrack.keyframeAt(timestamp)?.interpolation ?: ReplayInterpolation.LINEAR
        project.cameraOrbitTrack.put(ReplayKeyframe(timestamp, orbit, interpolation))
        selectedKeyframe = ReplayKeyframeSelection(CameraOrbitTrackType.id, timestamp)
    }

    fun selectedCameraOrbit(): ReplayCameraOrbit? = selectedKeyframe?.takeIf { it.trackId == CameraOrbitTrackType.id }
        ?.let { project.cameraOrbitTrack.keyframeAt(it.timestampNanos)?.value }

    fun setSelectedCameraOrbit(orbit: ReplayCameraOrbit): Boolean {
        val selection = selectedKeyframe?.takeIf { it.trackId == CameraOrbitTrackType.id } ?: return false
        val old = project.cameraOrbitTrack.keyframeAt(selection.timestampNanos) ?: return false
        project.cameraOrbitTrack.put(old.copy(value = orbit))
        return true
    }

    fun addFovKeyframe(timestampNanos: Long, fov: Float) {
        project.fovTrack.put(ReplayKeyframe(timestampNanos, fov))
        selectedKeyframe = ReplayKeyframeSelection("fov", timestampNanos.coerceAtLeast(0L))
    }

    fun addSpeedKeyframe(timestampNanos: Long, speed: Float) {
        project.speedTrack.put(ReplayKeyframe(timestampNanos, speed))
        selectedKeyframe = ReplayKeyframeSelection("speed", timestampNanos.coerceAtLeast(0L))
    }

    fun addTimeOfDayKeyframe(timestampNanos: Long, ticks: Int) {
        val timestamp = timestampNanos.coerceAtLeast(0L)
        val interpolation = project.timeOfDayTrack.keyframeAt(timestamp)?.interpolation ?: ReplayInterpolation.LINEAR
        project.timeOfDayTrack.put(ReplayKeyframe(timestamp, TimeOfDayTrackType.normalize(ticks), interpolation))
        selectedKeyframe = ReplayKeyframeSelection(TimeOfDayTrackType.id, timestamp)
    }

    fun timeOfDayAt(timestampNanos: Long): Int? = project.timeOfDayTrack.evaluate(timestampNanos)

    fun selectedTimeOfDay(): Int? = selectedKeyframe?.takeIf { it.trackId == TimeOfDayTrackType.id }
        ?.let { project.timeOfDayTrack.keyframeAt(it.timestampNanos)?.value }

    fun setSelectedTimeOfDay(ticks: Int): Boolean {
        val selection = selectedKeyframe?.takeIf { it.trackId == TimeOfDayTrackType.id } ?: return false
        val old = project.timeOfDayTrack.keyframeAt(selection.timestampNanos) ?: return false
        project.timeOfDayTrack.put(old.copy(value = TimeOfDayTrackType.normalize(ticks)))
        return true
    }

    fun speedAt(timestampNanos: Long): Float = project.speedTrack.evaluate(timestampNanos) ?: 1.0f

    fun selectKeyframe(trackId: String, timestampNanos: Long): Boolean {
        if (project.track(trackId)?.keyframeAt(timestampNanos) == null) return false
        selectedKeyframe = ReplayKeyframeSelection(trackId, timestampNanos)
        return true
    }

    fun selectedInterpolation(): ReplayInterpolation? = selectedKeyframe?.let { selection ->
        project.track(selection.trackId)?.keyframeAt(selection.timestampNanos)?.interpolation
    }

    fun updateSelectedInterpolation(interpolation: ReplayInterpolation): Boolean {
        val selection = selectedKeyframe ?: return false
        return project.track(selection.trackId)?.updateInterpolation(selection.timestampNanos, interpolation) ?: false
    }

    fun deleteSelectedKeyframe(): Boolean {
        val selection = selectedKeyframe ?: return false
        val removed = project.track(selection.trackId)?.delete(selection.timestampNanos) ?: false
        if (removed) selectedKeyframe = null
        return removed
    }

    fun moveSelectedKeyframe(timestampNanos: Long): Boolean {
        val selection = selectedKeyframe ?: return false
        val target = timestampNanos.coerceAtLeast(0L)
        val moved = project.track(selection.trackId)?.move(selection.timestampNanos, target) ?: false
        if (moved) selectedKeyframe = selection.copy(timestampNanos = target)
        return moved
    }

    fun selectedFloatValue(): Float? = selectedKeyframe?.let { selection ->
        when (selection.trackId) {
            "fov" -> project.fovTrack.keyframeAt(selection.timestampNanos)?.value
            "speed" -> project.speedTrack.keyframeAt(selection.timestampNanos)?.value
            else -> null
        }
    }

    fun setSelectedFloatValue(value: Float): Boolean {
        val selection = selectedKeyframe ?: return false
        when (selection.trackId) {
            "fov" -> {
                val old = project.fovTrack.keyframeAt(selection.timestampNanos) ?: return false
                project.fovTrack.put(old.copy(value = value))
            }
            "speed" -> {
                val old = project.speedTrack.keyframeAt(selection.timestampNanos) ?: return false
                project.speedTrack.put(old.copy(value = value))
            }
            else -> return false
        }
        return true
    }

    fun fovAt(timestampNanos: Long): Float? = project.fovTrack.evaluate(timestampNanos)

    fun fovKeyframes(): List<ReplayKeyframe<Float>> = project.fovTrack.keyframes()

    fun fovKeyframeTimes(): List<Long> = fovKeyframes().map { it.timestampNanos }

    fun timelineEvents(): List<ReplayTimelineEvent> = buildList {
        if (showPacketEvents) addAll(packetEvents)
        if (showCheckpointEvents) addAll(checkpointEvents)
        if (showMarkers) {
            addAll(
                markers.map {
                    ReplayTimelineEvent(
                        it.timestampNanos,
                        ReplayTimelineEventType.EVENT,
                    )
                },
            )
        }
        if (showCameraKeyframes) {
            addAll(
                project.cameraTrack.keyframes().map {
                    ReplayTimelineEvent(
                        it.timestampNanos,
                        ReplayTimelineEventType.CAMERA_KEYFRAME,
                    )
                },
            )
        }
        if (showFovKeyframes) {
            addAll(
                project.fovTrack.keyframes().map {
                    ReplayTimelineEvent(it.timestampNanos, ReplayTimelineEventType.FOV_KEYFRAME)
                },
            )
        }
    }

    fun markerCount(): Int = markers.size

    fun markers(): List<ReplayMarker> = markers.toList()

    fun cameraKeyframeCount(): Int = project.cameraTrack.size

    private fun normalizeRange() {
        val start =
            inPointNanos
                ?: return

        val end =
            outPointNanos
                ?: return

        if (start <= end) {
            return
        }

        inPointNanos =
            end

        outPointNanos =
            start
    }

    private fun ReplayCameraKeyframe.toPose() = ReplayCameraPose(x, y, z, yaw, pitch)

    private fun ReplayKeyframe<ReplayCameraPose>.toCameraKeyframe() = ReplayCameraKeyframe(
        timestampNanos,
        value.x,
        value.y,
        value.z,
        value.yaw,
        value.pitch,
    )
}
