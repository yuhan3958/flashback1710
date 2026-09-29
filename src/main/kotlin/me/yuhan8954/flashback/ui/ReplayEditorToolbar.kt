package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayEditorToolbar : ParentWidget<ReplayEditorToolbar>() {
    init {
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget("FLASHBACK 1710").left(6).top(7).color(ReplayUiStyle.TEXT_COLOR))
        child(button("Player", 102, 48) { ReplayPlayer.disableFreeCamera() })
        child(button("Free", 152, 40) { ReplayPlayer.enableFreeCamera() })
        child(button("Marker", 196, 48) { ReplayPlayer.addMarker() })
        child(
            ReplayButtonWidget().right(4).top(3).size(36, 18)
                .background(ReplayUiStyle.buttonBackground()).overlay(IKey.str("Exit"))
                .onMousePressed {
                    if (it != 0) {
                        false
                    } else {
                        ReplayPlayer.stop()
                        true
                    }
                },
        )
    }

    private fun button(label: String, left: Int, width: Int, action: () -> Boolean): ButtonWidget<*> = ReplayButtonWidget().left(left).top(3).size(width, 18)
        .background(ReplayUiStyle.buttonBackground()).overlay(IKey.str(label))
        .onMousePressed { it == 0 && action() }
}
