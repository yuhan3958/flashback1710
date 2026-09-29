package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.replay.ReplayPlayer

object ReplayValueInput {
    private var cameraField: String? = null

    var text: String? = null
        private set

    fun begin(): Boolean {
        if (ReplayPlayer.selectedFloatValue == null) return false
        cameraField = null
        text = ""
        return true
    }

    fun beginCamera(field: String): Boolean {
        if (ReplayPlayer.selectedCameraKeyframe == null) return false
        cameraField = field
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
        val value = text?.toDoubleOrNull() ?: return false
        val saved = cameraField?.let { ReplayPlayer.setSelectedCameraField(it, value) }
            ?: ReplayPlayer.setSelectedFloatValue(value.toFloat())
        if (!saved) return false
        text = null
        cameraField = null
        return true
    }

    fun cancel() {
        text = null
        cameraField = null
    }
}
