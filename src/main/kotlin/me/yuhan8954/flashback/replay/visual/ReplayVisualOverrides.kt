package me.yuhan8954.flashback.replay.visual

/** Temporary presentation settings; recorded replay and world state remain authoritative. */
class ReplayVisualOverrides {
    var active = false
        private set

    var timeOfDayOverride: Int? = null
        private set

    var renderHud = true
    var renderNametags = true
    var renderParticles = true

    fun activate() {
        active = true
    }

    fun setTimeOfDay(ticks: Int?) {
        timeOfDayOverride = ticks
    }

    fun clear() {
        active = false
        timeOfDayOverride = null
        renderHud = true
        renderNametags = true
        renderParticles = true
    }

    fun timeOfDay(): Int? = timeOfDayOverride.takeIf { active }
}
