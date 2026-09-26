package me.yuhan8954.flashback.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.yuhan8954.flashback.replay.ReplayMutationHooks;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @Inject(method = "setHealth", at = @At("RETURN"))
    private void flashback1710$setHealth(float health, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((EntityLivingBase) (Object) this);
    }

    @Inject(method = "addPotionEffect", at = @At("RETURN"))
    private void flashback1710$addPotionEffect(PotionEffect effect, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((EntityLivingBase) (Object) this);
    }

    @Inject(method = "removePotionEffectClient", at = @At("RETURN"))
    private void flashback1710$removePotionEffectClient(int potionId, CallbackInfoReturnable<PotionEffect> cir) {
        ReplayMutationHooks.markEntityDirty((EntityLivingBase) (Object) this);
    }

    @Inject(method = "clearActivePotions", at = @At("RETURN"))
    private void flashback1710$clearActivePotions(CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((EntityLivingBase) (Object) this);
    }

    @Inject(method = "setAbsorptionAmount", at = @At("RETURN"))
    private void flashback1710$setAbsorptionAmount(float amount, CallbackInfo ci) {
        ReplayMutationHooks.markEntityDirty((EntityLivingBase) (Object) this);
    }
}
