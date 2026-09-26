package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.UpOrDown
import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class ReplayTimelineWidget :
    Widget<ReplayTimelineWidget>(),
    Interactable {

    private val background =
        Rectangle()
            .color(
                ReplayUiStyle.TIMELINE_COLOR,
            )

    private val track =
        Rectangle()
            .color(
                ReplayUiStyle.TRACK_COLOR,
            )

    private val progress =
        Rectangle()
            .color(
                ReplayUiStyle.TRACK_PROGRESS_COLOR,
            )

    private val ruler =
        Rectangle()
            .color(
                ReplayUiStyle.RULER_COLOR,
            )

    private val rulerMajor =
        Rectangle()
            .color(
                ReplayUiStyle.RULER_MAJOR_COLOR,
            )

    private val playhead =
        Rectangle()
            .color(
                ReplayUiStyle.PLAYHEAD_COLOR,
            )

    private var visibleStartNanos =
        0.0

    private var nanosPerPixel =
        0.0

    private var lastDurationNanos =
        -1L

    private var panning =
        false

    private var lastPanMouseX =
        0

    override fun draw(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
    ) {
        val width =
            area.width

        val height =
            area.height

        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        ensureViewport(
            width,
        )

        if (
            !ReplayPlayer.paused &&
            !panning
        ) {
            followPlayhead(
                width,
            )
        }

        background.draw(
            context,
            0,
            0,
            width,
            height,
            widgetTheme.theme,
        )

        drawRuler(
            context,
            widgetTheme,
            width,
        )

        val trackTop =
            RULER_HEIGHT

        val trackHeight =
            (
                height -
                    trackTop -
                    3
                ).coerceAtLeast(
                6,
            )

        track.draw(
            context,
            0,
            trackTop,
            width,
            trackHeight,
            widgetTheme.theme,
        )

        val currentTime =
            ReplayPlayer.currentTimeNanos
                .toDouble()

        val progressWidth =
            when {
                currentTime <=
                    visibleStartNanos ->
                    0

                currentTime >=
                    visibleEndNanos(
                        width,
                    ) ->
                    width

                else ->
                    timeToX(
                        currentTime,
                    ).coerceIn(
                        0,
                        width,
                    )
            }

        if (progressWidth > 0) {
            progress.draw(
                context,
                0,
                trackTop,
                progressWidth,
                trackHeight,
                widgetTheme.theme,
            )
        }

        val playheadX =
            timeToX(
                currentTime,
            )

        if (
            playheadX in
            0 until width
        ) {
            playhead.draw(
                context,
                playheadX,
                RULER_HEIGHT - 2,
                1,
                height -
                    RULER_HEIGHT +
                    2,
                widgetTheme.theme,
            )

            drawPlayheadCap(
                context,
                widgetTheme,
                playheadX,
            )
        }
    }

    override fun onMousePressed(
        mouseButton: Int,
    ): Interactable.Result = when (mouseButton) {
        0 -> {
            seekToMouse()
            Interactable.Result.SUCCESS
        }

        2 -> {
            panning =
                true

            lastPanMouseX =
                context.absMouseX

            Interactable.Result.SUCCESS
        }

        else ->
            Interactable.Result.IGNORE
    }

    override fun onMouseDrag(
        mouseButton: Int,
        timeSinceClick: Long,
    ) {
        when (mouseButton) {
            0 ->
                seekToMouse()

            2 -> {
                val mouseX =
                    context.absMouseX

                val deltaX =
                    mouseX -
                        lastPanMouseX

                lastPanMouseX =
                    mouseX

                visibleStartNanos -=
                    deltaX *
                    nanosPerPixel

                clampViewport(
                    area.width,
                )
            }
        }
    }

    override fun onMouseRelease(
        mouseButton: Int,
    ): Boolean {
        if (mouseButton == 2) {
            panning =
                false
        }

        return mouseButton ==
            0 ||
            mouseButton ==
            2
    }

    override fun onMouseScroll(
        scrollDirection: UpOrDown,
        amount: Int,
    ): Boolean {
        val width =
            area.width

        val duration =
            ReplayPlayer.totalDurationNanos

        if (
            width <= 0 ||
            duration <= 0L
        ) {
            return false
        }

        ensureViewport(
            width,
        )

        val relativeX =
            (
                context.absMouseX -
                    area.x
                ).coerceIn(
                0,
                width,
            )

        val anchorTime =
            visibleStartNanos +
                relativeX *
                nanosPerPixel

        val zoomPower =
            scrollDirection.modifier
                .toDouble()

        val fitNanosPerPixel =
            duration.toDouble() /
                width

        val minNanosPerPixel =
            min(
                fitNanosPerPixel,
                MIN_NANOS_PER_PIXEL,
            )

        val newNanosPerPixel =
            (
                nanosPerPixel /
                    ZOOM_FACTOR.pow(
                        zoomPower,
                    )
                ).coerceIn(
                minNanosPerPixel,
                fitNanosPerPixel,
            )

        visibleStartNanos =
            anchorTime -
            relativeX *
            newNanosPerPixel

        nanosPerPixel =
            newNanosPerPixel

        clampViewport(
            width,
        )

        return true
    }

    private fun drawRuler(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        width: Int,
    ) {
        val duration =
            ReplayPlayer.totalDurationNanos

        if (duration <= 0L) {
            return
        }

        val targetStepNanos =
            nanosPerPixel *
                LABEL_TARGET_PIXELS

        val majorStep =
            chooseMajorStep(
                targetStepNanos,
            )

        val minorStep =
            majorStep /
                MINOR_DIVISIONS

        val firstMinor =
            ceil(
                visibleStartNanos /
                    minorStep,
            ).toLong() *
                minorStep

        val visibleEnd =
            visibleEndNanos(
                width,
            )

        var time =
            firstMinor

        while (
            time.toDouble() <=
            visibleEnd &&
            time <=
            duration
        ) {
            val x =
                timeToX(
                    time.toDouble(),
                )

            val major =
                time %
                    majorStep ==
                    0L

            val markHeight =
                if (major) {
                    MAJOR_MARK_HEIGHT
                } else {
                    MINOR_MARK_HEIGHT
                }

            (
                if (major) {
                    rulerMajor
                } else {
                    ruler
                }
                ).draw(
                context,
                x,
                RULER_HEIGHT -
                    markHeight,
                1,
                markHeight,
                widgetTheme.theme,
            )

            if (major) {
                drawTimeLabel(
                    time,
                    x,
                )
            }

            time +=
                minorStep
        }
    }

    private fun drawTimeLabel(
        timeNanos: Long,
        x: Int,
    ) {
        val font =
            Minecraft.getMinecraft()
                .fontRenderer

        val label =
            ReplayTimeFormatter.formatRuler(
                timeNanos,
            )

        val labelX =
            (
                x +
                    2
                ).coerceAtMost(
                max(
                    area.width -
                        font.getStringWidth(
                            label,
                        ),
                    0,
                ),
            )

        font.drawString(
            label,
            labelX,
            1,
            ReplayUiStyle.MUTED_TEXT_COLOR,
        )
    }

    private fun drawPlayheadCap(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        playheadX: Int,
    ) {
        val capLeft =
            (
                playheadX -
                    2
                ).coerceAtLeast(
                0,
            )

        val capWidth =
            min(
                5,
                area.width -
                    capLeft,
            )

        playhead.draw(
            context,
            capLeft,
            RULER_HEIGHT - 4,
            capWidth,
            3,
            widgetTheme.theme,
        )
    }

    private fun seekToMouse() {
        val width =
            area.width

        if (width <= 0) {
            return
        }

        ensureViewport(
            width,
        )

        val relativeX =
            (
                context.absMouseX -
                    area.x
                ).coerceIn(
                0,
                width,
            )

        val targetTimeNanos =
            (
                visibleStartNanos +
                    relativeX *
                    nanosPerPixel
                ).toLong()
                .coerceIn(
                    0L,
                    ReplayPlayer
                        .totalDurationNanos,
                )

        ReplayPlayer.seek(
            targetTimeNanos,
        )
    }

    private fun ensureViewport(
        width: Int,
    ) {
        val duration =
            ReplayPlayer.totalDurationNanos

        if (
            duration <= 0L ||
            width <= 0
        ) {
            visibleStartNanos =
                0.0

            nanosPerPixel =
                1.0

            lastDurationNanos =
                duration

            return
        }

        if (
            nanosPerPixel <= 0.0 ||
            lastDurationNanos !=
            duration
        ) {
            nanosPerPixel =
                duration.toDouble() /
                width

            visibleStartNanos =
                0.0

            lastDurationNanos =
                duration
        }

        clampViewport(
            width,
        )
    }

    private fun followPlayhead(
        width: Int,
    ) {
        val current =
            ReplayPlayer.currentTimeNanos
                .toDouble()

        val visibleDuration =
            width *
                nanosPerPixel

        val margin =
            visibleDuration *
                FOLLOW_MARGIN

        val visibleEnd =
            visibleStartNanos +
                visibleDuration

        if (
            current >
            visibleEnd -
            margin
        ) {
            visibleStartNanos =
                current -
                visibleDuration *
                FOLLOW_POSITION
        } else if (
            current <
            visibleStartNanos +
            margin
        ) {
            visibleStartNanos =
                current -
                visibleDuration *
                (
                    1.0 -
                    FOLLOW_POSITION
                )
        }

        clampViewport(
            width,
        )
    }

    private fun clampViewport(
        width: Int,
    ) {
        val duration =
            ReplayPlayer.totalDurationNanos
                .toDouble()

        val visibleDuration =
            width *
                nanosPerPixel

        val maxStart =
            max(
                duration -
                    visibleDuration,
                0.0,
            )

        visibleStartNanos =
            visibleStartNanos
                .coerceIn(
                    0.0,
                    maxStart,
                )
    }

    private fun visibleEndNanos(
        width: Int,
    ): Double =
        visibleStartNanos +
            width *
            nanosPerPixel

    private fun timeToX(
        timeNanos: Double,
    ): Int =
        (
            (
                timeNanos -
                    visibleStartNanos
                ) /
                nanosPerPixel
            ).roundToInt()

    private fun chooseMajorStep(
        targetNanos: Double,
    ): Long {
        NICE_STEPS_NANOS.forEach {
            if (
                it >=
                targetNanos
            ) {
                return it
            }
        }

        return NICE_STEPS_NANOS
            .last()
    }

    companion object {

        private const val RULER_HEIGHT =
            17

        private const val MINOR_MARK_HEIGHT =
            4

        private const val MAJOR_MARK_HEIGHT =
            8

        private const val MINOR_DIVISIONS =
            5L

        private const val LABEL_TARGET_PIXELS =
            72.0

        private const val MIN_NANOS_PER_PIXEL =
            5_000_000.0

        private const val ZOOM_FACTOR =
            1.25

        private const val FOLLOW_MARGIN =
            0.08

        private const val FOLLOW_POSITION =
            0.82

        private val NICE_STEPS_NANOS =
            longArrayOf(
                50_000_000L,
                100_000_000L,
                250_000_000L,
                500_000_000L,
                1_000_000_000L,
                2_000_000_000L,
                5_000_000_000L,
                10_000_000_000L,
                30_000_000_000L,
                60_000_000_000L,
                120_000_000_000L,
                300_000_000_000L,
                600_000_000_000L,
                1_800_000_000_000L,
                3_600_000_000_000L,
            )
    }
}
