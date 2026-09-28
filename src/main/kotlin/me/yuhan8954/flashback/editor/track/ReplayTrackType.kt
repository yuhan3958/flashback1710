package me.yuhan8954.flashback.editor.track

import java.io.DataInput
import java.io.DataOutput

interface ReplayTrackType<T> {
    val id: String

    fun evaluate(keyframes: List<ReplayKeyframe<T>>, timestampNanos: Long): T?

    fun writeValue(output: DataOutput, value: T)

    fun readValue(input: DataInput): T
}
