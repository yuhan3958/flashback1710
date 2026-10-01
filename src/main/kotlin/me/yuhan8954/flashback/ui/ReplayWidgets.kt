package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import com.cleanroommc.modularui.widgets.TextWidget
import me.yuhan8954.flashback.ReplayLang

internal class ReplayButtonWidget : ButtonWidget<ReplayButtonWidget>()

internal class ReplayContainerWidget : ParentWidget<ReplayContainerWidget>()

internal object ReplayEditorMetrics {
    const val TIMELINE_RULER_HEIGHT = 14
    const val TRACK_ROW_HEIGHT = 12
    const val BUTTON_HEIGHT = 18
}

internal fun editorButton(labelKey: String, left: Int, top: Int, width: Int, action: () -> Boolean): ButtonWidget<*> = ReplayButtonWidget()
    .left(left).top(top).size(width, ReplayEditorMetrics.BUTTON_HEIGHT)
    .background(ReplayUiStyle.buttonBackground()).overlay(ReplayLang.key(labelKey))
    .onMousePressed { it == 0 && action() }

internal class ReplayTextWidget : TextWidget<ReplayTextWidget> {

    constructor(key: IKey) :
        super(
            key,
        )

    constructor(text: String) :
        super(
            text,
        )
}
