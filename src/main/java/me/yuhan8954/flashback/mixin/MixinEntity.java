package me.yuhan8954.flashback.mixin;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.yuhan8954.flashback.replay.ReplayMutationHooks;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "setPosition", at = @At("RETURN"))
    private void flashback1710$setPosition(double x, double y, double z, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((Entity) (Object) this);
    }

    @Inject(method = "setPositionAndRotation", at = @At("RETURN"))
    private void flashback1710$setPositionAndRotation(double x, double y, double z, float yaw, float pitch,
        CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((Entity) (Object) this);
    }

    @Inject(method = "setVelocity", at = @At("RETURN"))
    private void flashback1710$setVelocity(double x, double y, double z, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((Entity) (Object) this);
    }

    @Inject(method = "setSneaking", at = @At("RETURN"))
    private void flashback1710$setSneaking(boolean sneaking, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((Entity) (Object) this);
    }

    @Inject(method = "setSprinting", at = @At("RETURN"))
    private void flashback1710$setSprinting(boolean sprinting, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((Entity) (Object) this);
    }

    @Inject(method = "setFire", at = @At("RETURN"))
    private void flashback1710$setFire(int seconds, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((Entity) (Object) this);
    }

    @Inject(method = "extinguish", at = @At("RETURN"))
    private void flashback1710$extinguish(CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((Entity) (Object) this);
    }
}
