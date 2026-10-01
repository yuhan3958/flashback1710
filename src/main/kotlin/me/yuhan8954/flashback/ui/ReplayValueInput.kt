package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.editor.ReplayEditorController
import me.yuhan8954.flashback.replay.ReplayPlayer

object ReplayValueInput {
    private var cameraField: String? = null
    private var timeOfDay = false

    var text: String? = null
        private set

    fun begin(): Boolean {
        if (ReplayPlayer.selectedFloatValue == null) return false
        cameraField = null
        timeOfDay = false
        text = ""
        return true
    }

    fun beginCamera(field: String): Boolean {
        if (ReplayPlayer.selectedCameraKeyframe == null) return false
        cameraField = field
        timeOfDay = false
        text = ""
        return true
    }

    fun beginTimeOfDay(): Boolean {
        if (ReplayPlayer.selectedTimeOfDay == null) return false
        cameraField = null
        timeOfDay = true
        text = ""
        return true
    }

    fun type(character: Char) {
        if (character.isDigit() || character == '.' || character == '-') text += character
    }

    fun backspace() {
        text = text?.dropLast(1)
    }

    fun commit(): Boolean {
        val saved = if (timeOfDay) {
            val ticks = text?.toIntOrNull() ?: return false
            ReplayEditorController.setSelectedTimeOfDay(ticks)
        } else {
            val value = text?.toDoubleOrNull() ?: return false
            cameraField?.let { ReplayEditorController.setSelectedCameraField(it, value) }
                ?: ReplayEditorController.setSelectedFloatValue(value.toFloat())
        }
        if (!saved) return false
        text = null
        cameraField = null
        timeOfDay = false
        return true
    }

    fun cancel() {
        text = null
        cameraField = null
        timeOfDay = false
    }
}
