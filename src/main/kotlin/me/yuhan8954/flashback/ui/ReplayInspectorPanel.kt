package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widget.ScrollWidget
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData
import me.yuhan8954.flashback.ReplayLang
import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.editor.ReplayTimelineEventType
import me.yuhan8954.flashback.editor.track.ReplayInterpolation
import me.yuhan8954.flashback.replay.ReplayPlayer
import java.util.Locale

class ReplayInspectorPanel : ScrollWidget<ReplayInspectorPanel>(VerticalScrollData(false, 4)) {
    init {
        scrollArea.scrollY.scrollSize = 308
        background(ReplayUiStyle.panelBackground(), ReplayUiStyle.panelBorder())
        child(ReplayTextWidget(ReplayLang.key("ui.inspector")).left(8).top(7).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        child(ReplayTextWidget(IKey.dynamic { title() }).left(8).top(26).color(ReplayUiStyle.TEXT_COLOR))
        child(ReplayTextWidget(IKey.dynamic { detail() }).left(8).top(44).color(ReplayUiStyle.MUTED_TEXT_COLOR))
        child(
            editorButton("ui.use_pose", 8, 64, 100) { ReplayEditorController.updateSelectedCameraKeyframePose() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe?.trackId == "camera" },
        )
        child(
            editorButton("ui.move_here", 8, 86, 100) { ReplayEditorController.moveSelectedKeyframe(ReplayPlayer.currentTimeNanos) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe != null },
        )
        child(
            editorButton("ui.delete_key", 8, 108, 100) { ReplayEditorController.deleteSelectedKeyframe() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe != null },
        )
        child(
            ReplayTextWidget(ReplayLang.key("ui.interpolation"))
                .left(8).top(248).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe != null },
        )
        ReplayInterpolation.entries.forEachIndexed { index, mode ->
            child(
                ReplayButtonWidget().left(8 + index * 42).top(266).size(40, 18)
                    .background(ReplayUiStyle.buttonBackground())
                    .overlay(IKey.dynamic {
                        val name = ReplayLang.text("ui.interpolation.${mode.serializedId}")
                        if (ReplayPlayer.selectedInterpolation == mode) "§b$name" else name
                    })
                    .onMousePressed { it == 0 && ReplayEditorController.updateSelectedInterpolation(mode) }
                    .setEnabledIf { ReplayPlayer.selectedKeyframe != null },
            )
        }
        child(
            ReplayTextWidget(IKey.dynamic { ReplayLang.text("ui.world_time", ReplayPlayer.selectedTimeOfDay ?: "") })
                .left(8).top(134).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedTimeOfDay != null },
        )
        child(
            editorButton("ui.edit_ticks", 8, 152, 100) { ReplayValueInput.beginTimeOfDay() }
                .setEnabledIf { ReplayPlayer.selectedTimeOfDay != null },
        )
        listOf("ui.dawn" to 0, "ui.noon" to 6000, "ui.dusk" to 12000, "ui.midnight" to 18000).forEachIndexed { index, preset ->
            child(
                editorButton(preset.first, 8 + (index % 2) * 52, 176 + (index / 2) * 22, 48) {
                    ReplayEditorController.setSelectedTimeOfDay(preset.second)
                }.setEnabledIf { ReplayPlayer.selectedTimeOfDay != null },
            )
        }
        child(
            ReplayTextWidget(IKey.dynamic {
                ReplayValueInput.text?.let { ReplayLang.text("ui.ticks_input", it) } ?: ReplayLang.text("ui.ticks_hint")
            })
                .left(8).top(224).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedTimeOfDay != null },
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
            ReplayTextWidget(IKey.dynamic {
                ReplayValueInput.text?.let { ReplayLang.text("ui.value_input", it) } ?: ReplayLang.text("ui.camera_hint")
            })
                .left(8).top(224).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe?.trackId == "camera" },
        )
        child(
            ReplayTextWidget(ReplayLang.key("ui.value")).left(8).top(134).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            ReplayTextWidget(IKey.dynamic {
                ReplayValueInput.text?.let { ReplayLang.text("ui.value_input", it) } ?: ReplayLang.text("ui.value_hint")
            })
                .left(8).top(196).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("ui.edit_value", 8, 214, 82) { ReplayValueInput.begin() }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("ui.adjust_minus_one", 8, 150, 44) { adjust(-1.0f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("ui.adjust_minus_tenth", 56, 150, 48) { adjust(-0.1f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("ui.adjust_plus_tenth", 8, 172, 44) { adjust(0.1f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            editorButton("ui.adjust_plus_one", 56, 172, 48) { adjust(1.0f) }
                .setEnabledIf { ReplayPlayer.selectedFloatValue != null },
        )
        child(
            ReplayTextWidget(IKey.dynamic {
                ReplayLang.text("ui.in_out", format(ReplayPlayer.inPointNanos), format(ReplayPlayer.outPointNanos))
            })
                .left(8).top(112).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            ReplayTextWidget(IKey.dynamic { ReplayLang.text("ui.camera_speed", ReplayPlayer.cameraSpeed ?: 0.0) })
                .left(8).top(64).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.camera_slower", 8, 82, 48) { changeCameraSpeed(0.8) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.camera_faster", 60, 82, 48) { changeCameraSpeed(1.25) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.set_in", 8, 130, 48) { ReplayEditorController.setInPoint() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.set_out", 60, 130, 52) { ReplayEditorController.setOutPoint() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.clear", 8, 152, 48) { ReplayEditorController.clearInOutRange() }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            ReplayTextWidget(ReplayLang.key("ui.visuals")).left(8).top(184).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            ReplayTextWidget(IKey.dynamic {
                ReplayLang.text("ui.time_value", ReplayPlayer.visibleTimeOfDay()?.toString() ?: ReplayLang.text("ui.recorded"))
            })
                .left(8).top(202).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.hud", 8, 220, 100) {
                ReplayPlayer.visualOverrides.renderHud = !ReplayPlayer.visualOverrides.renderHud
                true
            }.setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            ReplayTextWidget(IKey.dynamic {
                ReplayLang.text(if (ReplayPlayer.visualOverrides.renderHud) "ui.hud_on" else "ui.hud_off")
            })
                .left(8).top(242).color(ReplayUiStyle.TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            ReplayTextWidget(ReplayLang.key("ui.debug_overlays")).left(8).top(266).color(ReplayUiStyle.MUTED_TEXT_COLOR)
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.packet", 8, 282, 48) { ReplayEditorController.toggleTimelineFilter(ReplayTimelineEventType.PACKET) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
        child(
            editorButton("ui.checkpoint", 60, 282, 48) { ReplayEditorController.toggleTimelineFilter(ReplayTimelineEventType.CHECKPOINT) }
                .setEnabledIf { ReplayPlayer.selectedKeyframe == null },
        )
    }

    private fun title(): String = when (ReplayPlayer.selectedKeyframe?.trackId) {
        "camera" -> ReplayLang.text("ui.camera_keyframe")
        "fov" -> ReplayLang.text("ui.fov_keyframe")
        "speed" -> ReplayLang.text("ui.speed_keyframe")
        "time_of_day" -> ReplayLang.text("ui.time_keyframe")
        else -> ReplayLang.text("ui.replay")
    }

    private fun detail(): String = ReplayPlayer.selectedKeyframe?.let {
        ReplayTimeFormatter.format(it.timestampNanos)
    } ?: ReplayLang.text("ui.length", ReplayTimeFormatter.format(ReplayPlayer.totalDurationNanos))

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
        return ReplayLang.text("ui.field_value", ReplayLang.text("ui.field.${field.lowercase(Locale.ROOT)}"), String.format(Locale.ROOT, "%.2f", value))
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
