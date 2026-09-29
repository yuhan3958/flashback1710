package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayKeyframe

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
            "Marker",
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
        project.cameraTrack.put(ReplayKeyframe(timestamp, keyframe.toPose()))
        selectedKeyframe = ReplayKeyframeSelection("camera", timestamp)
    }

    fun selectCameraKeyframe(timestampNanos: Long): Boolean {
        if (project.cameraTrack.keyframeAt(timestampNanos) == null) return false
        selectedKeyframe = ReplayKeyframeSelection("camera", timestampNanos)
        return true
    }

    fun clearCameraKeyframeSelection() {
        clearKeyframeSelection()
    }

    fun clearKeyframeSelection() {
        selectedKeyframe = null
    }

    fun selectedCameraKeyframe(): ReplayCameraKeyframe? = selectedKeyframeTimeNanos
        ?.let { project.cameraTrack.keyframeAt(it) }
        ?.toCameraKeyframe()

    fun deleteSelectedCameraKeyframe(): Boolean {
        val timestamp = selectedKeyframeTimeNanos ?: return false
        val removed = project.cameraTrack.delete(timestamp)
        selectedKeyframe = null
        return removed
    }

    fun moveSelectedCameraKeyframe(timestampNanos: Long): Boolean {
        val selected = selectedKeyframeTimeNanos ?: return false
        if (!project.cameraTrack.move(selected, timestampNanos)) return false
        selectedKeyframe = ReplayKeyframeSelection("camera", timestampNanos.coerceAtLeast(0L))
        return true
    }

    fun cameraKeyframeTimes(): List<Long> = project.cameraTrack.keyframes().map { it.timestampNanos }

    fun cameraKeyframes(): List<ReplayCameraKeyframe> = project.cameraTrack.keyframes().map { it.toCameraKeyframe() }

    fun cameraPoseAt(
        timestampNanos: Long,
    ): ReplayCameraPose? = project.cameraTrack.evaluate(timestampNanos)

    fun addFovKeyframe(timestampNanos: Long, fov: Float) {
        project.fovTrack.put(ReplayKeyframe(timestampNanos, fov))
        selectedKeyframe = ReplayKeyframeSelection("fov", timestampNanos.coerceAtLeast(0L))
    }

    fun addSpeedKeyframe(timestampNanos: Long, speed: Float) {
        project.speedTrack.put(ReplayKeyframe(timestampNanos, speed))
        selectedKeyframe = ReplayKeyframeSelection("speed", timestampNanos.coerceAtLeast(0L))
    }

    fun speedAt(timestampNanos: Long): Float = project.speedTrack.evaluate(timestampNanos) ?: 1.0f

    fun selectKeyframe(trackId: String, timestampNanos: Long): Boolean {
        val exists = when (trackId) {
            "camera" -> project.cameraTrack.keyframeAt(timestampNanos) != null
            "fov" -> project.fovTrack.keyframeAt(timestampNanos) != null
            "speed" -> project.speedTrack.keyframeAt(timestampNanos) != null
            else -> false
        }
        if (exists) selectedKeyframe = ReplayKeyframeSelection(trackId, timestampNanos)
        return exists
    }

    fun deleteSelectedKeyframe(): Boolean {
        val selection = selectedKeyframe ?: return false
        val removed = when (selection.trackId) {
            "camera" -> project.cameraTrack.delete(selection.timestampNanos)
            "fov" -> project.fovTrack.delete(selection.timestampNanos)
            "speed" -> project.speedTrack.delete(selection.timestampNanos)
            else -> false
        }
        if (removed) selectedKeyframe = null
        return removed
    }

    fun moveSelectedKeyframe(timestampNanos: Long): Boolean {
        val selection = selectedKeyframe ?: return false
        val target = timestampNanos.coerceAtLeast(0L)
        val moved = when (selection.trackId) {
            "camera" -> project.cameraTrack.move(selection.timestampNanos, target)
            "fov" -> project.fovTrack.move(selection.timestampNanos, target)
            "speed" -> project.speedTrack.move(selection.timestampNanos, target)
            else -> false
        }
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
            "fov" -> project.fovTrack.put(ReplayKeyframe(selection.timestampNanos, value))
            "speed" -> project.speedTrack.put(ReplayKeyframe(selection.timestampNanos, value))
            else -> return false
        }
        return true
    }

    fun deleteFovKeyframe(timestampNanos: Long): Boolean = project.fovTrack.delete(timestampNanos)

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
