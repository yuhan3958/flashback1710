package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import me.yuhan8954.flashback.replay.ReplayPlayer
import kotlin.math.roundToInt

class ReplayTimelineWidget :
    Widget<ReplayTimelineWidget>(),
    Interactable {

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

        val trackY =
            (height - TRACK_HEIGHT) /
                2

        val progressWidth =
            (width * progressFraction())
                .roundToInt()
                .coerceIn(
                    0,
                    width,
                )

        track.draw(
            context,
            0,
            trackY,
            width,
            TRACK_HEIGHT,
            widgetTheme.theme,
        )

        if (progressWidth > 0) {
            progress.draw(
                context,
                0,
                trackY,
                progressWidth,
                TRACK_HEIGHT,
                widgetTheme.theme,
            )
        }

        val playheadX =
            (
                progressWidth -
                    PLAYHEAD_WIDTH / 2
                )
                .coerceIn(
                    0,
                    width - PLAYHEAD_WIDTH,
                )

        playhead.draw(
            context,
            playheadX,
            0,
            PLAYHEAD_WIDTH,
            height,
            widgetTheme.theme,
        )
    }

    override fun onMousePressed(mouseButton: Int): Interactable.Result {
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

    override fun onMouseRelease(mouseButton: Int): Boolean = mouseButton == 0

    private fun seekToMouse() {
        if (area.width <= 0) {
            return
        }

        val relativeX = (context.absMouseX - area.x).coerceIn(0, area.width)
        val fraction = relativeX.toDouble() / area.width
        val targetTimeNanos = (ReplayPlayer.totalDurationNanos * fraction).toLong()

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

        private const val TRACK_HEIGHT = 4
        private const val PLAYHEAD_WIDTH = 2
    }
}
