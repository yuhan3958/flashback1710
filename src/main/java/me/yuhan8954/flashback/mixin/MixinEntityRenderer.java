package me.yuhan8954.flashback.mixin;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.yuhan8954.flashback.replay.ReplayPlayer;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"), cancellable = true)
    private void flashback1710$suppressReplayReconstructionRender(float partialTicks, CallbackInfo ci) {
        if (ReplayPlayer.getReconstructing()) {
            ci.cancel();
        }
    }

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void flashback1710$applyEditorFov(float partialTicks, boolean useSetting,
        CallbackInfoReturnable<Float> cir) {
        if (!useSetting) {
            return;
        }

        Float fov = ReplayPlayer.editorFov();
        if (fov != null) {
            cir.setReturnValue(fov);
        }
    }
}
