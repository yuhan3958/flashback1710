package me.yuhan8954.flashback.replay

import java.util.ArrayDeque

class ReplayMutationTimeline<T>(
    private val historyDurationNanos: Long,
) {

    private val entries =
        ArrayDeque<TimestampedValue<T>>()

    private var active =
        false

    private var currentTimeNanos =
        0L

    private var retainedStartTimeNanos =
        0L

    val coverage: ReplayReverseCoverage?
        get() =
            if (active) {
                ReplayReverseCoverage(
                    startTimeNanos =
                    retainedStartTimeNanos,
                    endTimeNanos =
                    currentTimeNanos,
                )
            } else {
                null
            }

    fun start(
        timestampNanos: Long,
    ) {
        entries.clear()

        active =
            true

        currentTimeNanos =
            timestampNanos

        retainedStartTimeNanos =
            timestampNanos
    }

    fun clear() {
        entries.clear()

        active =
            false

        currentTimeNanos =
            0L

        retainedStartTimeNanos =
            0L
    }

    fun advanceTo(
        timestampNanos: Long,
    ) {
        check(
            active,
        ) {
            "Mutation timeline is not active"
        }

        currentTimeNanos =
            timestampNanos

        trim()
    }

    fun append(
        timestampNanos: Long,
        value: T,
    ) {
        check(
            active,
        ) {
            "Mutation timeline is not active"
        }

        require(
            timestampNanos >=
                retainedStartTimeNanos,
        ) {
            "Cannot append mutation before retained history"
        }

        currentTimeNanos =
            maxOf(
                currentTimeNanos,
                timestampNanos,
            )

        entries.addLast(
            TimestampedValue(
                timestampNanos,
                value,
            ),
        )

        trim()
    }

    fun removeAfter(
        targetTimeNanos: Long,
    ): List<T> {
        check(
            coverage?.contains(
                targetTimeNanos,
            ) == true,
        ) {
            "Target is outside retained mutation history: $targetTimeNanos"
        }

        val removed =
            mutableListOf<T>()

        while (
            entries.isNotEmpty() &&
            entries.peekLast()
                .timestampNanos >
            targetTimeNanos
        ) {
            removed +=
                entries.removeLast()
                    .value
        }

        currentTimeNanos =
            targetTimeNanos

        return removed
    }

    private fun trim() {
        val minimumTimeNanos =
            (
                currentTimeNanos -
                    historyDurationNanos
                ).coerceAtLeast(
                0L,
            )

        retainedStartTimeNanos =
            maxOf(
                retainedStartTimeNanos,
                minimumTimeNanos,
            )

        while (
            entries.isNotEmpty() &&
            entries.peekFirst()
                .timestampNanos <
            retainedStartTimeNanos
        ) {
            entries.removeFirst()
        }
    }
}

data class TimestampedValue<T>(
    val timestampNanos: Long,
    val value: T,
)
