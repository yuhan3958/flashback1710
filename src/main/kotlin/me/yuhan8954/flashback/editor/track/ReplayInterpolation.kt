package me.yuhan8954.flashback.editor.track

enum class ReplayInterpolation(val serializedId: String) {
    HOLD("hold"),
    LINEAR("linear"),
    SMOOTH("smooth"),
    ;

    companion object {
        fun fromSerializedId(id: String): ReplayInterpolation = entries.firstOrNull {
            it.serializedId == id || it.name == id
        } ?: throw IllegalArgumentException("Unknown replay interpolation: $id")
    }
}
