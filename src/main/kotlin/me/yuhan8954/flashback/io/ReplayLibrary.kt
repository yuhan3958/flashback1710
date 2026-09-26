package me.yuhan8954.flashback.io

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReplayLibraryEntry(
    val file: File,
    val metadata: ReplayMetadata?,
    val status: ReplayReadStatus,
    val durationNanos: Long,
    val packetCount: Int,
    val playable: Boolean,
    val error: String? = null,
)

object ReplayLibrary {

    fun createRecordingFile(
        directory: File,
        createdAtEpochMillis: Long =
            System.currentTimeMillis(),
    ): File {
        directory.mkdirs()

        val timestamp =
            SimpleDateFormat(
                "yyyy-MM-dd_HH-mm-ss_SSS",
                Locale.ROOT,
            ).format(
                Date(
                    createdAtEpochMillis,
                ),
            )

        var suffix =
            0

        while (true) {
            val fileName =
                if (suffix == 0) {
                    "$timestamp.fbr"
                } else {
                    "$timestamp-$suffix.fbr"
                }

            val candidate =
                File(
                    directory,
                    fileName,
                )

            if (!candidate.exists()) {
                return candidate
            }

            suffix++
        }
    }

    fun list(
        directory: File,
    ): List<ReplayLibraryEntry> {
        if (!directory.exists()) {
            return emptyList()
        }

        return directory.listFiles()
            .orEmpty()
            .asSequence()
            .filter {
                it.isFile &&
                    it.extension.equals(
                        "fbr",
                        ignoreCase = true,
                    )
            }.map(
                ::readEntry,
            ).sortedWith(
                compareByDescending<ReplayLibraryEntry> {
                    it.metadata
                        ?.createdAtEpochMillis
                        ?: it.file.lastModified()
                }.thenBy {
                    it.file.name
                },
            ).toList()
    }

    private fun readEntry(
        file: File,
    ): ReplayLibraryEntry = try {
        val reader =
            ReplayReader(
                file,
            )

        ReplayLibraryEntry(
            file =
            file,
            metadata =
            reader.metadata,
            status =
            reader.status,
            durationNanos =
            reader.durationNanos,
            packetCount =
            reader.packets.size,
            playable =
            true,
        )
    } catch (
        throwable: Throwable,
    ) {
        if (
            throwable is
                VirtualMachineError
        ) {
            throw throwable
        }

        ReplayLibraryEntry(
            file =
            file,
            metadata =
            null,
            status =
            ReplayReadStatus.CORRUPT,
            durationNanos =
            0L,
            packetCount =
            0,
            playable =
            false,
            error =
            throwable.message
                ?: throwable.javaClass
                    .simpleName,
        )
    }
}
