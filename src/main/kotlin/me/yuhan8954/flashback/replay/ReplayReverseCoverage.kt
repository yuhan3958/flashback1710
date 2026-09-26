package me.yuhan8954.flashback.replay

data class ReplayReverseCoverage(
    val startTimeNanos: Long,
    val endTimeNanos: Long,
) {

    init {
        require(
            startTimeNanos <=
                endTimeNanos,
        ) {
            "Invalid reverse coverage: $startTimeNanos..$endTimeNanos"
        }
    }

    fun contains(
        targetTimeNanos: Long,
    ): Boolean =
        targetTimeNanos in
            startTimeNanos..endTimeNanos
}

enum class ReplayReversePath {
    IN_PLACE,
    REBUILD,
}

object ReplayReversePlanner {

    fun choose(
        targetTimeNanos: Long,
        mutationCoverage: ReplayReverseCoverage?,
        frameCoverage: ReplayReverseCoverage?,
    ): ReplayReversePath {
        if (
            mutationCoverage?.contains(
                targetTimeNanos,
            ) != true
        ) {
            return ReplayReversePath.REBUILD
        }

        if (
            frameCoverage?.contains(
                targetTimeNanos,
            ) != true
        ) {
            return ReplayReversePath.REBUILD
        }

        return ReplayReversePath.IN_PLACE
    }
}
