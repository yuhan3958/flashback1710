package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ScrollWidget
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData
import com.cleanroommc.modularui.widgets.ButtonWidget
import me.yuhan8954.flashback.editor.ReplayTimelineEventType
import me.yuhan8954.flashback.replay.ReplayPlayer
import java.util.Locale

class ReplayInspectorPanel : ScrollWidget<ReplayInspectorPanel>(VerticalScrollData(false, 4)) {
    init {
        scrollArea.scrollY.scrollSize = 390
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget("INSPECTOR").left(8).top(7).color(ReplayUiStyle.TEXT_COLOR))
        child(ReplayTextWidget(IKey.dynamic { title() }).left(8).top(26).color(ReplayUiStyle.TEXT_COLOR))
        child(ReplayTextWidget(IKey.dynamic { detail() }).left(8).top(44).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        child(button("Use camera pose", 8, 64, 110) { ReplayPlayer.updateSelectedCameraKeyframePose() })
        child(button("Move to playhead", 8, 86, 110) { ReplayPlayer.moveSelectedKeyframe(ReplayPlayer.currentTimeNanos) })
        child(button("Delete key", 8, 108, 110) { ReplayPlayer.deleteSelectedKeyframe() })
        child(ReplayTextWidget("Value (FOV / Speed)").left(8).top(134).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        child(
            ReplayTextWidget(IKey.dynamic { ReplayValueInput.text ?: "Click Edit, type value, Enter" })
                .left(8).top(170).color(ReplayUiStyle.TEXT_COLOR),
        )
        child(button("Edit value", 8, 190, 82) { ReplayValueInput.begin() })
        child(button("-1", 8, 150, 30) { adjust(-1.0f) })
        child(button("-0.1", 42, 150, 38) { adjust(-0.1f) })
        child(button("+0.1", 84, 150, 38) { adjust(0.1f) })
        child(button("+1", 126, 150, 30) { adjust(1.0f) })
        child(
            ReplayTextWidget(IKey.dynamic { "In ${format(ReplayPlayer.inPointNanos)}  Out ${format(ReplayPlayer.outPointNanos)}" })
                .left(8).top(216).color(ReplayUiStyle.MUTED_TEXT_COLOR),
        )
        child(
            ReplayTextWidget(IKey.dynamic { "Camera speed ${ReplayPlayer.cameraSpeed ?: 0.0}" })
                .left(8).top(236).color(ReplayUiStyle.MUTED_TEXT_COLOR),
        )
        child(button("Cam -", 8, 252, 48) { changeCameraSpeed(0.8) })
        child(button("Cam +", 60, 252, 48) { changeCameraSpeed(1.25) })
        child(button("Set In", 8, 280, 48) { ReplayPlayer.setInPoint() })
        child(button("Set Out", 60, 280, 52) { ReplayPlayer.setOutPoint() })
        child(button("Clear", 8, 302, 48) { ReplayPlayer.clearInOutRange() })
        child(
            button("HUD", 60, 302, 48) {
                ReplayUiController.toggleHudVisibility()
                true
            },
        )
        child(ReplayTextWidget("DEBUG OVERLAYS").left(8).top(334).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        child(button("Packets", 8, 350, 60) { ReplayPlayer.toggleTimelineFilter(ReplayTimelineEventType.PACKET) })
        child(button("Checkpoints", 72, 350, 86) { ReplayPlayer.toggleTimelineFilter(ReplayTimelineEventType.CHECKPOINT) })
    }

    private fun title(): String = ReplayPlayer.selectedKeyframe?.let { "${it.trackId.uppercase()} KEYFRAME" } ?: "REPLAY"

    private fun detail(): String = ReplayPlayer.selectedKeyframe?.let {
        val value = ReplayPlayer.selectedCameraKeyframe?.let { camera ->
            "${camera.x.toInt()}, ${camera.y.toInt()}, ${camera.z.toInt()}  ${camera.yaw.toInt()} / ${camera.pitch.toInt()}"
        } ?: ReplayPlayer.selectedFloatValue?.let { value ->
            String.format(Locale.ROOT, "%.2f", value)
        } ?: "pose"
        "${ReplayTimeFormatter.format(it.timestampNanos)}  $value"
    } ?: "${ReplayTimeFormatter.format(ReplayPlayer.currentTimeNanos)} / ${ReplayTimeFormatter.format(ReplayPlayer.totalDurationNanos)}"

    private fun format(time: Long?): String = time?.let(ReplayTimeFormatter::format) ?: "--"

    private fun adjust(delta: Float): Boolean {
        val value = ReplayPlayer.selectedFloatValue ?: return false
        return ReplayPlayer.setSelectedFloatValue(value + delta)
    }

    private fun changeCameraSpeed(multiplier: Double): Boolean {
        val speed = ReplayPlayer.cameraSpeed ?: return false
        return ReplayPlayer.setCameraSpeed(speed * multiplier)
    }

    private fun button(label: String, left: Int, top: Int, width: Int, action: () -> Boolean): ButtonWidget<*> = ReplayButtonWidget().left(left).top(top).size(width, 18)
        .background(ReplayUiStyle.buttonBackground()).overlay(IKey.str(label))
        .onMousePressed { it == 0 && action() }
}
