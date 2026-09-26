package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.drawable.Rectangle

object ReplayUiStyle {

    const val PANEL_COLOR =
        0xE6151515.toInt()

    const val PANEL_BORDER_COLOR =
        0xFF454545.toInt()

    const val BUTTON_COLOR =
        0xFF282828.toInt()

    const val BUTTON_ACTIVE_COLOR =
        0xFF3A3A3A.toInt()

    const val TIMELINE_COLOR =
        0xFF1C1C1C.toInt()

    const val RULER_COLOR =
        0xFF686868.toInt()

    const val RULER_MAJOR_COLOR =
        0xFFA8A8A8.toInt()

    const val TRACK_COLOR =
        0xFF333333.toInt()

    const val TRACK_PROGRESS_COLOR =
        0xFF555555.toInt()

    const val PLAYHEAD_COLOR =
        0xFFFFFFFF.toInt()

    const val PACKET_EVENT_COLOR =
        0xFF4F6C8A.toInt()

    const val CHECKPOINT_EVENT_COLOR =
        0xFF8A6C4F.toInt()

    const val MARKER_EVENT_COLOR =
        0xFFE8D75A.toInt()

    const val CAMERA_KEYFRAME_COLOR =
        0xFF6FD6FF.toInt()

    const val RANGE_BOUNDARY_COLOR =
        0xFFFF8A65.toInt()

    const val SPEED_FORWARD_COLOR =
        0xFF80FF80.toInt()

    const val SPEED_REVERSE_COLOR =
        0xFFFFB060.toInt()

    const val TEXT_COLOR =
        0xFFF2F2F2.toInt()

    const val MUTED_TEXT_COLOR =
        0xFFA0A0A0.toInt()

    fun panelBackground(): Rectangle = Rectangle()
        .color(PANEL_COLOR)

    fun panelBorder(): Rectangle = Rectangle()
        .color(PANEL_BORDER_COLOR)
        .hollow(1.0f)

    fun buttonBackground(): Rectangle = Rectangle()
        .color(BUTTON_COLOR)

    fun timelineBackground(): Rectangle = Rectangle()
        .color(TIMELINE_COLOR)
}
