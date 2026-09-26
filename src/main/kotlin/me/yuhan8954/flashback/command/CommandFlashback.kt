package me.yuhan8954.flashback.command

import me.yuhan8954.flashback.camera.ReplayCameraController
import me.yuhan8954.flashback.recording.ReplayRecorder
import me.yuhan8954.flashback.replay.ReplayClock
import me.yuhan8954.flashback.replay.ReplayPlayer
import me.yuhan8954.flashback.ui.ReplayLibraryController
import me.yuhan8954.flashback.ui.ReplayUiController
import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.ChatComponentText
import java.io.File

class CommandFlashback : CommandBase() {

    private val mc: Minecraft
        get() = Minecraft.getMinecraft()

    private val replayDirectory: File
        get() = File(mc.mcDataDir, "replays")

    override fun getCommandName(): String = "flashback"

    override fun getCommandUsage(sender: ICommandSender): String = "/flashback <record|stop|library|play|pause|resume|toggle|speed|step|camera|ui>"

    override fun getRequiredPermissionLevel(): Int = 0

    override fun processCommand(
        sender: ICommandSender,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            send(
                "Usage: " +
                    getCommandUsage(
                        sender,
                    ),
            )
            return
        }

        when (args[0].lowercase()) {
            "record" -> {
                val file =
                    ReplayRecorder.startNew(
                        replayDirectory,
                    )

                send(
                    "Recording started: " +
                        file.name,
                )
            }

            "stop" -> {
                ReplayRecorder.stop()
                ReplayPlayer.stop()

                send("Recording and playback stopped")
            }

            "library" -> {
                ReplayLibraryController.open()
            }

            "play" -> {
                val fileName =
                    args.getOrNull(
                        1,
                    )

                if (fileName == null) {
                    ReplayLibraryController.open()
                    return
                }

                val file =
                    replayDirectory.listFiles()
                        .orEmpty()
                        .firstOrNull {
                            it.isFile &&
                                it.name ==
                                fileName
                        }

                if (
                    file == null ||
                    !file.extension.equals(
                        "fbr",
                        ignoreCase = true,
                    )
                ) {
                    send("Replay file not found")
                    return
                }

                ReplayPlayer.play(
                    file,
                )

                send(
                    "Playback started paused: " +
                        file.name,
                )
            }

            "pause" -> {
                if (
                    !ReplayPlayer.pause()
                ) {
                    send("No replay is playing")
                    return
                }

                send("Playback paused")
            }

            "resume" -> {
                if (
                    !ReplayPlayer.resume()
                ) {
                    send("No replay is playing")
                    return
                }

                send("Playback resumed")
            }

            "toggle" -> {
                if (
                    !ReplayPlayer.togglePause()
                ) {
                    send("No replay is playing")
                    return
                }

                send(
                    if (ReplayPlayer.paused) {
                        "Playback paused"
                    } else {
                        "Playback resumed"
                    },
                )
            }

            "speed" -> {
                setPlaybackSpeed(
                    args,
                )
            }

            "step" -> {
                if (
                    !ReplayPlayer.playing
                ) {
                    send("No replay is playing")
                    return
                }

                if (
                    !ReplayPlayer.step()
                ) {
                    send("Pause playback before stepping")
                    return
                }

                send("Playback advanced by one tick")
            }

            "camera" -> {
                controlCamera(
                    args,
                )
            }

            "ui" -> {
                if (!ReplayPlayer.playing) {
                    send("No replay is playing")
                    return
                }

                ReplayUiController.open()
            }

            else -> {
                send("Unknown subcommand: ${args[0]}")
            }
        }
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
    ): List<String>? {
        if (args.size == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                "record",
                "stop",
                "library",
                "play",
                "pause",
                "resume",
                "toggle",
                "speed",
                "step",
                "camera",
                "ui",
            )
        }

        if (
            args.size == 2 &&
            args[0].equals(
                "play",
                ignoreCase = true,
            )
        ) {
            return getListOfStringsMatchingLastWord(
                args,
                *replayDirectory.listFiles()
                    .orEmpty()
                    .asSequence()
                    .filter {
                        it.isFile &&
                            it.extension.equals(
                                "fbr",
                                ignoreCase = true,
                            )
                    }.map {
                        it.name
                    }.toList()
                    .toTypedArray(),
            )
        }

        if (
            args.size == 2 &&
            args[0].equals(
                "camera",
                ignoreCase = true,
            )
        ) {
            return getListOfStringsMatchingLastWord(
                args,
                "free",
                "player",
                "speed",
            )
        }

        if (
            args.size == 2 &&
            args[0].equals(
                "speed",
                ignoreCase = true,
            )
        ) {
            return getListOfStringsMatchingLastWord(
                args,
                *ReplayClock.SUPPORTED_SPEEDS
                    .map {
                        it.toString()
                    }.toTypedArray(),
            )
        }

        return null
    }

    private fun controlCamera(args: Array<String>) {
        if (!ReplayPlayer.playing) {
            send("No replay is playing")
            return
        }

        when (
            args.getOrNull(
                1,
            )?.lowercase()
        ) {
            "free" -> {
                if (ReplayPlayer.freeCameraActive) {
                    send("Free camera is already active")
                    return
                }

                ReplayPlayer.enableFreeCamera()
                send("Free camera enabled")
            }

            "player" -> {
                if (!ReplayPlayer.freeCameraActive) {
                    send("Player camera is already active")
                    return
                }

                ReplayPlayer.disableFreeCamera()
                send("Player camera enabled")
            }

            "speed" ->
                setCameraSpeed(
                    args,
                )

            else ->
                send(
                    "Usage: /flashback camera <free|player|speed>",
                )
        }
    }

    private fun setCameraSpeed(args: Array<String>) {
        val speed =
            args.getOrNull(
                2,
            )?.toDoubleOrNull()

        if (
            speed == null ||
            speed < ReplayCameraController.MIN_MOVEMENT_SPEED ||
            speed > ReplayCameraController.MAX_MOVEMENT_SPEED ||
            speed.isNaN() ||
            speed.isInfinite()
        ) {
            send(
                "Invalid camera speed. Use ${ReplayCameraController.MIN_MOVEMENT_SPEED} to " +
                    ReplayCameraController.MAX_MOVEMENT_SPEED,
            )

            return
        }

        ReplayPlayer.setCameraSpeed(
            speed,
        )

        send(
            "Free camera speed set to $speed",
        )
    }

    private fun setPlaybackSpeed(args: Array<String>) {
        if (!ReplayPlayer.playing) {
            send("No replay is playing")
            return
        }

        val speed =
            args.getOrNull(
                1,
            )?.toDoubleOrNull()

        if (
            speed == null ||
            !ReplayClock.isSupportedSpeed(
                speed,
            )
        ) {
            send(
                "Invalid speed. Supported values: " +
                    ReplayClock.SUPPORTED_SPEEDS
                        .joinToString(", "),
            )

            return
        }

        ReplayPlayer.setSpeed(
            speed,
        )

        send(
            "Playback speed set to ${speed}x",
        )
    }

    private fun send(message: String) {
        mc.thePlayer?.addChatMessage(
            ChatComponentText(
                "§b[Flashback] §f$message",
            ),
        )
    }
}
