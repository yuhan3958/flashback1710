package me.yuhan8954.flashback.replay

class ReplayClock(
    private val timeSource: () -> Long =
        System::nanoTime,
) {

    var currentTimeNanos =
        0L
        private set

    private var speedMultiplier =
        DEFAULT_SPEED

    val speed: Double
        get() = speedMultiplier

    var paused =
        false
        private set

    private var lastUpdateNanos =
        timeSource()

    fun reset() {
        currentTimeNanos = 0L
        speedMultiplier = DEFAULT_SPEED
        paused = false
        lastUpdateNanos =
            timeSource()
    }

    fun update() {
        val now =
            timeSource()

        val wallTimeDelta =
            (now - lastUpdateNanos)
                .coerceAtLeast(0L)

        if (!paused) {
            currentTimeNanos +=
                (wallTimeDelta * speedMultiplier)
                    .toLong()
        }

        lastUpdateNanos = now
    }

    fun pause() {
        if (paused) {
            return
        }

        update()
        paused = true
    }

    fun resume() {
        if (!paused) {
            return
        }

        lastUpdateNanos =
            timeSource()

        paused = false
    }

    fun togglePause() {
        if (paused) {
            resume()
        } else {
            pause()
        }
    }

    fun setSpeed(speed: Double) {
        require(
            isSupportedSpeed(
                speed,
            ),
        ) {
            "Unsupported replay speed: $speed"
        }

        update()

        speedMultiplier = speed
    }

    fun step() {
        check(paused) {
            "Replay must be paused before stepping"
        }

        currentTimeNanos +=
            MINECRAFT_TICK_NANOS

        lastUpdateNanos =
            timeSource()
    }

    fun stop() {
        currentTimeNanos = 0L
        speedMultiplier = DEFAULT_SPEED
        paused = true
        lastUpdateNanos =
            timeSource()
    }

    companion object {

        const val MINECRAFT_TICK_NANOS =
            50_000_000L

        const val DEFAULT_SPEED = 1.0

        val SUPPORTED_SPEEDS =
            listOf(
                0.25,
                0.5,
                1.0,
                2.0,
                4.0,
            )

        fun isSupportedSpeed(speed: Double): Boolean = speed in SUPPORTED_SPEEDS
    }
}
