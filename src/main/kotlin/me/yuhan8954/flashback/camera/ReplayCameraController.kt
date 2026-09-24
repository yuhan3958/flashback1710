package me.yuhan8954.flashback.camera

import me.yuhan8954.flashback.replay.ReplaySession
import me.yuhan8954.flashback.ui.ReplayUiScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.MovementInput
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ReplayCameraController(
    private val session: ReplaySession,
) {

    val state =
        ReplayCameraState()

    private val minecraft:
        Minecraft
        get() = session.minecraft

    private var camera:
        EntityReplayCamera? =
        null

    private var playerMovementInput:
        MovementInput? =
        null

    private var playerYaw =
        0.0f

    private var playerPitch =
        0.0f

    private var playerPreviousYaw =
        0.0f

    private var playerPreviousPitch =
        0.0f

    private var pendingMouseX =
        0

    private var pendingMouseY =
        0

    fun enable(): Boolean {
        if (state.active) {
            return false
        }

        val view =
            minecraft.renderViewEntity
                ?: session.player

        camera =
            EntityReplayCamera(
                session.world,
            ).also {
                it.setPositionAndRotation(
                    view.posX,
                    view.posY +
                        view.eyeHeight,
                    view.posZ,
                    view.rotationYaw,
                    view.rotationPitch,
                )

                it.prevPosX = it.posX
                it.prevPosY = it.posY
                it.prevPosZ = it.posZ
                it.lastTickPosX = it.posX
                it.lastTickPosY = it.posY
                it.lastTickPosZ = it.posZ
                it.prevRotationYaw = it.rotationYaw
                it.prevRotationPitch = it.rotationPitch
            }

        playerMovementInput =
            session.player.movementInput

        session.player.movementInput =
            MovementInput()

        capturePlayerRotation()

        pendingMouseX = 0
        pendingMouseY = 0
        state.active = true
        minecraft.renderViewEntity = camera

        return true
    }

    fun disable(): Boolean {
        if (!state.active) {
            return false
        }

        restorePlayerRotation()

        minecraft.renderViewEntity =
            session.player

        playerMovementInput?.let {
            session.player.movementInput =
                it
        }

        playerMovementInput = null
        camera = null
        pendingMouseX = 0
        pendingMouseY = 0
        state.active = false

        return true
    }

    fun close() {
        disable()
    }

    fun beginTick() {
        if (!state.active) {
            return
        }

        capturePlayerRotation()
    }

    fun tick() {
        if (!state.active) {
            return
        }

        restorePlayerRotation()
        prepareCameraTick()

        val replayUiOpen =
            minecraft.currentScreen is
                ReplayUiScreen

        if (
            (
                !minecraft.inGameHasFocus ||
                    minecraft.currentScreen != null
                ) &&
            !replayUiOpen
        ) {
            pendingMouseX = 0
            pendingMouseY = 0
            return
        }

        updateRotation()
        updatePosition()
    }

    fun handleUiMouseInput(
        deltaX: Int,
        deltaY: Int,
    ): Boolean {
        if (!state.active) {
            return false
        }

        pendingMouseX += deltaX
        pendingMouseY += deltaY
        return true
    }

    fun handleMouseInput(
        deltaX: Int,
        deltaY: Int,
        wheelDelta: Int,
    ): Boolean {
        if (
            !state.active ||
            !minecraft.inGameHasFocus ||
            minecraft.currentScreen != null
        ) {
            return false
        }

        pendingMouseX += deltaX
        pendingMouseY += deltaY

        if (wheelDelta != 0) {
            val multiplier =
                if (wheelDelta > 0) {
                    SPEED_STEP_MULTIPLIER
                } else {
                    1.0 /
                        SPEED_STEP_MULTIPLIER
                }

            setMovementSpeed(
                state.movementSpeed *
                    multiplier,
            )
        }

        return true
    }

    fun setMovementSpeed(speed: Double): Boolean {
        if (
            speed < MIN_MOVEMENT_SPEED ||
            speed > MAX_MOVEMENT_SPEED ||
            speed.isNaN() ||
            speed.isInfinite()
        ) {
            return false
        }

        state.movementSpeed = speed
        return true
    }

    private fun restorePlayerRotation() {
        val player =
            session.player

        player.rotationYaw = playerYaw
        player.rotationPitch = playerPitch
        player.prevRotationYaw = playerPreviousYaw
        player.prevRotationPitch = playerPreviousPitch
    }

    private fun capturePlayerRotation() {
        val player =
            session.player

        playerYaw = player.rotationYaw
        playerPitch = player.rotationPitch
        playerPreviousYaw = player.prevRotationYaw
        playerPreviousPitch = player.prevRotationPitch
    }

    private fun prepareCameraTick() {
        val currentCamera =
            camera ?: return

        currentCamera.prevPosX =
            currentCamera.posX
        currentCamera.prevPosY =
            currentCamera.posY
        currentCamera.prevPosZ =
            currentCamera.posZ
        currentCamera.lastTickPosX =
            currentCamera.posX
        currentCamera.lastTickPosY =
            currentCamera.posY
        currentCamera.lastTickPosZ =
            currentCamera.posZ
        currentCamera.prevRotationYaw =
            currentCamera.rotationYaw
        currentCamera.prevRotationPitch =
            currentCamera.rotationPitch
    }

    private fun updateRotation() {
        val currentCamera =
            camera ?: return

        val sensitivity =
            minecraft.gameSettings
                .mouseSensitivity *
                0.6f +
                0.2f

        val sensitivityScale =
            sensitivity *
                sensitivity *
                sensitivity *
                8.0f

        val pitchDirection =
            if (
                minecraft.gameSettings
                    .invertMouse
            ) {
                -1
            } else {
                1
            }

        currentCamera.setAngles(
            pendingMouseX *
                sensitivityScale,
            pendingMouseY *
                sensitivityScale *
                pitchDirection,
        )

        pendingMouseX = 0
        pendingMouseY = 0
    }

    private fun updatePosition() {
        val currentCamera =
            camera ?: return

        val settings =
            minecraft.gameSettings

        val forward =
            keyDirection(
                settings.keyBindForward,
                settings.keyBindBack,
            )

        val strafe =
            keyDirection(
                settings.keyBindLeft,
                settings.keyBindRight,
            )

        val vertical =
            keyDirection(
                settings.keyBindJump,
                settings.keyBindSneak,
            )

        if (
            forward == 0.0 &&
            strafe == 0.0 &&
            vertical == 0.0
        ) {
            return
        }

        val yawRadians =
            Math.toRadians(
                currentCamera.rotationYaw
                    .toDouble(),
            )

        var movementX =
            -sin(
                yawRadians,
            ) *
                forward +
                cos(
                    yawRadians,
                ) *
                strafe

        var movementY =
            vertical

        var movementZ =
            cos(
                yawRadians,
            ) *
                forward +
                sin(
                    yawRadians,
                ) *
                strafe

        val length =
            sqrt(
                movementX * movementX +
                    movementY * movementY +
                    movementZ * movementZ,
            )

        if (length <= 0.0) {
            return
        }

        val scale =
            state.movementSpeed /
                length

        movementX *= scale
        movementY *= scale
        movementZ *= scale

        currentCamera.setPosition(
            currentCamera.posX +
                movementX,
            currentCamera.posY +
                movementY,
            currentCamera.posZ +
                movementZ,
        )
    }

    private fun keyDirection(
        positive: KeyBinding,
        negative: KeyBinding,
    ): Double {
        var direction = 0.0

        if (
            GameSettings.isKeyDown(
                positive,
            )
        ) {
            direction += 1.0
        }

        if (
            GameSettings.isKeyDown(
                negative,
            )
        ) {
            direction -= 1.0
        }

        return direction
    }

    companion object {

        const val DEFAULT_MOVEMENT_SPEED = 0.4
        const val MIN_MOVEMENT_SPEED = 0.05
        const val MAX_MOVEMENT_SPEED = 8.0

        private const val SPEED_STEP_MULTIPLIER = 1.25
    }
}
