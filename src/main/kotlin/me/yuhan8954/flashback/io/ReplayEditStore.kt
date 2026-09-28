package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.editor.ReplayCameraKeyframe
import me.yuhan8954.flashback.editor.ReplayEditorState
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Camera edits live beside the immutable replay stream. */
object ReplayEditStore {

    private const val VERSION = 1
    private const val MAX_KEYFRAMES = 100_000
    private const val MAX_MARKERS = 100_000

    fun load(replayFile: File, editor: ReplayEditorState) {
        val edits = editFile(replayFile)
        if (!edits.isFile) return
        DataInputStream(edits.inputStream().buffered()).use { input ->
            require(input.readInt() == VERSION) { "Unsupported replay edit version" }
            val count = input.readInt()
            require(count in 0..MAX_KEYFRAMES) { "Invalid camera keyframe count" }
            repeat(count) {
                editor.addCameraKeyframe(
                    ReplayCameraKeyframe(
                        input.readLong(),
                        input.readDouble(),
                        input.readDouble(),
                        input.readDouble(),
                        input.readFloat(),
                        input.readFloat(),
                    ),
                )
            }
            val inPoint = input.readLong()
            val outPoint = input.readLong()
            if (inPoint >= 0L) editor.setInPoint(inPoint)
            if (outPoint >= 0L) editor.setOutPoint(outPoint)
            val markerCount = input.readInt()
            require(markerCount in 0..MAX_MARKERS) { "Invalid marker count" }
            repeat(markerCount) {
                editor.addMarker(input.readLong(), input.readUTF())
            }
            editor.clearCameraKeyframeSelection()
        }
    }

    fun save(replayFile: File, editor: ReplayEditorState) {
        val edits = editFile(replayFile)
        val temporary = File(edits.parentFile, edits.name + ".tmp")
        DataOutputStream(temporary.outputStream().buffered()).use { output ->
            output.writeInt(VERSION)
            val keyframes = editor.cameraKeyframes()
            output.writeInt(keyframes.size)
            keyframes.forEach {
                output.writeLong(it.timestampNanos)
                output.writeDouble(it.x)
                output.writeDouble(it.y)
                output.writeDouble(it.z)
                output.writeFloat(it.yaw)
                output.writeFloat(it.pitch)
            }
            output.writeLong(editor.inPointNanos ?: -1L)
            output.writeLong(editor.outPointNanos ?: -1L)
            val markers = editor.markers()
            output.writeInt(markers.size)
            markers.forEach {
                output.writeLong(it.timestampNanos)
                output.writeUTF(it.label)
            }
        }
        try {
            Files.move(
                temporary.toPath(),
                edits.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                edits.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun editFile(replayFile: File): File = File(replayFile.parentFile, replayFile.name + ".fbe")
}
