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
