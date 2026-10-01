package me.yuhan8954.flashback

import com.cleanroommc.modularui.api.drawable.IKey
import net.minecraft.util.StatCollector

object ReplayLang {
    fun text(key: String, vararg arguments: Any): String =
        StatCollector.translateToLocalFormatted("flashback1710.$key", *arguments)

    fun key(key: String): IKey = IKey.dynamic { text(key) }
}
