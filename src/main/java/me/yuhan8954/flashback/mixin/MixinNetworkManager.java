package me.yuhan8954.flashback.mixin;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;
import me.yuhan8954.flashback.recording.ReplayRecorder;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {

    @Shadow
    public abstract INetHandler getNetHandler();

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;" + "Lnet/minecraft/network/Packet;)V",
        at = @At("HEAD"))
    private void flashback$recordInboundPacket(ChannelHandlerContext context, Packet packet, CallbackInfo ci) {

        if (!(this.getNetHandler() instanceof NetHandlerPlayClient)) {
            return;
        }

        ReplayRecorder.record(packet);
    }
}
