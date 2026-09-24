package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.drawable.Rectangle

object ReplayUiStyle {

    const val PANEL_COLOR =
        0xD0181B20.toInt()

    const val PANEL_BORDER_COLOR =
        0xCC58616E.toInt()

    const val TRACK_COLOR =
        0xFF303640.toInt()

    const val TRACK_PROGRESS_COLOR =
        0xFF4AA3DF.toInt()

    const val PLAYHEAD_COLOR =
        0xFFFFFFFF.toInt()

    const val TEXT_COLOR =
        0xFFE6E8EB.toInt()

    const val MUTED_TEXT_COLOR =
        0xFF9AA2AD.toInt()

    fun panelBackground(): Rectangle = Rectangle()
        .color(PANEL_COLOR)
        .cornerRadius(3)

    fun panelBorder(): Rectangle = Rectangle()
        .color(PANEL_BORDER_COLOR)
        .hollow(1.0f)
}
