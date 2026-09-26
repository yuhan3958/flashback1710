package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.screen.GuiScreenWrapper

class ReplayLibraryUiScreen(
    screen: ReplayLibraryPanel,
) : GuiScreenWrapper(
    screen,
) {

    override fun doesGuiPauseGame(): Boolean = false
}
