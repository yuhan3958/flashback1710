package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.editor.ReplayCameraKeyframe
import me.yuhan8954.flashback.editor.ReplayCameraPose
import me.yuhan8954.flashback.editor.ReplayEditorState
import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayEditStoreTest {

    @Test
    fun `camera fov and speed survive a save and load`() {
        val directory = Files.createTempDirectory("flashback-all-tracks").toFile()
        try {
            val replay = directory.resolve("session.fbr")
            val source = ReplayEditorState(emptyList(), emptyList())
            source.addCameraKeyframe(ReplayCameraKeyframe(10L, 1.0, 2.0, 3.0, 4.0f, 5.0f))
            source.addFovKeyframe(10L, 75.0f)
            source.addSpeedKeyframe(10L, -0.5f)
            ReplayEditStore.save(replay, source)
            val loaded = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, loaded)
            assertEquals(1.0, loaded.cameraPoseAt(10L)?.x)
            assertEquals(75.0f, loaded.fovAt(10L))
            assertEquals(-0.5f, loaded.speedAt(10L))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `loading replaces persistent edits while keeping replay metadata and filters`() {
        val directory = Files.createTempDirectory("flashback-replace-edits").toFile()
        try {
            val replay = directory.resolve("session.fbr")
            val saved = ReplayEditorState(emptyList(), emptyList())
            saved.addCameraKeyframe(ReplayCameraKeyframe(40L, 4.0, 0.0, 0.0, 0.0f, 0.0f))
            saved.addMarker(40L, "New")
            ReplayEditStore.save(replay, saved)

            val reused = ReplayEditorState(listOf(5L), listOf(6L))
            reused.addCameraKeyframe(ReplayCameraKeyframe(20L, 2.0, 0.0, 0.0, 0.0f, 0.0f))
            reused.addFovKeyframe(20L, 80.0f)
            reused.addMarker(20L, "Old")
            reused.setInPoint(10L)
            reused.setOutPoint(30L)
            reused.showPacketEvents = true
            ReplayEditStore.load(replay, reused)

            assertEquals(listOf(40L), reused.cameraKeyframeTimes())
            assertEquals(emptyList(), reused.fovKeyframeTimes())
            assertEquals(listOf("New"), reused.markers().map { it.label })
            assertEquals(null, reused.inPointNanos)
            assertEquals(null, reused.outPointNanos)
            assertEquals(null, reused.selectedKeyframeTimeNanos)
            assertEquals(true, reused.showPacketEvents)
            assertEquals(1, reused.timelineEvents().count { it.type == me.yuhan8954.flashback.editor.ReplayTimelineEventType.PACKET })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `camera keys markers and range survive reopening`() {
        val directory = Files.createTempDirectory("flashback-edits").toFile()
        try {
            val replay = directory.resolve("session.fbr")
            val original = ReplayEditorState(emptyList(), emptyList())
            original.addCameraKeyframe(ReplayCameraKeyframe(20L, 1.0, 2.0, 3.0, 4.0f, 5.0f))
            original.project.cameraTrack.put(
                ReplayKeyframe(30L, ReplayCameraPose(6.0, 7.0, 8.0, 9.0f, 10.0f), ReplayInterpolation.LINEAR),
            )
            original.addFovKeyframe(20L, 70.0f)
            original.addFovKeyframe(40L, 30.0f)
            original.addMarker(30L, "Cut")
            original.setInPoint(10L)
            original.setOutPoint(40L)

            ReplayEditStore.save(replay, original)
            java.io.DataInputStream(replay.resolveSibling(replay.name + ".fbe").inputStream()).use {
                assertEquals(2, it.readInt())
            }
            val reopened = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, reopened)

            assertEquals(original.cameraKeyframes(), reopened.cameraKeyframes())
            assertEquals(original.project.cameraTrack.keyframes(), reopened.project.cameraTrack.keyframes())
            assertEquals(original.fovKeyframes(), reopened.fovKeyframes())
            assertEquals(original.markers(), reopened.markers())
            assertEquals(10L, reopened.inPointNanos)
            assertEquals(40L, reopened.outPointNanos)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `v2 writes stable interpolation ids and accepts legacy enum names`() {
        val directory = Files.createTempDirectory("flashback-interpolation-edits").toFile()
        try {
            val replay = directory.resolve("session.fbr")
            val editor = ReplayEditorState(emptyList(), emptyList())
            editor.addFovKeyframe(10L, 70.0f)
            ReplayEditStore.save(replay, editor)
            DataInputStream(directory.resolve("session.fbr.fbe").inputStream()).use { input ->
                assertEquals(2, input.readInt())
                assertEquals(3, input.readInt())
                input.readUTF()
                val cameraBytes = ByteArray(input.readInt())
                input.readFully(cameraBytes)
                assertEquals("fov", input.readUTF())
                val fovBytes = ByteArray(input.readInt())
                input.readFully(fovBytes)
                DataInputStream(ByteArrayInputStream(fovBytes)).use { track ->
                    assertEquals(1, track.readInt())
                    assertEquals(10L, track.readLong())
                    assertEquals("linear", track.readUTF())
                }
            }
            assertEquals(ReplayInterpolation.LINEAR, ReplayInterpolation.fromSerializedId("linear"))
            assertEquals(ReplayInterpolation.LINEAR, ReplayInterpolation.fromSerializedId("LINEAR"))

            DataOutputStream(directory.resolve("session.fbr.fbe").outputStream()).use { output ->
                output.writeInt(2)
                output.writeInt(1)
                val bytes = java.io.ByteArrayOutputStream().also { buffer ->
                    DataOutputStream(buffer).use { track ->
                        track.writeInt(1)
                        track.writeLong(15L)
                        track.writeUTF("LINEAR")
                        track.writeFloat(50.0f)
                    }
                }.toByteArray()
                output.writeUTF("fov")
                output.writeInt(bytes.size)
                output.write(bytes)
                output.writeLong(-1L)
                output.writeLong(-1L)
                output.writeInt(0)
            }
            ReplayEditStore.load(replay, editor)
            assertEquals(listOf(15L), editor.fovKeyframeTimes())
            assertEquals(50.0f, editor.fovAt(15L))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `unknown v2 track can be skipped without losing following edits`() {
        val directory = Files.createTempDirectory("flashback-unknown-track").toFile()
        try {
            val replay = directory.resolve("session.fbr")
            DataOutputStream(directory.resolve("session.fbr.fbe").outputStream()).use { output ->
                output.writeInt(2)
                output.writeInt(1)
                output.writeUTF("future-track")
                output.writeInt(4)
                output.writeInt(123)
                output.writeLong(10L)
                output.writeLong(20L)
                output.writeInt(1)
                output.writeLong(15L)
                output.writeUTF("Marker")
            }
            val editor = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, editor)
            assertEquals(emptyList(), editor.cameraKeyframeTimes())
            assertEquals(emptyList(), editor.fovKeyframeTimes())
            assertEquals(10L, editor.inPointNanos)
            assertEquals(20L, editor.outPointNanos)
            assertEquals("Marker", editor.markers().single().label)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `version one camera edits load into the camera track`() {
        val directory = Files.createTempDirectory("flashback-v1-edits").toFile()
        try {
            val replay = directory.resolve("session.fbr")
            DataOutputStream(directory.resolve("session.fbr.fbe").outputStream()).use { output ->
                output.writeInt(1)
                output.writeInt(1)
                output.writeLong(25L)
                output.writeDouble(1.0)
                output.writeDouble(2.0)
                output.writeDouble(3.0)
                output.writeFloat(4.0f)
                output.writeFloat(5.0f)
                output.writeLong(10L)
                output.writeLong(40L)
                output.writeInt(1)
                output.writeLong(30L)
                output.writeUTF("Cut")
            }
            val editor = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, editor)
            assertEquals(ReplayCameraPose(1.0, 2.0, 3.0, 4.0f, 5.0f), editor.project.cameraTrack.evaluate(25L))
            assertEquals(ReplayInterpolation.LINEAR, editor.project.cameraTrack.keyframeAt(25L)?.interpolation)
            assertEquals(emptyList(), editor.fovKeyframeTimes())
            assertEquals(10L, editor.inPointNanos)
            assertEquals(40L, editor.outPointNanos)
            assertEquals("Cut", editor.markers().single().label)
        } finally {
            directory.deleteRecursively()
        }
    }
}
