package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.widget.ParentWidget
import me.yuhan8954.flashback.ReplayLang
import me.yuhan8954.flashback.replay.ReplayPlayer

class ReplayEditorToolbar : ParentWidget<ReplayEditorToolbar>() {
    init {
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget(ReplayLang.key("ui.brand")).left(6).top(7).color(ReplayUiStyle.TEXT_COLOR))
        child(editorButton("ui.player_camera", 64, 3, 48) { ReplayPlayer.disableFreeCamera() })
        child(editorButton("ui.free_camera", 114, 3, 40) { ReplayPlayer.enableFreeCamera() })
        child(
            ReplayButtonWidget().right(4).top(3).size(36, 18)
                .background(ReplayUiStyle.buttonBackground()).overlay(ReplayLang.key("ui.exit"))
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
