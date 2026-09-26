package me.yuhan8954.flashback.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayTimeFormatterTest {

    @Test
    fun `format handles milliseconds minutes and hours`() {
        assertEquals(
            "00:00.000",
            ReplayTimeFormatter.format(
                0L,
            ),
        )
        assertEquals(
            "01:02.345",
            ReplayTimeFormatter.format(
                62_345_000_000L,
            ),
        )
        assertEquals(
            "01:02:03.004",
            ReplayTimeFormatter.format(
                3_723_004_000_000L,
            ),
        )
    }

    @Test
    fun `format clamps negative replay time`() {
        assertEquals(
            "00:00.000",
            ReplayTimeFormatter.format(
                -1L,
            ),
        )
    }

    @Test
    fun `ruler chooses compact representation`() {
        assertEquals(
            "00.250",
            ReplayTimeFormatter.formatRuler(
                250_000_000L,
            ),
        )
        assertEquals(
            "01:05",
            ReplayTimeFormatter.formatRuler(
                65_000_000_000L,
            ),
        )
        assertEquals(
            "1:01:05",
            ReplayTimeFormatter.formatRuler(
                3_665_000_000_000L,
            ),
        )
    }

    @Test
    fun `speed formatting preserves supported fractions`() {
        assertEquals(
            "1x",
            ReplayTimeFormatter.formatSpeed(
                1.0,
            ),
        )
        assertEquals(
            "0.5x",
            ReplayTimeFormatter.formatSpeed(
                0.5,
            ),
        )
        assertEquals(
            "-0.25x",
            ReplayTimeFormatter.formatSpeed(
                -0.25,
            ),
        )
        assertEquals(
            "4x",
            ReplayTimeFormatter.formatSpeed(
                4.0,
            ),
        )
    }
}
