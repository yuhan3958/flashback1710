package me.yuhan8954.flashback.editor.track

import java.io.DataInput
import java.io.DataOutput

class ReplayTrack<T>(
    val id: String,
    val type: ReplayTrackType<T>,
) {
    private val entries = mutableListOf<ReplayKeyframe<T>>()

    val size: Int get() = entries.size

    fun clear() {
        entries.clear()
    }

    fun keyframes(): List<ReplayKeyframe<T>> = entries.toList()

    fun keyframeAt(timestampNanos: Long): ReplayKeyframe<T>? = entries.firstOrNull { it.timestampNanos == timestampNanos }

    fun put(keyframe: ReplayKeyframe<T>) {
        type.validate(keyframe.value)
        val normalized = keyframe.copy(timestampNanos = keyframe.timestampNanos.coerceAtLeast(0L))
        entries.removeAll { it.timestampNanos == normalized.timestampNanos }
        entries.add(normalized)
        entries.sortBy { it.timestampNanos }
    }

    fun move(fromTimestampNanos: Long, toTimestampNanos: Long): Boolean {
        val keyframe = keyframeAt(fromTimestampNanos) ?: return false
        delete(fromTimestampNanos)
        put(keyframe.copy(timestampNanos = toTimestampNanos))
        return true
    }

    fun delete(timestampNanos: Long): Boolean = entries.removeAll { it.timestampNanos == timestampNanos }

    fun evaluate(timestampNanos: Long): T? {
        if (entries.isEmpty()) return null
        val index = entries.binarySearchBy(timestampNanos) { it.timestampNanos }
        val neighbors = when {
            index >= 0 -> entries.subList(index, index + 1)
            -index - 1 == 0 -> entries.subList(0, 1)
            -index - 1 == entries.size -> entries.subList(entries.lastIndex, entries.size)
            else -> entries.subList(-index - 2, -index)
        }
        return type.evaluate(neighbors, timestampNanos)
    }

    fun writeKeyframes(output: DataOutput) {
        output.writeInt(entries.size)
        entries.forEach { keyframe ->
            output.writeLong(keyframe.timestampNanos)
            output.writeUTF(keyframe.interpolation.serializedId)
            type.writeValue(output, keyframe.value)
        }
    }

    fun readKeyframes(input: DataInput, maximumCount: Int) {
        val count = input.readInt()
        require(count in 0..maximumCount) { "Invalid keyframe count" }
        repeat(count) {
            val timestamp = input.readLong()
            val interpolation = ReplayInterpolation.fromSerializedId(input.readUTF())
            put(ReplayKeyframe(timestamp, type.readValue(input), interpolation))
        }
    }
}
