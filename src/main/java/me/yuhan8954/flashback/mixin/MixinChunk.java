package me.yuhan8954.flashback.mixin;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.yuhan8954.flashback.replay.ReplayChunkMutationHooks;

@Mixin(Chunk.class)
public abstract class MixinChunk {

    @Inject(method = "func_150807_a", at = @At("HEAD"))
    private void flashback1710$beforeSetBlock(int x, int y, int z, Block block, int metadata,
        CallbackInfoReturnable<Boolean> cir) {
        ReplayChunkMutationHooks.beforeBlockChange((Chunk) (Object) this, x, y, z);
    }

    @Inject(method = "func_150807_a", at = @At("RETURN"))
    private void flashback1710$afterSetBlock(int x, int y, int z, Block block, int metadata,
        CallbackInfoReturnable<Boolean> cir) {
        ReplayChunkMutationHooks.afterBlockChange(cir.getReturnValue());
    }

    @Inject(method = "setBlockMetadata", at = @At("HEAD"))
    private void flashback1710$beforeSetBlockMetadata(int x, int y, int z, int metadata,
        CallbackInfoReturnable<Boolean> cir) {
        ReplayChunkMutationHooks.beforeBlockChange((Chunk) (Object) this, x, y, z);
    }

    @Inject(method = "setBlockMetadata", at = @At("RETURN"))
    private void flashback1710$afterSetBlockMetadata(int x, int y, int z, int metadata,
        CallbackInfoReturnable<Boolean> cir) {
        ReplayChunkMutationHooks.afterBlockChange(cir.getReturnValue());
    }

    @Inject(method = "fillChunk", at = @At("HEAD"))
    private void flashback1710$beforeFillChunk(
        byte[] data,
        int primaryBitMask,
        int addBitMask,
        boolean includeBiome,
        CallbackInfo ci) {
        ReplayChunkMutationHooks.beforeFillChunk((Chunk) (Object) this);
    }

    @Inject(method = "fillChunk", at = @At("RETURN"))
    private void flashback1710$afterFillChunk(
        byte[] data,
        int primaryBitMask,
        int addBitMask,
        boolean includeBiome,
        CallbackInfo ci) {
        ReplayChunkMutationHooks.afterFillChunk();
    }
}
