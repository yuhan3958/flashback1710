package me.yuhan8954.flashback.hud

import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft
import java.util.Locale

object ReplayStatusHud {

    fun appendTo(lines: MutableList<String>) {
        val minecraft =
            Minecraft.getMinecraft()

        if (
            !ReplayPlayer.playing ||
            minecraft.gameSettings.showDebugInfo
        ) {
            return
        }

        lines.add(
            "§bFlashback Replay",
        )
        lines.add(
            "§fState: ${playbackState()}",
        )
        lines.add(
            "§fTime: ${formatTime(ReplayPlayer.currentTimeNanos)}",
        )
        lines.add(
            "§fSpeed: ${formatMultiplier(ReplayPlayer.speed)}",
        )
        lines.add(
            "§fPackets: ${ReplayPlayer.processedPacketCount}" +
                "/${ReplayPlayer.totalPacketCount}",
        )
        lines.add(
            cameraLine(),
        )
    }

    private fun playbackState(): String =
        if (ReplayPlayer.paused) {
            "§ePaused"
        } else {
            "§aPlaying"
        }

    private fun cameraLine(): String {
        if (!ReplayPlayer.freeCameraActive) {
            return "§fCamera: Player"
        }

        return "§fCamera: Free §7(" +
            formatDecimal(
                ReplayPlayer.cameraSpeed ?: 0.0,
            ) +
            ")"
    }

    private fun formatTime(nanoseconds: Long): String {
        val totalMilliseconds =
            nanoseconds / NANOS_PER_MILLISECOND

        val minutes =
            totalMilliseconds /
                MILLIS_PER_MINUTE

        val seconds =
            totalMilliseconds %
                MILLIS_PER_MINUTE /
                MILLIS_PER_SECOND

        val milliseconds =
            totalMilliseconds %
                MILLIS_PER_SECOND

        return String.format(
            Locale.ROOT,
            "%02d:%02d.%03d",
            minutes,
            seconds,
            milliseconds,
        )
    }

    private fun formatMultiplier(speed: Double): String =
        "${formatDecimal(speed)}x"

    private fun formatDecimal(value: Double): String =
        String.format(
            Locale.ROOT,
            "%.2f",
            value,
        ).trimEnd('0')
            .trimEnd('.')

    private const val NANOS_PER_MILLISECOND =
        1_000_000L

    private const val MILLIS_PER_SECOND =
        1_000L

    private const val MILLIS_PER_MINUTE =
        60_000L
}
