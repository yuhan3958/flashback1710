package me.yuhan8954.flashback.command

import me.yuhan8954.flashback.recording.ReplayRecorder
import me.yuhan8954.flashback.replay.ReplayClock
import me.yuhan8954.flashback.replay.ReplayPlayer
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

    private val testReplay: File
        get() = File(replayDirectory, "latest.fbr")

    override fun getCommandName(): String = "flashback"

    override fun getCommandUsage(sender: ICommandSender): String = "/flashback <record|stop|play|pause|resume|toggle|speed|step>"

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
                replayDirectory.mkdirs()

                ReplayRecorder.start(testReplay)

                send("Recording started")
            }

            "stop" -> {
                ReplayRecorder.stop()
                ReplayPlayer.stop()

                send("Recording and playback stopped")
            }

            "play" -> {
                if (!testReplay.exists()) {
                    send("Replay file not found")
                    return
                }

                ReplayPlayer.play(testReplay)

                send("Playback started")
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
                "play",
                "pause",
                "resume",
                "toggle",
                "speed",
                "step",
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
