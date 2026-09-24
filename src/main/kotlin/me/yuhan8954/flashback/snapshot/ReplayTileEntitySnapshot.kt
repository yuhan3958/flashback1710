package me.yuhan8954.flashback.snapshot

import net.minecraft.nbt.NBTTagCompound

data class ReplayTileEntitySnapshot(
    val x: Int,
    val y: Int,
    val z: Int,
    val nbt: NBTTagCompound,
)
