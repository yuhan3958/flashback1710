package me.yuhan8954.flashback.editor

import kotlin.math.abs

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

    private val cameraKeyframes =
        mutableListOf<ReplayCameraKeyframe>()

    var selectedKeyframeTimeNanos: Long? = null
        private set

    var showPacketEvents = false
    var showCheckpointEvents = false
    var showMarkers = true
    var showCameraKeyframes = true

    var inPointNanos:
        Long? =
        null
        private set

    var outPointNanos:
        Long? =
        null
        private set

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
        cameraKeyframes.removeAll {
            it.timestampNanos ==
                timestamp
        }

        cameraKeyframes +=
            keyframe.copy(
                timestampNanos =
                timestamp,
            )

        cameraKeyframes.sortBy {
            it.timestampNanos
        }
        selectedKeyframeTimeNanos = timestamp
    }

    fun selectCameraKeyframe(timestampNanos: Long): Boolean {
        if (cameraKeyframes.none { it.timestampNanos == timestampNanos }) return false
        selectedKeyframeTimeNanos = timestampNanos
        return true
    }

    fun clearCameraKeyframeSelection() {
        selectedKeyframeTimeNanos = null
    }

    fun selectedCameraKeyframe(): ReplayCameraKeyframe? = cameraKeyframes.firstOrNull {
        it.timestampNanos == selectedKeyframeTimeNanos
    }

    fun deleteSelectedCameraKeyframe(): Boolean {
        val timestamp = selectedKeyframeTimeNanos ?: return false
        val removed = cameraKeyframes.removeAll { it.timestampNanos == timestamp }
        selectedKeyframeTimeNanos = null
        return removed
    }

    fun moveSelectedCameraKeyframe(timestampNanos: Long): Boolean {
        val selected = selectedCameraKeyframe() ?: return false
        cameraKeyframes.remove(selected)
        addCameraKeyframe(selected.copy(timestampNanos = timestampNanos))
        return true
    }

    fun cameraKeyframeTimes(): List<Long> = cameraKeyframes.map { it.timestampNanos }

    fun cameraKeyframes(): List<ReplayCameraKeyframe> = cameraKeyframes.toList()

    fun cameraPoseAt(
        timestampNanos: Long,
    ): ReplayCameraPose? {
        if (cameraKeyframes.isEmpty()) {
            return null
        }

        val before =
            cameraKeyframes
                .lastOrNull {
                    it.timestampNanos <=
                        timestampNanos
                }
                ?: cameraKeyframes.first()

        val after =
            cameraKeyframes
                .firstOrNull {
                    it.timestampNanos >=
                        timestampNanos
                }
                ?: cameraKeyframes.last()

        if (
            before.timestampNanos ==
            after.timestampNanos
        ) {
            return before.toPose()
        }

        val interpolation =
            (
                timestampNanos -
                    before.timestampNanos
                ).toDouble() /
                (
                    after.timestampNanos -
                        before.timestampNanos
                    ).toDouble()

        return ReplayCameraPose(
            x =
            lerp(
                before.x,
                after.x,
                interpolation,
            ),
            y =
            lerp(
                before.y,
                after.y,
                interpolation,
            ),
            z =
            lerp(
                before.z,
                after.z,
                interpolation,
            ),
            yaw =
            lerpAngle(
                before.yaw,
                after.yaw,
                interpolation,
            ),
            pitch =
            lerp(
                before.pitch.toDouble(),
                after.pitch.toDouble(),
                interpolation,
            ).toFloat(),
        )
    }

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
                cameraKeyframes.map {
                    ReplayTimelineEvent(
                        it.timestampNanos,
                        ReplayTimelineEventType.CAMERA_KEYFRAME,
                    )
                },
            )
        }
    }

    fun markerCount(): Int = markers.size

    fun markers(): List<ReplayMarker> = markers.toList()

    fun cameraKeyframeCount(): Int = cameraKeyframes.size

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

    private fun ReplayCameraKeyframe.toPose(): ReplayCameraPose = ReplayCameraPose(
        x,
        y,
        z,
        yaw,
        pitch,
    )

    companion object {

        private fun lerp(
            older: Double,
            newer: Double,
            interpolation: Double,
        ): Double = older +
            (
                newer -
                    older
                ) *
            interpolation.coerceIn(
                0.0,
                1.0,
            )

        private fun lerpAngle(
            older: Float,
            newer: Float,
            interpolation: Double,
        ): Float {
            var difference =
                newer -
                    older

            while (
                difference <
                -180.0f
            ) {
                difference +=
                    360.0f
            }

            while (
                difference >=
                180.0f
            ) {
                difference -=
                    360.0f
            }

            val result =
                older +
                    difference *
                    interpolation
                        .coerceIn(
                            0.0,
                            1.0,
                        )

            if (
                abs(
                    result,
                ) <
                0.000001
            ) {
                return 0.0f
            }

            return result.toFloat()
        }
    }
}
