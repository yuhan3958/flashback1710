package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import me.yuhan8954.flashback.replay.ReplayClock
import me.yuhan8954.flashback.replay.ReplayPlayer
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
            RULER_HEIGHT +
                TRACK_MARGIN_TOP

        val trackHeight =
            (
                height -
                    trackTop -
                    5
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

        val progressWidth =
            (
                width *
                    progressFraction()
                ).roundToInt()
                .coerceIn(
                    0,
                    width,
                )

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
            progressWidth
                .coerceIn(
                    0,
                    width - 1,
                )

        playhead.draw(
            context,
            playheadX,
            RULER_HEIGHT - 1,
            1,
            height - RULER_HEIGHT + 1,
            widgetTheme.theme,
        )

        drawPlayheadCap(
            context,
            widgetTheme,
            playheadX,
        )
    }

    override fun onMousePressed(
        mouseButton: Int,
    ): Interactable.Result {
        if (mouseButton != 0) {
            return Interactable.Result.IGNORE
        }

        seekToMouse()

        return Interactable.Result.SUCCESS
    }

    override fun onMouseDrag(
        mouseButton: Int,
        timeSinceClick: Long,
    ) {
        if (mouseButton == 0) {
            seekToMouse()
        }
    }

    override fun onMouseRelease(
        mouseButton: Int,
    ): Boolean =
        mouseButton ==
            0

    private fun drawRuler(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        width: Int,
    ) {
        val duration =
            ReplayPlayer.totalDurationNanos

        if (
            duration <= 0L ||
            width <= 0
        ) {
            return
        }

        val totalTicks =
            (
                duration /
                    ReplayClock.MINECRAFT_TICK_NANOS
                ).coerceAtLeast(
                1L,
            )

        val tickSpacing =
            width.toDouble() /
                totalTicks

        val minorStep =
            chooseMinorStep(
                tickSpacing,
            )

        val majorStep =
            minorStep *
                MAJOR_TICKS_PER_GROUP

        var tick =
            0L

        while (tick <= totalTicks) {
            val x =
                (
                    tick.toDouble() /
                        totalTicks *
                        width
                    ).roundToInt()
                    .coerceIn(
                        0,
                        width - 1,
                    )

            val major =
                tick %
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

            tick +=
                minorStep
        }
    }

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
            if (
                capLeft +
                    7 >
                area.width
            ) {
                area.width -
                    capLeft
            } else {
                7
            }

        playhead.draw(
            context,
            capLeft,
            RULER_HEIGHT - 4,
            capWidth,
            3,
            widgetTheme.theme,
        )
    }

    private fun chooseMinorStep(
        tickSpacing: Double,
    ): Long {
        var step =
            1L

        while (
            tickSpacing *
                step <
            MIN_MINOR_PIXEL_SPACING
        ) {
            step *=
                if (
                    step %
                        5L ==
                    0L
                ) {
                    2L
                } else {
                    5L
                }
        }

        return step
    }

    private fun seekToMouse() {
        if (
            area.width <=
            0
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

        val fraction =
            relativeX.toDouble() /
                area.width

        val targetTimeNanos =
            (
                ReplayPlayer
                    .totalDurationNanos *
                    fraction
                ).toLong()

        ReplayPlayer.seek(
            targetTimeNanos,
        )
    }

    private fun progressFraction(): Double {
        val duration =
            ReplayPlayer.totalDurationNanos

        if (duration <= 0L) {
            return 0.0
        }

        return (
            ReplayPlayer.currentTimeNanos
                .toDouble() /
                duration
            ).coerceIn(
            0.0,
            1.0,
        )
    }

    companion object {

        private const val RULER_HEIGHT =
            16

        private const val TRACK_MARGIN_TOP =
            3

        private const val MINOR_MARK_HEIGHT =
            5

        private const val MAJOR_MARK_HEIGHT =
            10

        private const val MAJOR_TICKS_PER_GROUP =
            5L

        private const val MIN_MINOR_PIXEL_SPACING =
            8.0
    }
}
