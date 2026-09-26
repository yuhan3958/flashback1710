package me.yuhan8954.flashback.replay

import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity

object ReplayMutationHooks {

    @JvmStatic
    fun markTileEntityDirty(
        tileEntity: TileEntity,
    ) {
        val world =
            tileEntity.worldObj as?
                ReplayWorld
                ?: return

        world.mutationJournal
            .markTileEntityDirty(
                tileEntity,
            )
    }

    @JvmStatic
    fun markEntityDirty(
        entity: Entity,
    ) {
        val world =
            entity.worldObj as?
                ReplayWorld
                ?: return

        world.mutationJournal
            .markEntityDirty(
                entity,
            )
    }
}
