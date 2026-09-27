package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.editor.ReplayCameraKeyframe
import me.yuhan8954.flashback.editor.ReplayEditorState
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
            original.addMarker(30L, "Cut")
            original.setInPoint(10L)
            original.setOutPoint(40L)

            ReplayEditStore.save(replay, original)
            val reopened = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, reopened)

            assertEquals(original.cameraKeyframes(), reopened.cameraKeyframes())
            assertEquals(original.markers(), reopened.markers())
            assertEquals(10L, reopened.inPointNanos)
            assertEquals(40L, reopened.outPointNanos)
        } finally {
            directory.deleteRecursively()
        }
    }
}
