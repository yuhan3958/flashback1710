package me.yuhan8954.flashback.editor.track

enum class ReplayInterpolation(val serializedId: String) {
    LINEAR("linear"),
    ;

    companion object {
        fun fromSerializedId(id: String): ReplayInterpolation = entries.firstOrNull {
            it.serializedId == id || it.name == id
        } ?: throw IllegalArgumentException("Unknown replay interpolation: $id")
    }
}
