package me.yuhan8954.flashback.command

import me.yuhan8954.flashback.camera.ReplayCameraController
import me.yuhan8954.flashback.ReplayLang
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

    override fun getCommandUsage(sender: ICommandSender): String = ReplayLang.text("command.usage")

    override fun getRequiredPermissionLevel(): Int = 0

    override fun processCommand(
        sender: ICommandSender,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            send(
                ReplayLang.text("command.usage_prefix", getCommandUsage(sender)),
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
                    ReplayLang.text("command.record_started", file.name),
                )
            }

            "stop" -> {
                ReplayRecorder.stop()
                ReplayPlayer.stop()

                send(ReplayLang.text("command.stopped"))
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
                    send(ReplayLang.text("command.file_not_found"))
                    return
                }

                ReplayPlayer.play(
                    file,
                )

                send(
                    ReplayLang.text("command.play_started", file.name),
                )
            }

            "pause" -> {
                if (
                    !ReplayPlayer.pause()
                ) {
                    send(ReplayLang.text("command.no_replay"))
                    return
                }

                send(ReplayLang.text("command.paused"))
            }

            "resume" -> {
                if (
                    !ReplayPlayer.resume()
                ) {
                    send(ReplayLang.text("command.no_replay"))
                    return
                }

                send(ReplayLang.text("command.resumed"))
            }

            "toggle" -> {
                if (
                    !ReplayPlayer.togglePause()
                ) {
                    send(ReplayLang.text("command.no_replay"))
                    return
                }

                send(
                    if (ReplayPlayer.paused) {
                        ReplayLang.text("command.paused")
                    } else {
                        ReplayLang.text("command.resumed")
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
                    send(ReplayLang.text("command.no_replay"))
                    return
                }

                if (
                    !ReplayPlayer.step()
                ) {
                    send(ReplayLang.text("command.pause_before_step"))
                    return
                }

                send(ReplayLang.text("command.stepped"))
            }

            "camera" -> {
                controlCamera(
                    args,
                )
            }

            "ui" -> {
                if (!ReplayPlayer.playing) {
                    send(ReplayLang.text("command.no_replay"))
                    return
                }

                ReplayUiController.open()
            }

            else -> {
                send(ReplayLang.text("command.unknown", args[0]))
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
            send(ReplayLang.text("command.no_replay"))
            return
        }

        when (
            args.getOrNull(
                1,
            )?.lowercase()
        ) {
            "free" -> {
                if (ReplayPlayer.freeCameraActive) {
                    send(ReplayLang.text("command.free_already"))
                    return
                }

                ReplayPlayer.enableFreeCamera()
                send(ReplayLang.text("command.free_enabled"))
            }

            "player" -> {
                if (!ReplayPlayer.freeCameraActive) {
                    send(ReplayLang.text("command.player_already"))
                    return
                }

                ReplayPlayer.disableFreeCamera()
                send(ReplayLang.text("command.player_enabled"))
            }

            "speed" ->
                setCameraSpeed(
                    args,
                )

            else ->
                send(
                    ReplayLang.text("command.camera_usage"),
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
                ReplayLang.text(
                    "command.invalid_camera_speed",
                    ReplayCameraController.MIN_MOVEMENT_SPEED,
                    ReplayCameraController.MAX_MOVEMENT_SPEED,
                ),
            )

            return
        }

        ReplayPlayer.setCameraSpeed(
            speed,
        )

        send(
            ReplayLang.text("command.camera_speed_set", speed),
        )
    }

    private fun setPlaybackSpeed(args: Array<String>) {
        if (!ReplayPlayer.playing) {
            send(ReplayLang.text("command.no_replay"))
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
                ReplayLang.text("command.invalid_speed", ReplayClock.SUPPORTED_SPEEDS.joinToString(", ")),
            )

            return
        }

        ReplayPlayer.setSpeed(
            speed,
        )

        send(
            ReplayLang.text("command.speed_set", speed),
        )
    }

    private fun send(message: String) {
        mc.thePlayer?.addChatMessage(
            ChatComponentText(
                "§b${ReplayLang.text("command.prefix")} §f$message",
            ),
        )
    }
}
