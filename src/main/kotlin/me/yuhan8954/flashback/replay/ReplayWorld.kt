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

    private var advanceSimulation =
        true

    private var stepRequested =
        false

    fun beginReplayTick(paused: Boolean) {
        advanceSimulation =
            !paused ||
            stepRequested

        stepRequested = false
    }

    fun requestStep() {
        stepRequested = true
    }

    override fun tick() {
        if (advanceSimulation) {
            super.tick()
        }
    }

    override fun updateEntities() {
        if (advanceSimulation) {
            super.updateEntities()
        }
    }
}
