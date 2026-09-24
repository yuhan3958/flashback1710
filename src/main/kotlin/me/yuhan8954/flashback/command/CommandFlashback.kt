package me.yuhan8954.flashback.command

import me.yuhan8954.flashback.recording.ReplayRecorder
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

    override fun getCommandUsage(sender: ICommandSender): String = "/flashback <record|stop|play>"

    override fun getRequiredPermissionLevel(): Int = 0

    override fun processCommand(
        sender: ICommandSender,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            send("Usage: /flashback <record|stop|play>")
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

                send("Recording stopped")
            }

            "play" -> {
                if (!testReplay.exists()) {
                    send("Replay file not found")
                    return
                }

                ReplayPlayer.play(testReplay)

                send("Playback started")
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
            )
        }

        return null
    }

    private fun send(message: String) {
        mc.thePlayer?.addChatMessage(
            ChatComponentText(
                "§b[Flashback] §f$message",
            ),
        )
    }
}
