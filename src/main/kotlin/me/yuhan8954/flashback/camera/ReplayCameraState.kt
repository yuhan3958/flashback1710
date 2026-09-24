package me.yuhan8954.flashback.camera

data class ReplayCameraState(
    var active: Boolean = false,
    var movementSpeed: Double =
        ReplayCameraController.DEFAULT_MOVEMENT_SPEED,
)
