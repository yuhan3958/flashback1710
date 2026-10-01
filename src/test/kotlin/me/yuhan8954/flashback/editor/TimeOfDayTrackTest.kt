package me.yuhan8954.flashback.editor

import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.editor.track.ReplayKeyframe
import me.yuhan8954.flashback.editor.track.ReplayTrack
import me.yuhan8954.flashback.editor.track.type.TimeOfDayTrackType
import me.yuhan8954.flashback.io.ReplayEditStore
import me.yuhan8954.flashback.replay.visual.ReplayVisualOverrides
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeOfDayTrackTest {
    @Test
    fun evaluationAndWraparound() {
        val track = ReplayTrack(TimeOfDayTrackType.id, TimeOfDayTrackType)
        assertNull(track.evaluate(5L))
        track.put(ReplayKeyframe(0L, 6000))
        assertEquals(6000, track.evaluate(100L))
        track.put(ReplayKeyframe(0L, 0))
        track.put(ReplayKeyframe(10L, 12000))
        assertEquals(6000, track.evaluate(5L))
        track.put(ReplayKeyframe(0L, 6000))
        track.put(ReplayKeyframe(10L, 18000))
        assertEquals(12000, track.evaluate(5L))
        track.updateInterpolation(0L, ReplayInterpolation.HOLD)
        assertEquals(6000, track.evaluate(5L))
        track.put(ReplayKeyframe(0L, 23000))
        track.put(ReplayKeyframe(10L, 1000))
        assertEquals(0, track.evaluate(5L))
        track.updateInterpolation(0L, ReplayInterpolation.SMOOTH)
        assertEquals(0, track.evaluate(5L))
        assertEquals(1000, track.evaluate(10L))

        track.put(ReplayKeyframe(20L, 3000, ReplayInterpolation.SMOOTH))
        track.put(ReplayKeyframe(30L, 5000))
        track.updateInterpolation(10L, ReplayInterpolation.SMOOTH)
        for (time in 10L..20L) {
            val ticks = track.evaluate(time)!!
            assertEquals(true, ticks in 1000..3000)
        }
    }

    @Test
    fun visualStateClearsOnStop() {
        val visual = ReplayVisualOverrides()
        visual.setTimeOfDay(6000)
        assertNull(visual.timeOfDay())
        visual.activate()
        assertEquals(6000, visual.timeOfDay())
        visual.renderHud = false
        visual.clear()
        assertNull(visual.timeOfDay())
        assertEquals(true, visual.renderHud)
    }

    @Test
    fun genericPersistenceKeepsTimeAndOlderProjectsLoad() {
        val directory = Files.createTempDirectory("flashback-time-track").toFile()
        try {
            val replay = File(directory, "test.mcpr")
            val editor = ReplayEditorState(emptyList(), emptyList())
            editor.addCameraKeyframe(ReplayCameraKeyframe(0L, 0.0, 0.0, 0.0, 0f, 0f))
            editor.addFovKeyframe(1L, 70f)
            editor.addSpeedKeyframe(2L, 1f)
            editor.addTimeOfDayKeyframe(3L, 23000)
            editor.updateSelectedInterpolation(ReplayInterpolation.SMOOTH)
            ReplayEditStore.save(replay, editor)
            val loaded = ReplayEditorState(emptyList(), emptyList())
            ReplayEditStore.load(replay, loaded)
            assertEquals(0L, loaded.project.cameraTrack.keyframeAt(0L)?.timestampNanos)
            assertEquals(70f, loaded.project.fovTrack.keyframeAt(1L)?.value)
            assertEquals(1f, loaded.project.speedTrack.keyframeAt(2L)?.value)
            assertEquals(23000, loaded.project.timeOfDayTrack.keyframeAt(3L)?.value)
            assertEquals(ReplayInterpolation.SMOOTH, loaded.project.timeOfDayTrack.keyframeAt(3L)?.interpolation)
        } finally {
            directory.deleteRecursively()
        }
    }
}
