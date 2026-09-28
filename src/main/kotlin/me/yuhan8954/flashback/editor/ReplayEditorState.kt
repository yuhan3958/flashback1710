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

    var selectedKeyframeTimeNanos: Long? = null
        private set

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
        selectedKeyframeTimeNanos = null
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
        selectedKeyframeTimeNanos = timestamp
    }

    fun selectCameraKeyframe(timestampNanos: Long): Boolean {
        if (project.cameraTrack.keyframeAt(timestampNanos) == null) return false
        selectedKeyframeTimeNanos = timestampNanos
        return true
    }

    fun clearCameraKeyframeSelection() {
        selectedKeyframeTimeNanos = null
    }

    fun selectedCameraKeyframe(): ReplayCameraKeyframe? = selectedKeyframeTimeNanos
        ?.let { project.cameraTrack.keyframeAt(it) }
        ?.toCameraKeyframe()

    fun deleteSelectedCameraKeyframe(): Boolean {
        val timestamp = selectedKeyframeTimeNanos ?: return false
        val removed = project.cameraTrack.delete(timestamp)
        selectedKeyframeTimeNanos = null
        return removed
    }

    fun moveSelectedCameraKeyframe(timestampNanos: Long): Boolean {
        val selected = selectedKeyframeTimeNanos ?: return false
        if (!project.cameraTrack.move(selected, timestampNanos)) return false
        selectedKeyframeTimeNanos = timestampNanos.coerceAtLeast(0L)
        return true
    }

    fun cameraKeyframeTimes(): List<Long> = project.cameraTrack.keyframes().map { it.timestampNanos }

    fun cameraKeyframes(): List<ReplayCameraKeyframe> = project.cameraTrack.keyframes().map { it.toCameraKeyframe() }

    fun cameraPoseAt(
        timestampNanos: Long,
    ): ReplayCameraPose? = project.cameraTrack.evaluate(timestampNanos)

    fun addFovKeyframe(timestampNanos: Long, fov: Float) {
        project.fovTrack.put(ReplayKeyframe(timestampNanos, fov))
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
