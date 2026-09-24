package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import com.cleanroommc.modularui.widgets.TextWidget

internal class ReplayButtonWidget : ButtonWidget<ReplayButtonWidget>()

internal class ReplayContainerWidget : ParentWidget<ReplayContainerWidget>()

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
