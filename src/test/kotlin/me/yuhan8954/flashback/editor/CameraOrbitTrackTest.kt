package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrack
import me.yuhan8954.flashback.editor.track.type.CameraOrbitTrackType
import me.yuhan8954.flashback.io.ReplayEditStore
import java.io.File
import java.nio.file.Files
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraOrbitTrackTest {
    private fun orbit(yaw: Float = 0f, pitch: Float = 0f, distance: Double = 10.0) =
        ReplayCameraOrbit(2.0, 3.0, 4.0, distance, yaw, pitch)

    @Test
    fun poseStaysAtDistanceAndLooksAtCenter() {
        for ((yaw, pitch) in listOf(0f to 0f, 90f to 0f, 180f to 0f, 45f to 30f, -90f to -45f)) {
            val value = orbit(yaw, pitch)
            val pose = ReplayCameraOrbitMath.toCameraPose(value)
            val dx = value.centerX - pose.x
            val dy = value.centerY - pose.y
            val dz = value.centerZ - pose.z
            assertEquals(value.distance, hypot(hypot(dx, dz), dy), 1e-8)
            val yawRadians = Math.toRadians(yaw.toDouble())
            val pitchRadians = Math.toRadians(pitch.toDouble())
            assertEquals(-sin(yawRadians) * cos(pitchRadians), dx / value.distance, 1e-8)
            assertEquals(-sin(pitchRadians), dy / value.distance, 1e-8)
            assertEquals(cos(yawRadians) * cos(pitchRadians), dz / value.distance, 1e-8)
            assertEquals(yaw, pose.yaw)
            assertEquals(pitch, pose.pitch)
        }
    }

    @Test
    fun yawKeepsFullRevolutions() {
        val track = ReplayTrack(CameraOrbitTrackType.id, CameraOrbitTrackType)
        track.put(ReplayKeyframe(0L, orbit(0f)))
        track.put(ReplayKeyframe(10L, orbit(360f)))
        assertEquals(180f, track.evaluate(5L)?.yaw)
        track.put(ReplayKeyframe(10L, orbit(720f)))
        assertEquals(360f, track.evaluate(5L)?.yaw)
    }

    @Test
    fun interpolationAndValidation() {
        val track = ReplayTrack(CameraOrbitTrackType.id, CameraOrbitTrackType)
        val first = orbit(0f, -30f, 4.0)
        val last = orbit(360f, 30f, 12.0).copy(centerX = 12.0)
        track.put(ReplayKeyframe(0L, first, ReplayInterpolation.HOLD))
        track.put(ReplayKeyframe(10L, last))
        assertEquals(first, track.evaluate(5L))
        track.updateInterpolation(0L, ReplayInterpolation.LINEAR)
        val middle = assertNotNull(track.evaluate(5L))
        assertEquals(7.0, middle.centerX, 1e-8)
        assertEquals(8.0, middle.distance, 1e-8)
        assertEquals(180f, middle.yaw)
        assertEquals(0f, middle.pitch)
        track.updateInterpolation(0L, ReplayInterpolation.SMOOTH)
        for (time in 0L..10L) {
            val value = assertNotNull(track.evaluate(time))
            assertTrue(value.distance.isFinite() && value.distance >= ReplayCameraOrbitMath.MIN_DISTANCE)
            assertTrue(value.yaw.isFinite() && value.pitch.isFinite())
        }
        assertEquals(first, track.evaluate(0L))
        assertEquals(last, track.evaluate(10L))
        assertFailsWith<IllegalArgumentException> { track.put(ReplayKeyframe(20L, orbit(distance = Double.NaN))) }
        assertFailsWith<IllegalArgumentException> { track.put(ReplayKeyframe(20L, orbit(distance = -1.0))) }
        assertFailsWith<IllegalArgumentException> { track.put(ReplayKeyframe(20L, orbit(distance = 0.0))) }
    }

    @Test
    fun cameraSourcePriority() {
        val editor = ReplayEditorState(emptyList(), emptyList())
        assertNull(ReplayCameraEvaluator.evaluate(editor, 0L))
        editor.addCameraKeyframe(ReplayCameraKeyframe(0L, 1.0, 2.0, 3.0, 4f, 5f))
        assertEquals(1.0, ReplayCameraEvaluator.evaluate(editor, 0L)?.x)
        editor.addCameraOrbitKeyframe(0L, orbit())
        assertEquals(ReplayCameraOrbitMath.toCameraPose(orbit()), ReplayCameraEvaluator.evaluate(editor, 0L))
    }

    @Test
    fun orbitPersistsWithoutChangingSidecarVersion() {
        val directory = Files.createTempDirectory("flashback-orbit").toFile()
        try {
            val replay = File(directory, "orbit.fbr")
            val editor = ReplayEditorState(emptyList(), emptyList())
            editor.addCameraOrbitKeyframe(4L, orbit(720f, 25f))
            editor.updateSelectedInterpolation(ReplayInterpolation.SMOOTH)
            ReplayEditStore.save(replay, editor)
            val loaded = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, loaded)
            val keyframe = assertNotNull(loaded.project.cameraOrbitTrack.keyframeAt(4L))
            assertEquals(720f, keyframe.value.yaw)
            assertEquals(ReplayInterpolation.SMOOTH, keyframe.interpolation)
        } finally {
            directory.deleteRecursively()
        }
    }
}
