package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.editor.ReplayCameraKeyframe
import me.yuhan8954.flashback.editor.ReplayCameraPose
import me.yuhan8954.flashback.editor.ReplayEditorState
import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import java.io.DataOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayEditStoreTest {

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
            assertEquals(original.markers(), reopened.markers())
            assertEquals(10L, reopened.inPointNanos)
            assertEquals(40L, reopened.outPointNanos)
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
            assertEquals(10L, editor.inPointNanos)
            assertEquals(40L, editor.outPointNanos)
            assertEquals("Cut", editor.markers().single().label)
        } finally {
            directory.deleteRecursively()
        }
    }
}
