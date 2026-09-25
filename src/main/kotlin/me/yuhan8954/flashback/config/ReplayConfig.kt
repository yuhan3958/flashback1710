package me.yuhan8954.flashback.config

import net.minecraftforge.common.config.Configuration
import java.io.File

object ReplayConfig {

    var checkpointIntervalSeconds = 30
        private set

    var checkpointAnchorIntervalSeconds = 300
        private set

    @JvmStatic
    fun load(file: File) {
        val configuration = Configuration(file)

        configuration.load()

        checkpointIntervalSeconds = configuration.getInt(
            "checkpointIntervalSeconds",
            "recording",
            30,
            0,
            3600,
            "Interval in seconds between delta replay checkpoints. 0 disables delta checkpoints.",
        )

        checkpointAnchorIntervalSeconds = configuration.getInt(
            "checkpointAnchorIntervalSeconds",
            "recording",
            300,
            0,
            86400,
            "Interval in seconds between full replay checkpoint anchors. 0 disables full anchors.",
        )

        if (configuration.hasChanged()) {
            configuration.save()
        }
    }
}
