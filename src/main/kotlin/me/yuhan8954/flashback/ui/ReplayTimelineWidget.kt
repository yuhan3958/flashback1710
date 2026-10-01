package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.UpOrDown
import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.ReplayLang
import me.yuhan8954.flashback.editor.ReplayEditorTracks
import me.yuhan8954.flashback.editor.ReplayTimelineEventType
import me.yuhan8954.flashback.replay.ReplayPlayer
import net.minecraft.client.Minecraft
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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

    private val packetEvent =
        Rectangle()
            .color(
                ReplayUiStyle.PACKET_EVENT_COLOR,
            )

    private val checkpointEvent =
        Rectangle()
            .color(
                ReplayUiStyle.CHECKPOINT_EVENT_COLOR,
            )

    private val markerEvent =
        Rectangle()
            .color(
                ReplayUiStyle.MARKER_EVENT_COLOR,
            )

    private val cameraKeyframeEvent =
        Rectangle()
            .color(
                ReplayUiStyle.CAMERA_KEYFRAME_COLOR,
            )

    private val fovKeyframeEvent = Rectangle().color(ReplayUiStyle.FOV_KEYFRAME_COLOR)
    private val speedKeyframeEvent = Rectangle().color(ReplayUiStyle.SPEED_KEYFRAME_COLOR)
    private val orbitKeyframeEvent = Rectangle().color(ReplayUiStyle.ORBIT_KEYFRAME_COLOR)
    private val timeKeyframeEvent = Rectangle().color(ReplayUiStyle.TIME_KEYFRAME_COLOR)
    private val rangeShade = Rectangle().color(0x88000000.toInt())

    private val selectedKeyframeEvent =
        Rectangle().color(ReplayUiStyle.PLAYHEAD_COLOR)

    private val rangeBoundary =
        Rectangle()
            .color(
                ReplayUiStyle.RANGE_BOUNDARY_COLOR,
            )

    private val viewport = ReplayTimelineViewport()

    private var visibleStartNanos: Double
        get() = viewport.visibleStartNanos
        set(value) {
            viewport.visibleStartNanos = value
        }

    private var nanosPerPixel: Double
        get() = viewport.nanosPerPixel
        set(value) {
            viewport.nanosPerPixel = value
        }

    private var lastDurationNanos =
        -1L

    private var lastWidth = -1

    private val interaction = ReplayTimelineInteraction()

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
            !interaction.panning
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

        for (row in ReplayEditorTracks.rows.indices) {
            if (row % 2 == 1) {
                track.draw(context, 0, RULER_HEIGHT + row * ROW_HEIGHT, width, ROW_HEIGHT, widgetTheme.theme)
            }
        }

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

        drawEditorOverlays(
            context,
            widgetTheme,
            width,
            height,
        )

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
            val keyframeHit = selectKeyframeAtMouse()
            interaction.pressKeyframe(keyframeHit)
            if (!keyframeHit) {
                ReplayEditorController.clearKeyframeSelection()
                seekToMouse()
            }
            Interactable.Result.SUCCESS
        }

        2 -> {
            interaction.beginPan(context.absMouseX)

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
            0 -> if (interaction.draggingKeyframe()) {
                ReplayEditorController.moveSelectedKeyframe(mouseTime())
            } else {
                seekToMouse()
            }

            2 ->
                panToMouse()
        }
    }

    override fun onMouseRelease(
        mouseButton: Int,
    ): Boolean = interaction.release(mouseButton)

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

        if (Interactable.hasShiftDown()) {
            panByWheel(
                scrollDirection,
                width,
            )

            return true
        }

        zoomAtMouse(
            scrollDirection,
            width,
        )

        return true
    }

    private fun drawEditorOverlays(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        width: Int,
        height: Int,
    ) {
        val inTime = ReplayPlayer.inPointNanos ?: 0L
        val outTime = ReplayPlayer.outPointNanos ?: ReplayPlayer.totalDurationNanos
        val inX = timeToX(inTime.toDouble()).coerceIn(0, width)
        val outX = timeToX(outTime.toDouble()).coerceIn(0, width)
        if (inX > 0) rangeShade.draw(context, 0, RULER_HEIGHT, inX, height - RULER_HEIGHT, widgetTheme.theme)
        if (outX < width) rangeShade.draw(context, outX, RULER_HEIGHT, width - outX, height - RULER_HEIGHT, widgetTheme.theme)
        drawRangeBoundary(
            context,
            widgetTheme,
            ReplayPlayer.inPointNanos,
            width,
            height,
        )

        drawRangeBoundary(
            context,
            widgetTheme,
            ReplayPlayer.outPointNanos,
            width,
            height,
        )

        var lastPacketX =
            Int.MIN_VALUE

        ReplayPlayer.timelineEvents
            .forEach { event ->
                val x =
                    timeToX(
                        event.timestampNanos
                            .toDouble(),
                    )

                if (
                    x !in
                    0 until width
                ) {
                    return@forEach
                }

                when (event.type) {
                    ReplayTimelineEventType.PACKET -> {
                        if (x == lastPacketX) {
                            return@forEach
                        }

                        lastPacketX =
                            x

                        packetEvent.draw(
                            context,
                            x,
                            RULER_HEIGHT,
                            1,
                            3,
                            widgetTheme.theme,
                        )
                    }

                    ReplayTimelineEventType.CHECKPOINT ->
                        checkpointEvent.draw(
                            context,
                            x,
                            RULER_HEIGHT,
                            2,
                            7,
                            widgetTheme.theme,
                        )

                    ReplayTimelineEventType.EVENT ->
                        markerEvent.draw(
                            context,
                            x,
                            RULER_HEIGHT + 5 * ROW_HEIGHT,
                            2,
                            ROW_HEIGHT,
                            widgetTheme.theme,
                        )

                    ReplayTimelineEventType.CAMERA_KEYFRAME ->
                        drawKeyframe(context, widgetTheme, "camera", event.timestampNanos, x, 0, cameraKeyframeEvent)

                    ReplayTimelineEventType.FOV_KEYFRAME ->
                        drawKeyframe(context, widgetTheme, "fov", event.timestampNanos, x, 2, fovKeyframeEvent)
                }
            }
        ReplayPlayer.keyframeTimes("speed").forEach { time ->
            val x = timeToX(time.toDouble())
            if (x in 0 until width) {
                drawKeyframe(context, widgetTheme, "speed", time, x, 3, speedKeyframeEvent)
            }
        }
        ReplayPlayer.keyframeTimes("camera_orbit").forEach { time ->
            val x = timeToX(time.toDouble())
            if (x in 0 until width) drawKeyframe(context, widgetTheme, "camera_orbit", time, x, 1, orbitKeyframeEvent)
        }
        ReplayPlayer.keyframeTimes("time_of_day").forEach { time ->
            val x = timeToX(time.toDouble())
            if (x in 0 until width) drawKeyframe(context, widgetTheme, "time_of_day", time, x, 4, timeKeyframeEvent)
        }
        ReplayPlayer.markers().forEach { marker ->
            val x = timeToX(marker.timestampNanos.toDouble())
            if (x in 0 until width - 20) {
                Minecraft.getMinecraft().fontRenderer.drawString(
                    marker.label.ifBlank { ReplayLang.text("marker.unnamed") },
                    x + 3,
                    RULER_HEIGHT + 5 * ROW_HEIGHT + 2,
                    ReplayUiStyle.MARKER_EVENT_COLOR,
                )
            }
        }
    }

    private fun drawKeyframe(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        trackId: String,
        time: Long,
        x: Int,
        row: Int,
        normal: Rectangle,
    ) {
        val selected = ReplayPlayer.selectedKeyframe?.let { it.trackId == trackId && it.timestampNanos == time } == true
        val drawable = if (selected) selectedKeyframeEvent else normal
        drawable.draw(context, x, RULER_HEIGHT + row * ROW_HEIGHT + 4, 6, 6, widgetTheme.theme)
    }

    private fun selectKeyframeAtMouse(): Boolean {
        val y = context.absMouseY - area.y
        val trackId = ReplayTimelineHitTest.keyframeTrackAt(y, RULER_HEIGHT, ROW_HEIGHT) ?: return false
        val mouseX = context.absMouseX - area.x
        val nearest = ReplayTimelineHitTest.nearestKeyframe(
            trackId,
            ReplayPlayer.keyframeTimes(trackId),
            mouseX,
            KEYFRAME_HIT_RADIUS,
        ) { timeToX(it.toDouble()) } ?: return false
        return ReplayEditorController.selectKeyframe(nearest.trackId, nearest.timestampNanos)
    }

    private fun drawRangeBoundary(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        timestampNanos: Long?,
        width: Int,
        height: Int,
    ) {
        val time =
            timestampNanos ?: return

        val x =
            timeToX(
                time.toDouble(),
            )

        if (
            x !in
            0 until width
        ) {
            return
        }

        rangeBoundary.draw(
            context,
            x,
            RULER_HEIGHT - 2,
            1,
            height -
                RULER_HEIGHT +
                2,
            widgetTheme.theme,
        )
    }

    private fun panToMouse() {
        viewport.pan(interaction.panDelta(context.absMouseX))
    }

    private fun panByWheel(
        scrollDirection: UpOrDown,
        width: Int,
    ) {
        val visibleDuration =
            width *
                nanosPerPixel

        viewport.pan((scrollDirection.modifier * visibleDuration * WHEEL_PAN_FRACTION / nanosPerPixel).toInt())
    }

    private fun zoomAtMouse(
        scrollDirection: UpOrDown,
        width: Int,
    ) {
        val relativeX =
            (
                context.absMouseX -
                    area.x
                ).coerceIn(
                0,
                width,
            )

        viewport.zoom(relativeX, ZOOM_FACTOR.pow(scrollDirection.modifier.toDouble()))
    }

    private fun drawRuler(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
    ) {
        viewport.rulerTicks(LABEL_TARGET_PIXELS).forEach { (time, x, major) ->
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

        val targetTimeNanos = viewport.xToTime(relativeX)

        ReplayPlayer.seek(targetTimeNanos)
    }

    private fun mouseTime(): Long = viewport.xToTime((context.absMouseX - area.x).coerceIn(0, area.width))

    private fun ensureViewport(
        width: Int,
    ) {
        val duration =
            ReplayPlayer.totalDurationNanos

        if (
            duration <= 0L ||
            width <= 0
        ) {
            viewport.fit(duration, width)

            lastDurationNanos =
                duration

            lastWidth = width

            return
        }

        if (
            nanosPerPixel <= 0.0 ||
            lastDurationNanos != duration || lastWidth != width
        ) {
            viewport.fit(duration, width)

            lastDurationNanos =
                duration

            lastWidth = width
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
        viewport.clamp()
    }

    private fun visibleEndNanos(
        width: Int,
    ): Double = visibleStartNanos +
        width *
        nanosPerPixel

    private fun timeToX(timeNanos: Double): Int = viewport.timeToX(timeNanos.toLong())

    companion object {

        private const val RULER_HEIGHT = ReplayEditorMetrics.TIMELINE_RULER_HEIGHT

        private const val ROW_HEIGHT = ReplayEditorMetrics.TRACK_ROW_HEIGHT

        private const val KEYFRAME_HIT_RADIUS = 5

        private const val MINOR_MARK_HEIGHT =
            4

        private const val MAJOR_MARK_HEIGHT =
            8

        private const val LABEL_TARGET_PIXELS =
            72.0

        private const val ZOOM_FACTOR =
            1.25

        private const val WHEEL_PAN_FRACTION =
            0.12

        private const val FOLLOW_MARGIN =
            0.08

        private const val FOLLOW_POSITION =
            0.82
    }
}
