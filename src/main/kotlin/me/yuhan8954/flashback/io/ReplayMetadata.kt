package me.yuhan8954.flashback.io

import me.yuhan8954.flashback.Flashback1710

data class ReplayMetadata(
    val createdAtEpochMillis: Long,
    val minecraftVersion: String,
    val flashbackVersion: String,
    val gtnhVersion: String? = null,
    val modFingerprint: String? = null,
) {

    companion object {

        fun current(): ReplayMetadata = ReplayMetadata(
            createdAtEpochMillis = System.currentTimeMillis(),
            minecraftVersion = "1.7.10",
            flashbackVersion = Flashback1710.VERSION,
        )
    }
}
