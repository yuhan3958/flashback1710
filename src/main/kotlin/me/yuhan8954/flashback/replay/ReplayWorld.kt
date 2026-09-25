package me.yuhan8954.flashback.replay

import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.profiler.Profiler
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.WorldSettings

class ReplayWorld(
    handler: ReplayNetHandler,
    settings: WorldSettings,
    dimension: Int,
    difficulty: EnumDifficulty,
    profiler: Profiler,
) : WorldClient(
    handler,
    settings,
    dimension,
    difficulty,
    profiler,
) {

    private var replayTimeNanos =
        0L

    private var timeBaseReplayNanos =
        0L

    private var timeBaseWorldTime =
        0L

    private var timeBaseTotalWorldTime =
        0L

    private var daylightCycle =
        true

    private var completedSimulationTicks =
        0L

    private var simulationTicks =
        0

    fun initializeReplayTime(
        worldTime: Long,
        totalWorldTime: Long,
    ) {
        correctReplayTime(
            worldTime,
            totalWorldTime,
            0L,
        )
    }

    fun correctReplayTime(
        worldTime: Long,
        totalWorldTime: Long,
        currentReplayTimeNanos: Long,
    ) {
        replayTimeNanos = currentReplayTimeNanos
        timeBaseReplayNanos = currentReplayTimeNanos
        timeBaseWorldTime = normalizeWorldTime(worldTime)
        timeBaseTotalWorldTime = totalWorldTime
        daylightCycle = worldTime >= 0L
        applyReplayTime()
    }

    fun beginReplayTick(
        currentReplayTimeNanos: Long,
    ) {
        replayTimeNanos = currentReplayTimeNanos
        applyReplayTime()

        val targetSimulationTicks =
            currentReplayTimeNanos /
                ReplayClock.MINECRAFT_TICK_NANOS

        simulationTicks =
            (targetSimulationTicks - completedSimulationTicks)
                .coerceIn(
                    0L,
                    Int.MAX_VALUE.toLong(),
                ).toInt()

        completedSimulationTicks =
            targetSimulationTicks
    }

    fun seekReplayTime(currentReplayTimeNanos: Long) {
        replayTimeNanos = currentReplayTimeNanos
        completedSimulationTicks = currentReplayTimeNanos / ReplayClock.MINECRAFT_TICK_NANOS
        simulationTicks = 0
        applyReplayTime()
    }

    override fun tick() {
        repeat(simulationTicks) {
            super.tick()
            applyReplayTime()
        }
    }

    override fun updateEntities() {
        repeat(simulationTicks) {
            super.updateEntities()
        }
    }

    override fun getCelestialAngle(partialTicks: Float): Float {
        val elapsedNanos =
            (replayTimeNanos - timeBaseReplayNanos)
                .coerceAtLeast(0L)

        val elapsedTicks =
            elapsedNanos /
                ReplayClock.MINECRAFT_TICK_NANOS

        val replayPartialTick =
            if (daylightCycle) {
                (
                    elapsedNanos %
                        ReplayClock.MINECRAFT_TICK_NANOS
                    ).toFloat() /
                    ReplayClock.MINECRAFT_TICK_NANOS
            } else {
                0.0f
            }

        return provider.calculateCelestialAngle(
            timeBaseWorldTime +
                if (daylightCycle) {
                    elapsedTicks
                } else {
                    0L
                },
            replayPartialTick,
        )
    }

    private fun applyReplayTime() {
        val elapsedTicks =
            (replayTimeNanos - timeBaseReplayNanos) /
                ReplayClock.MINECRAFT_TICK_NANOS

        val worldTime =
            if (daylightCycle) {
                timeBaseWorldTime + elapsedTicks
            } else {
                timeBaseWorldTime
            }

        setWorldTime(
            worldTime,
        )

        func_82738_a(
            timeBaseTotalWorldTime + elapsedTicks,
        )
    }

    private fun normalizeWorldTime(worldTime: Long): Long = if (worldTime < 0L) {
        -worldTime
    } else {
        worldTime
    }
}
