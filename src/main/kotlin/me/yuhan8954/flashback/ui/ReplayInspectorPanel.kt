package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ScrollWidget
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData
import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.editor.ReplayTimelineEventType
import me.yuhan8954.flashback.replay.ReplayPlayer
import java.util.Locale

class ReplayInspectorPanel : ScrollWidget<ReplayInspectorPanel>(VerticalScrollData(false, 4)) {
    init {
        scrollArea.scrollY.scrollSize = 270
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget("INSPECTOR").left(8).top(7).color(ReplayUiStyle.TEXT_COLOR))
        child(ReplayTextWidget(IKey.dynamic { title() }).left(8).top(26).color(ReplayUiStyle.TEXT_COLOR))
        child(ReplayTextWidget(IKey.dynamic { detail() }).left(8).top(44).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        child(
            editorButton("Use pose", 8, 64, 100) { ReplayEditorController.updateSelectedCameraKeyframePose() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe?.trackId == "camera" },
        )
        child(
            editorButton("Move here", 8, 86, 100) { ReplayEditorController.moveSelectedKeyframe(ReplayPlayer.currentTimeNanos) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe != null },
        )
        child(
            editorButton("Delete key", 8, 108, 100) { ReplayEditorController.deleteSelectedKeyframe() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe != null },
        )
        listOf("X", "Y", "Z", "Yaw", "Pitch").forEachIndexed { index, field ->
            child(
                ReplayButtonWidget().left(8).top(134 + index * 17).size(100, 16)
                    .background(ReplayUiStyle.buttonBackground())
                    .overlay(IKey.dynamic { cameraField(field) })
                    .onMousePressed { it == 0 && ReplayValueInput.beginCamera(field) }
                    .setEnabledIf { ReplayPlayer.selectedKeyframe?.trackId == "camera" },
            )
        }
        child(
            ReplayTextWidget(IKey.dynamic { ReplayValueInput.text?.let { "Value: $it|" } ?: "Click a field to edit" })
                .left(8).top(224).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe?.trackId == "camera" },
        )
        child(
            ReplayTextWidget("Value (FOV / Speed)").left(8).top(134).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            ReplayTextWidget(IKey.dynamic { ReplayValueInput.text?.let { "Value: $it|" } ?: "Click Edit, type value, Enter" })
                .left(8).top(196).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("Edit value", 8, 214, 82) { ReplayValueInput.begin() }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("-1", 8, 150, 44) { adjust(-1.0f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("-0.1", 56, 150, 48) { adjust(-0.1f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("+0.1", 8, 172, 44) { adjust(0.1f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("+1", 56, 172, 48) { adjust(1.0f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            ReplayTextWidget(IKey.dynamic { "In ${format(ReplayPlayer.inPointNanos)}  Out ${format(ReplayPlayer.outPointNanos)}" })
                .left(8).top(112).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            ReplayTextWidget(IKey.dynamic { "Camera speed ${ReplayPlayer.cameraSpeed ?: 0.0}" })
                .left(8).top(64).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("Cam -", 8, 82, 48) { changeCameraSpeed(0.8) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("Cam +", 60, 82, 48) { changeCameraSpeed(1.25) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("Set In", 8, 130, 48) { ReplayEditorController.setInPoint() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("Set Out", 60, 130, 52) { ReplayEditorController.setOutPoint() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("Clear", 8, 152, 48) { ReplayEditorController.clearInOutRange() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("HUD", 60, 152, 48) {
                ReplayUiController.toggleHudVisibility()
                true
            }.setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            ReplayTextWidget("DEBUG OVERLAYS").left(8).top(184).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("Pkt", 8, 200, 48) { ReplayEditorController.toggleTimelineFilter(ReplayTimelineEventType.PACKET) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("Check", 60, 200, 48) { ReplayEditorController.toggleTimelineFilter(ReplayTimelineEventType.CHECKPOINT) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
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

    private fun cameraField(field: String): String {
        val pose = ReplayPlayer.selectedCameraKeyframe ?: return ""
        val value = when (field) {
            "X" -> pose.x
            "Y" -> pose.y
            "Z" -> pose.z
            "Yaw" -> pose.yaw.toDouble()
            else -> pose.pitch.toDouble()
        }
        return "$field: ${String.format(Locale.ROOT, "%.2f", value)}"
    }

    private fun adjust(delta: Float): Boolean {
        val value = ReplayPlayer.selectedFloatValue ?: return false
        return ReplayEditorController.setSelectedFloatValue(value + delta)
    }

    private fun changeCameraSpeed(multiplier: Double): Boolean {
        val speed = ReplayPlayer.cameraSpeed ?: return false
        return ReplayPlayer.setCameraSpeed(speed * multiplier)
    }
}
