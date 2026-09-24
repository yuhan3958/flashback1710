package me.yuhan8954.flashback.camera

import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class EntityReplayCamera(
    world: World,
) : EntityLivingBase(
    world,
) {

    init {
        noClip = true
        ignoreFrustumCheck = true

        setSize(
            0.0f,
            0.0f,
        )
    }

    override fun onUpdate() {
        noClip = true
    }

    override fun getEyeHeight(): Float = 0.0f

    override fun canBeCollidedWith(): Boolean = false

    override fun getHeldItem(): ItemStack? = null

    override fun getEquipmentInSlot(slot: Int): ItemStack? = null

    override fun setCurrentItemOrArmor(
        slot: Int,
        stack: ItemStack?,
    ) {}

    override fun getLastActiveItems(): Array<ItemStack?> =
        emptyArray()
}
