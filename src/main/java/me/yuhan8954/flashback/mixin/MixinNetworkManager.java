package me.yuhan8954.flashback.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.yuhan8954.flashback.recording.ReplayRecorder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {

    @Inject(
        method =
            "channelRead0(Lio/netty/channel/ChannelHandlerContext;" +
                "Lnet/minecraft/network/Packet;)V",
        at = @At("HEAD")
    )
    private void flashback$recordPacket(
        ChannelHandlerContext context,
        Packet packet,
        CallbackInfo ci
    ) {
        ReplayRecorder.record(packet);
    }
}
