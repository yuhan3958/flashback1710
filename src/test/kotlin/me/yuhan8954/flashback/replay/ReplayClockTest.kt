package me.yuhan8954.flashback.replay

import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayClockTest {

    @Test
    fun `pause and resume preserve replay time`() {
        var wallTime = 0L
        val clock =
            ReplayClock {
                wallTime
            }

        clock.reset()
        clock.resume()
        wallTime += 100_000_000L
        clock.update()
        clock.pause()
        wallTime += 500_000_000L
        clock.update()

        assertEquals(
            100_000_000L,
            clock.currentTimeNanos,
        )

        clock.resume()
        wallTime += 50_000_000L
        clock.update()

        assertEquals(
            150_000_000L,
            clock.currentTimeNanos,
        )
    }

    @Test
    fun `speed changes preserve accumulated replay time`() {
        var wallTime = 0L
        val clock =
            ReplayClock {
                wallTime
            }

        clock.reset()
        clock.resume()
        wallTime += 100_000_000L
        clock.setSpeed(
            2.0,
        )
        wallTime += 100_000_000L
        clock.update()

        assertEquals(
            300_000_000L,
            clock.currentTimeNanos,
        )
    }

    @Test
    fun `step advances exactly one Minecraft tick`() {
        val clock =
            ReplayClock {
                0L
            }

        clock.reset()
        clock.step()

        assertEquals(
            ReplayClock.MINECRAFT_TICK_NANOS,
            clock.currentTimeNanos,
        )
    }
    @Test
    fun `minus four speed rewinds at four times wall clock`() {
        var wallTime = 0L
        val clock =
            ReplayClock {
                wallTime
            }

        clock.reset()
        clock.seek(
            1_000_000_000L,
        )
        clock.setSpeed(
            -4.0,
        )
        clock.resume()

        wallTime +=
            100_000_000L

        clock.update()

        assertEquals(
            600_000_000L,
            clock.currentTimeNanos,
        )
    }

    @Test
    fun `direction changes preserve accumulated replay position`() {
        var wallTime = 0L
        val clock =
            ReplayClock {
                wallTime
            }

        clock.reset()
        clock.seek(
            1_000_000_000L,
        )
        clock.resume()

        wallTime +=
            100_000_000L

        clock.setSpeed(
            -1.0,
        )

        assertEquals(
            1_100_000_000L,
            clock.currentTimeNanos,
        )

        wallTime +=
            250_000_000L

        clock.setSpeed(
            2.0,
        )

        assertEquals(
            850_000_000L,
            clock.currentTimeNanos,
        )

        wallTime +=
            100_000_000L

        clock.update()

        assertEquals(
            1_050_000_000L,
            clock.currentTimeNanos,
        )
    }

}
