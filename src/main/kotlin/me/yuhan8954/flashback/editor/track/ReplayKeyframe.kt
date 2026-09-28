package me.yuhan8954.flashback.editor.track

data class ReplayKeyframe<T>(
    val timestampNanos: Long,
    val value: T,
    val interpolation: ReplayInterpolation = ReplayInterpolation.LINEAR,
)
