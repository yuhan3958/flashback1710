package me.yuhan8954.flashback.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReplayEditorStateTest {

    @Test
    fun `range normalizes when out is set before in`() {
        val state =
            ReplayEditorState(
                packetTimes = emptyList(),
                checkpointTimes = emptyList(),
            )

        state.setInPoint(
            200L,
        )
        state.setOutPoint(
            100L,
        )

        assertEquals(
            100L,
            state.inPointNanos,
        )
        assertEquals(
            200L,
            state.outPointNanos,
        )
    }

    @Test
    fun `camera interpolation is linear and uses shortest yaw path`() {
        val state =
            ReplayEditorState(
                packetTimes = emptyList(),
                checkpointTimes = emptyList(),
            )

        state.addCameraKeyframe(
            ReplayCameraKeyframe(
                timestampNanos = 0L,
                x = 0.0,
                y = 10.0,
                z = 0.0,
                yaw = 170.0f,
                pitch = 0.0f,
            ),
        )
        state.addCameraKeyframe(
            ReplayCameraKeyframe(
                timestampNanos = 100L,
                x = 10.0,
                y = 20.0,
                z = 30.0,
                yaw = -170.0f,
                pitch = 20.0f,
            ),
        )

        val pose =
            assertNotNull(
                state.cameraPoseAt(
                    50L,
                ),
            )

        assertEquals(
            5.0,
            pose.x,
        )
        assertEquals(
            15.0,
            pose.y,
        )
        assertEquals(
            15.0,
            pose.z,
        )
        assertEquals(
            10.0f,
            pose.pitch,
        )
        assertTrue(
            pose.yaw ==
                180.0f ||
                pose.yaw ==
                -180.0f,
        )
    }

    @Test
    fun `camera track clamps before first and after last keyframe`() {
        val state =
            ReplayEditorState(
                packetTimes = emptyList(),
                checkpointTimes = emptyList(),
            )

        state.addCameraKeyframe(
            ReplayCameraKeyframe(
                timestampNanos = 100L,
                x = 1.0,
                y = 2.0,
                z = 3.0,
                yaw = 4.0f,
                pitch = 5.0f,
            ),
        )

        assertEquals(
            1.0,
            state.cameraPoseAt(
                0L,
            )?.x,
        )
        assertEquals(
            1.0,
            state.cameraPoseAt(
                1_000L,
            )?.x,
        )
    }

    @Test
    fun `timeline combines packets checkpoints markers and keyframes`() {
        val state =
            ReplayEditorState(
                packetTimes =
                listOf(
                    10L,
                    20L,
                ),
                checkpointTimes =
                listOf(
                    15L,
                ),
            )

        state.addMarker(
            12L,
        )
        state.addCameraKeyframe(
            ReplayCameraKeyframe(
                timestampNanos = 18L,
                x = 0.0,
                y = 0.0,
                z = 0.0,
                yaw = 0.0f,
                pitch = 0.0f,
            ),
        )

        val events =
            state.timelineEvents()

        assertEquals(
            5,
            events.size,
        )
        assertEquals(
            2,
            events.count {
                it.type ==
                    ReplayTimelineEventType.PACKET
            },
        )
        assertEquals(
            1,
            events.count {
                it.type ==
                    ReplayTimelineEventType.CHECKPOINT
            },
        )
        assertEquals(
            1,
            events.count {
                it.type ==
                    ReplayTimelineEventType.EVENT
            },
        )
        assertEquals(
            1,
            events.count {
                it.type ==
                    ReplayTimelineEventType.CAMERA_KEYFRAME
            },
        )
    }

    @Test
    fun `keyframe at same timestamp replaces previous keyframe`() {
        val state =
            ReplayEditorState(
                packetTimes = emptyList(),
                checkpointTimes = emptyList(),
            )

        state.addCameraKeyframe(
            ReplayCameraKeyframe(
                timestampNanos = 10L,
                x = 1.0,
                y = 0.0,
                z = 0.0,
                yaw = 0.0f,
                pitch = 0.0f,
            ),
        )
        state.addCameraKeyframe(
            ReplayCameraKeyframe(
                timestampNanos = 10L,
                x = 2.0,
                y = 0.0,
                z = 0.0,
                yaw = 0.0f,
                pitch = 0.0f,
            ),
        )

        assertEquals(
            1,
            state.cameraKeyframeCount(),
        )
        assertEquals(
            2.0,
            state.cameraPoseAt(
                10L,
            )?.x,
        )
    }
}
