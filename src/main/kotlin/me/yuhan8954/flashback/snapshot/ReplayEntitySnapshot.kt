package me.yuhan8954.flashback.snapshot

import net.minecraft.nbt.NBTTagCompound

data class ReplayEntitySnapshot(
    val entityId: Int,
    val entityType: String?,
    val entityClass: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val motionX: Double,
    val motionY: Double,
    val motionZ: Double,
    val nbt: NBTTagCompound,
)
