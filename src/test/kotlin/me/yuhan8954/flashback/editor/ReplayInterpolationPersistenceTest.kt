package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.io.ReplayEditStore
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayInterpolationPersistenceTest {
    @Test
    fun modesSurviveEditStoreRoundTrip() {
        val directory = Files.createTempDirectory("flashback-interpolation").toFile()
        try {
            val replay = File(directory, "test.mcpr")
            val editor = ReplayEditorState(emptyList(), emptyList())
            editor.addCameraKeyframe(ReplayCameraKeyframe(0L, 0.0, 0.0, 0.0, 0f, 0f))
            editor.updateSelectedInterpolation(ReplayInterpolation.SMOOTH)
            editor.addFovKeyframe(0L, 70f)
            editor.updateSelectedInterpolation(ReplayInterpolation.HOLD)
            editor.addSpeedKeyframe(0L, 1f)
            ReplayEditStore.save(replay, editor)
            val savedBytes = File(directory, "test.mcpr.fbe").readBytes().toString(Charsets.ISO_8859_1)
            assertEquals(false, savedBytes.contains("SMOOTH"))
            assertEquals(true, savedBytes.contains("smooth"))
            val loaded = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, loaded)
            assertEquals(ReplayInterpolation.SMOOTH, loaded.project.cameraTrack.keyframeAt(0L)?.interpolation)
            assertEquals(ReplayInterpolation.HOLD, loaded.project.fovTrack.keyframeAt(0L)?.interpolation)
            assertEquals(ReplayInterpolation.LINEAR, loaded.project.speedTrack.keyframeAt(0L)?.interpolation)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun legacyUppercaseIdLoadsFromV2() {
        val directory = Files.createTempDirectory("flashback-interpolation-legacy").toFile()
        try {
            val replay = File(directory, "test.mcpr")
            val trackBytes = ByteArrayOutputStream().also { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.writeInt(1)
                    output.writeLong(0L)
                    output.writeUTF("LINEAR")
                    output.writeFloat(70f)
                }
            }.toByteArray()
            DataOutputStream(File(directory, "test.mcpr.fbe").outputStream()).use { output ->
                output.writeInt(2)
                output.writeInt(1)
                output.writeUTF("fov")
                output.writeInt(trackBytes.size)
                output.write(trackBytes)
                output.writeLong(-1L)
                output.writeLong(-1L)
                output.writeInt(0)
            }
            val loaded = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, loaded)
            assertEquals(ReplayInterpolation.LINEAR, loaded.project.fovTrack.keyframeAt(0L)?.interpolation)
        } finally {
            directory.deleteRecursively()
        }
    }
}
