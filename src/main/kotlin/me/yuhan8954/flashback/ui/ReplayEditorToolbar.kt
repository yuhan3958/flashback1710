package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ParentWidget
import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayEditorToolbar : ParentWidget<ReplayEditorToolbar>() {
    init {
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget("FLASHBACK 1710").left(6).top(7).color(ReplayUiStyle.TEXT_COLOR))
        child(editorButton("Player", 102, 3, 48) { ReplayPlayer.disableFreeCamera() })
        child(editorButton("Free", 152, 3, 40) { ReplayPlayer.enableFreeCamera() })
        child(editorButton("Marker", 196, 3, 48) { ReplayEditorController.addMarker() })
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
}
