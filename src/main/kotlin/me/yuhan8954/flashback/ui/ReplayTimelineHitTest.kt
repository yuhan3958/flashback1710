package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.editor.ReplayEditorTracks
import me.yuhan8954.flashback.editor.ReplayKeyframeSelection
import kotlin.math.abs

object ReplayTimelineHitTest {
    fun keyframeTrackAt(y: Int, rulerHeight: Int, rowHeight: Int): String? {
        if (y < rulerHeight || rowHeight <= 0) return null
        return ReplayEditorTracks.rows.getOrNull((y - rulerHeight) / rowHeight)?.takeIf { it.keyframed }?.id
    }

    fun nearestKeyframe(
        trackId: String,
        timestamps: List<Long>,
        mouseX: Int,
        radius: Int,
        timeToX: (Long) -> Int,
    ): ReplayKeyframeSelection? = timestamps.asSequence()
        .map { it to abs(timeToX(it) - mouseX) }
        .filter { it.second <= radius }
        .minByOrNull { it.second }
        ?.let { ReplayKeyframeSelection(trackId, it.first) }
}

/** Mouse state shared by the timeline's seek, keyframe drag, and pan paths. */
class ReplayTimelineInteraction {
    var panning = false
        private set

    private var keyframePressed = false
    private var lastPanMouseX = 0

    fun pressKeyframe(hit: Boolean) {
        keyframePressed = hit
    }

    fun draggingKeyframe(): Boolean = keyframePressed

    fun beginPan(mouseX: Int) {
        panning = true
        lastPanMouseX = mouseX
    }

    fun panDelta(mouseX: Int): Int {
        val delta = mouseX - lastPanMouseX
        lastPanMouseX = mouseX
        return delta
    }

    fun release(mouseButton: Int): Boolean = when (mouseButton) {
        0 -> {
            keyframePressed = false
            true
        }
        2 -> {
            panning = false
            true
        }
        else -> false
    }
}
