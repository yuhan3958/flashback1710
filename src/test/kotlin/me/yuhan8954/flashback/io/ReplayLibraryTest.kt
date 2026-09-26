package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.replay.RecordedPacket
import me.yuhan8954.flashback.snapshot.ReplayInventorySnapshot
import me.yuhan8954.flashback.snapshot.ReplayPlayerSnapshot
import me.yuhan8954.flashback.snapshot.ReplaySnapshot
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplayLibraryTest {

    @Test
    fun `recording filenames are unique for the same timestamp`() {
        val directory =
            Files.createTempDirectory(
                "flashback-library-names",
            ).toFile()

        val first =
            ReplayLibrary.createRecordingFile(
                directory,
                createdAtEpochMillis = 1_000L,
            )

        first.createNewFile()

        val second =
            ReplayLibrary.createRecordingFile(
                directory,
                createdAtEpochMillis = 1_000L,
            )

        assertFalse(
            first.name ==
                second.name,
        )
        assertTrue(
            second.name.endsWith(
                "-1.fbr",
            ),
        )
    }

    @Test
    fun `truncated replay remains listed as playable prefix`() {
        val directory =
            Files.createTempDirectory(
                "flashback-library-truncated",
            ).toFile()

        val file =
            ReplayLibrary.createRecordingFile(
                directory,
                createdAtEpochMillis = 2_000L,
            )

        ReplayWriter(
            file,
            snapshot(),
        ).use {
            it.write(
                RecordedPacket(
                    timestampNanos = 50L,
                    packetClass = "example.Packet",
                    payload = byteArrayOf(
                        1,
                    ),
                ),
            )
        }

        val bytes =
            file.readBytes()

        file.writeBytes(
            bytes.copyOf(
                bytes.size -
                    9,
            ),
        )

        val entry =
            ReplayLibrary.list(
                directory,
            ).single()

        assertEquals(
            ReplayReadStatus.TRUNCATED,
            entry.status,
        )
        assertTrue(
            entry.playable,
        )
        assertEquals(
            1,
            entry.packetCount,
        )
    }

    @Test
    fun `unreadable replay is visible but not playable`() {
        val directory =
            Files.createTempDirectory(
                "flashback-library-corrupt",
            ).toFile()

        val file =
            ReplayLibrary.createRecordingFile(
                directory,
                createdAtEpochMillis = 3_000L,
            )

        file.writeText(
            "not a replay",
        )

        val entry =
            ReplayLibrary.list(
                directory,
            ).single()

        assertEquals(
            ReplayReadStatus.CORRUPT,
            entry.status,
        )
        assertFalse(
            entry.playable,
        )
    }

    private fun snapshot(): ReplaySnapshot = ReplaySnapshot(
        dimensionId = 0,
        seed = 123L,
        worldTime = 0L,
        totalWorldTime = 0L,
        raining = false,
        thundering = false,
        rainStrength = 0.0f,
        thunderStrength = 0.0f,
        player = ReplayPlayerSnapshot(
            entityId = 1,
            profileId = null,
            profileName = "Tester",
            x = 0.0,
            y = 64.0,
            z = 0.0,
            yaw = 0.0f,
            pitch = 0.0f,
            motionX = 0.0,
            motionY = 0.0,
            motionZ = 0.0,
            inventory = ReplayInventorySnapshot(
                selectedSlot = 0,
                mainInventory = emptyList(),
                armorInventory = emptyList(),
            ),
        ),
        chunks = emptyList(),
        tileEntities = emptyList(),
        entities = emptyList(),
    )
}
