package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.editor.ReplayCameraKeyframe
import me.yuhan8954.flashback.editor.ReplayEditorState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Editor edits live beside the immutable replay stream. */
object ReplayEditStore {

    private const val VERSION = 2
    private const val MAX_KEYFRAMES = 100_000
    private const val MAX_MARKERS = 100_000
    private const val MAX_TRACKS = 100
    private const val MAX_TRACK_BYTES = 8_000_000

    fun load(replayFile: File, editor: ReplayEditorState) {
        val edits = editFile(replayFile)
        if (!edits.isFile) return
        DataInputStream(edits.inputStream().buffered()).use { input ->
            editor.clearPersistentEdits()
            when (input.readInt()) {
                1 -> loadV1Tracks(input, editor)
                VERSION -> loadV2Tracks(input, editor)
                else -> error("Unsupported replay edit version")
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
            editor.clearKeyframeSelection()
        }
    }

    fun save(replayFile: File, editor: ReplayEditorState) {
        val edits = editFile(replayFile)
        val temporary = File(edits.parentFile, edits.name + ".tmp")
        DataOutputStream(temporary.outputStream().buffered()).use { output ->
            output.writeInt(VERSION)
            val tracks = editor.project.tracks()
            require(tracks.size <= MAX_TRACKS) { "Too many editor tracks" }
            output.writeInt(tracks.size)
            tracks.forEach { track ->
                require(track.size <= MAX_KEYFRAMES) { "Too many keyframes" }
                val bytes = ByteArrayOutputStream().also { buffer ->
                    DataOutputStream(buffer).use { track.writeKeyframes(it) }
                }.toByteArray()
                require(bytes.size <= MAX_TRACK_BYTES) { "Editor track too large" }
                output.writeUTF(track.id)
                output.writeInt(bytes.size)
                output.write(bytes)
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

    private fun loadV1Tracks(input: DataInputStream, editor: ReplayEditorState) {
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
    }

    private fun loadV2Tracks(input: DataInputStream, editor: ReplayEditorState) {
        val count = input.readInt()
        require(count in 0..MAX_TRACKS) { "Invalid editor track count" }
        val seenIds = mutableSetOf<String>()
        repeat(count) {
            val id = input.readUTF()
            require(seenIds.add(id)) { "Duplicate editor track: $id" }
            val length = input.readInt()
            require(length in 0..MAX_TRACK_BYTES) { "Invalid editor track length" }
            val bytes = ByteArray(length)
            input.readFully(bytes)
            val track = editor.project.track(id)
            if (track != null) {
                DataInputStream(ByteArrayInputStream(bytes)).use { trackInput ->
                    track.readKeyframes(trackInput, MAX_KEYFRAMES)
                    require(trackInput.available() == 0) { "Unexpected editor track data" }
                }
            }
        }
    }
}
