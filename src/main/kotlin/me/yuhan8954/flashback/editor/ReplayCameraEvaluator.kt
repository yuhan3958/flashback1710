package me.yuhan8954.flashback.editor

/** Orbit takes priority when both automated camera tracks have a value. */
object ReplayCameraEvaluator {
    fun evaluate(editor: ReplayEditorState, timeNanos: Long): ReplayCameraPose? =
        editor.project.cameraOrbitTrack.evaluate(timeNanos)?.let(ReplayCameraOrbitMath::toCameraPose)
            ?: editor.cameraPoseAt(timeNanos)
}
