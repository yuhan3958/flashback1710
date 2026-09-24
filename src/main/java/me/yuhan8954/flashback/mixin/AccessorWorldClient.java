package me.yuhan8954.flashback.mixin;

import java.util.Set;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldClient.class)
public interface AccessorWorldClient {

    @Accessor("entityList")
    Set<Entity> getReplayEntityList();
}
