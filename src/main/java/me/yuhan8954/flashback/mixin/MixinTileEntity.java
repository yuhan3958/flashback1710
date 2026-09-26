package me.yuhan8954.flashback.mixin;

import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.yuhan8954.flashback.replay.ReplayMutationHooks;

@Mixin(TileEntity.class)
public abstract class MixinTileEntity {

    @Inject(method = "markDirty", at = @At("HEAD"))
    private void flashback1710$markDirty(CallbackInfo ci) {
        ReplayMutationHooks.markTileEntityDirty((TileEntity) (Object) this);
    }
}
