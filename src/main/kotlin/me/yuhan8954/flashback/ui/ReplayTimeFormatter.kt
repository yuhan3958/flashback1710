package me.yuhan8954.flashback.ui

import java.util.Locale

object ReplayTimeFormatter {

    fun format(nanoseconds: Long): String {
        val totalMilliseconds =
            nanoseconds
                .coerceAtLeast(0L) /
                NANOS_PER_MILLISECOND

        val hours =
            totalMilliseconds /
                MILLIS_PER_HOUR

        val minutes =
            totalMilliseconds %
                MILLIS_PER_HOUR /
                MILLIS_PER_MINUTE

        val seconds =
            totalMilliseconds %
                MILLIS_PER_MINUTE /
                MILLIS_PER_SECOND

        val milliseconds =
            totalMilliseconds %
                MILLIS_PER_SECOND

        return if (hours > 0L) {
            String.format(
                Locale.ROOT,
                "%02d:%02d:%02d.%03d",
                hours,
                minutes,
                seconds,
                milliseconds,
            )
        } else {
            String.format(
                Locale.ROOT,
                "%02d:%02d.%03d",
                minutes,
                seconds,
                milliseconds,
            )
        }
    }

    fun formatRuler(
        nanoseconds: Long,
    ): String {
        val totalMilliseconds =
            nanoseconds
                .coerceAtLeast(
                    0L,
                ) /
                NANOS_PER_MILLISECOND

        val hours =
            totalMilliseconds /
                MILLIS_PER_HOUR

        val minutes =
            totalMilliseconds %
                MILLIS_PER_HOUR /
                MILLIS_PER_MINUTE

        val seconds =
            totalMilliseconds %
                MILLIS_PER_MINUTE /
                MILLIS_PER_SECOND

        val tenths =
            totalMilliseconds %
                MILLIS_PER_SECOND /
                100L

        return if (hours > 0L) {
            String.format(
                Locale.ROOT,
                "%d:%02d:%02d",
                hours,
                minutes,
                seconds,
            )
        } else if (
            totalMilliseconds >=
            MILLIS_PER_MINUTE
        ) {
            String.format(
                Locale.ROOT,
                "%02d:%02d",
                minutes,
                seconds,
            )
        } else {
            String.format(
                Locale.ROOT,
                "%02d.%d",
                seconds,
                tenths,
            )
        }
    }

    fun formatSpeed(speed: Double): String = String.format(
        Locale.ROOT,
        "%.2fx",
        speed,
    ).replace(
        ".00x",
        "x",
    ).replace(
        "0x",
        "x",
    )

    private const val NANOS_PER_MILLISECOND =
        1_000_000L

    private const val MILLIS_PER_SECOND =
        1_000L

    private const val MILLIS_PER_MINUTE =
        60_000L

    private const val MILLIS_PER_HOUR =
        3_600_000L
}
