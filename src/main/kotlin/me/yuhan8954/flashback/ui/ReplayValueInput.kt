package me.yuhan8954.flashback.ui

import me.yuhan8954.flashback.replay.ReplayPlayer

object ReplayValueInput {
    var text: String? = null
        private set

    fun begin(): Boolean {
        if (ReplayPlayer.selectedFloatValue == null) return false
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
        val value = text?.toFloatOrNull() ?: return false
        if (!ReplayPlayer.setSelectedFloatValue(value)) return false
        text = null
        return true
    }

    fun cancel() {
        text = null
    }
}
