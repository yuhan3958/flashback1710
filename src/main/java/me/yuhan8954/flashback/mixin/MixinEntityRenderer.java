package me.yuhan8954.flashback.mixin;

import me.yuhan8954.flashback.replay.ReplayPlayer;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"), cancellable = true)
    private void flashback1710$suppressReplayReconstructionRender(
        float partialTicks,
        CallbackInfo ci
    ) {
        if (ReplayPlayer.getReconstructing()) {
            ci.cancel();
        }
    }
}
