package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.UpOrDown
import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import me.yuhan8954.flashback.replay.ReplayClock
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft
import kotlin.math.ceil
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

    private var viewStartNanos =
        0L

    private var visibleDurationNanos =
        0L

    private var middleDragging =
        false

    private var lastDragMouseX =
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

        val duration =
            ReplayPlayer.totalDurationNanos

        updateViewport(
            duration,
        )

        background.draw(
            context,
            0,
            0,
            width,
            height,
            widgetTheme.theme,
        )

        if (duration <= 0L) {
            return
        }

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
                    2
                ).coerceAtLeast(
                5,
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

        val playheadX =
            timeToX(
                currentTime,
                width,
            )

        val progressStart =
            timeToX(
                viewStartNanos,
                width,
            )

        if (
            playheadX >
            progressStart
        ) {
            progress.draw(
                context,
                progressStart,
                trackTop,
                (
                    playheadX -
                        progressStart
                    ).coerceAtMost(
                    width -
                        progressStart,
                ),
                trackHeight,
                widgetTheme.theme,
            )
        }

        if (
            playheadX in
            0 until width
        ) {
            playhead.draw(
                context,
                playheadX,
                RULER_HEIGHT - 2,
                1,
                height - RULER_HEIGHT + 2,
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
    ): Interactable.Result =
        when (mouseButton) {
            0 -> {
                seekToMouse()
                Interactable.Result.SUCCESS
            }

            MIDDLE_MOUSE_BUTTON -> {
                middleDragging =
                    true

                lastDragMouseX =
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

            MIDDLE_MOUSE_BUTTON -> {
                if (!middleDragging) {
                    return
                }

                val currentMouseX =
                    context.absMouseX

                val deltaPixels =
                    currentMouseX -
                        lastDragMouseX

                lastDragMouseX =
                    currentMouseX

                panPixels(
                    deltaPixels,
                )
            }
        }
    }

    override fun onMouseRelease(
        mouseButton: Int,
    ): Boolean {
        if (
            mouseButton ==
            MIDDLE_MOUSE_BUTTON
        ) {
            middleDragging =
                false

            return true
        }

        return mouseButton ==
            0
    }

    override fun onMouseScroll(
        scrollDirection: UpOrDown,
        amount: Int,
    ): Boolean {
        if (
            area.width <= 0 ||
            ReplayPlayer.totalDurationNanos <=
            0L
        ) {
            return false
        }

        val cursorFraction =
            (
                context.absMouseX -
                    area.x
                ).toDouble()
                .div(
                    area.width,
                ).coerceIn(
                    0.0,
                    1.0,
                )

        val anchorTime =
            viewStartNanos +
                (
                    visibleDurationNanos *
                        cursorFraction
                    ).toLong()

        val zoomFactor =
            if (
                scrollDirection.modifier >
                0
            ) {
                ZOOM_IN_FACTOR
            } else {
                ZOOM_OUT_FACTOR
            }

        val steps =
            amount
                .coerceAtLeast(
                    1,
                )

        var newVisible =
            visibleDurationNanos
                .toDouble()

        repeat(
            steps,
        ) {
            newVisible *=
                zoomFactor
        }

        visibleDurationNanos =
            newVisible
                .toLong()
                .coerceIn(
                    MIN_VISIBLE_DURATION_NANOS,
                    ReplayPlayer.totalDurationNanos
                        .coerceAtLeast(
                            MIN_VISIBLE_DURATION_NANOS,
                        ),
                ).coerceAtMost(
                    ReplayPlayer.totalDurationNanos,
                )

        viewStartNanos =
            anchorTime -
                (
                    visibleDurationNanos *
                        cursorFraction
                    ).toLong()

        clampViewport()

        return true
    }

    private fun updateViewport(
        duration: Long,
    ) {
        if (duration <= 0L) {
            viewStartNanos =
                0L

            visibleDurationNanos =
                0L

            return
        }

        if (
            visibleDurationNanos <= 0L ||
            visibleDurationNanos >
            duration
        ) {
            visibleDurationNanos =
                duration
                    .coerceAtMost(
                        DEFAULT_VISIBLE_DURATION_NANOS,
                    ).coerceAtLeast(
                        MIN_VISIBLE_DURATION_NANOS
                            .coerceAtMost(
                                duration,
                            ),
                    )

            viewStartNanos =
                (
                    ReplayPlayer.currentTimeNanos -
                        visibleDurationNanos /
                        5
                    ).coerceAtLeast(
                    0L,
                )

            clampViewport()
        }

        if (!ReplayPlayer.paused) {
            followPlayhead()
        }
    }

    private fun followPlayhead() {
        val currentTime =
            ReplayPlayer.currentTimeNanos

        val leftEdge =
            viewStartNanos +
                (
                    visibleDurationNanos *
                        FOLLOW_EDGE_FRACTION
                    ).toLong()

        val rightEdge =
            viewStartNanos +
                visibleDurationNanos -
                (
                    visibleDurationNanos *
                        FOLLOW_EDGE_FRACTION
                    ).toLong()

        when {
            currentTime <
                leftEdge ->
                viewStartNanos =
                    currentTime -
                        (
                            visibleDurationNanos *
                                FOLLOW_TARGET_FRACTION
                            ).toLong()

            currentTime >
                rightEdge ->
                viewStartNanos =
                    currentTime -
                        (
                            visibleDurationNanos *
                                (
                                    1.0 -
                                        FOLLOW_TARGET_FRACTION
                                    )
                            ).toLong()
        }

        clampViewport()
    }

    private fun panPixels(
        deltaPixels: Int,
    ) {
        if (
            area.width <= 0 ||
            visibleDurationNanos <= 0L
        ) {
            return
        }

        val deltaTime =
            (
                visibleDurationNanos
                    .toDouble() *
                    deltaPixels /
                    area.width
                ).toLong()

        viewStartNanos -=
            deltaTime

        clampViewport()
    }

    private fun clampViewport() {
        val duration =
            ReplayPlayer.totalDurationNanos

        if (
            duration <= 0L ||
            visibleDurationNanos <= 0L
        ) {
            viewStartNanos =
                0L

            return
        }

        val maximumStart =
            (
                duration -
                    visibleDurationNanos
                ).coerceAtLeast(
                0L,
            )

        viewStartNanos =
            viewStartNanos.coerceIn(
                0L,
                maximumStart,
            )
    }

    private fun drawRuler(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        width: Int,
    ) {
        val majorInterval =
            chooseMajorInterval(
                width,
            )

        val minorInterval =
            (
                majorInterval /
                    MINOR_DIVISIONS
                ).coerceAtLeast(
                ReplayClock.MINECRAFT_TICK_NANOS,
            )

        val viewEnd =
            (
                viewStartNanos +
                    visibleDurationNanos
                ).coerceAtMost(
                ReplayPlayer.totalDurationNanos,
            )

        var tick =
            firstTickAtOrAfter(
                viewStartNanos,
                minorInterval,
            )

        while (tick <= viewEnd) {
            val x =
                timeToX(
                    tick,
                    width,
                )

            val major =
                tick %
                    majorInterval ==
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
                    tick,
                    x,
                    width,
                )
            }

            tick +=
                minorInterval
        }
    }

    private fun drawTimeLabel(
        timeNanos: Long,
        x: Int,
        width: Int,
    ) {
        val fontRenderer =
            Minecraft.getMinecraft()
                .fontRenderer

        val label =
            ReplayTimeFormatter.format(
                timeNanos,
            )

        val labelWidth =
            fontRenderer.getStringWidth(
                label,
            )

        val labelX =
            (
                x +
                    2
                ).coerceAtMost(
                (
                    width -
                        labelWidth -
                        1
                    ).coerceAtLeast(
                    0,
                ),
            )

        fontRenderer.drawString(
            label,
            labelX,
            1,
            LABEL_COLOR,
        )
    }

    private fun chooseMajorInterval(
        width: Int,
    ): Long =
        MAJOR_INTERVALS_NANOS
            .firstOrNull {
                (
                    it.toDouble() /
                        visibleDurationNanos *
                        width
                    ) >=
                    MIN_MAJOR_PIXEL_SPACING
            } ?: MAJOR_INTERVALS_NANOS.last()

    private fun firstTickAtOrAfter(
        timeNanos: Long,
        intervalNanos: Long,
    ): Long =
        (
            ceil(
                timeNanos.toDouble() /
                    intervalNanos,
            ) *
                intervalNanos
            ).toLong()

    private fun drawPlayheadCap(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        playheadX: Int,
    ) {
        val capLeft =
            (
                playheadX -
                    3
                ).coerceAtLeast(
                0,
            )

        val capWidth =
            (
                area.width -
                    capLeft
                ).coerceAtMost(
                7,
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
        if (
            area.width <= 0 ||
            visibleDurationNanos <= 0L
        ) {
            return
        }

        val relativeX =
            (
                context.absMouseX -
                    area.x
                ).coerceIn(
                0,
                area.width,
            )

        ReplayPlayer.seek(
            xToTime(
                relativeX,
                area.width,
            ),
        )
    }

    private fun timeToX(
        timeNanos: Long,
        width: Int,
    ): Int =
        (
            (
                timeNanos -
                    viewStartNanos
                ).toDouble() /
                visibleDurationNanos *
                width
            ).roundToInt()

    private fun xToTime(
        x: Int,
        width: Int,
    ): Long =
        (
            viewStartNanos +
                visibleDurationNanos *
                (
                    x.toDouble() /
                        width
                    )
            ).toLong()
            .coerceIn(
                0L,
                ReplayPlayer.totalDurationNanos,
            )

    companion object {

        private const val RULER_HEIGHT =
            18

        private const val MINOR_MARK_HEIGHT =
            4

        private const val MAJOR_MARK_HEIGHT =
            9

        private const val MINOR_DIVISIONS =
            5L

        private const val MIN_MAJOR_PIXEL_SPACING =
            72.0

        private const val DEFAULT_VISIBLE_DURATION_NANOS =
            30_000_000_000L

        private const val MIN_VISIBLE_DURATION_NANOS =
            1_000_000_000L

        private const val FOLLOW_EDGE_FRACTION =
            0.08

        private const val FOLLOW_TARGET_FRACTION =
            0.18

        private const val ZOOM_IN_FACTOR =
            0.8

        private const val ZOOM_OUT_FACTOR =
            1.25

        private const val MIDDLE_MOUSE_BUTTON =
            2

        private const val LABEL_COLOR =
            0xA8A8A8

        private val MAJOR_INTERVALS_NANOS =
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
