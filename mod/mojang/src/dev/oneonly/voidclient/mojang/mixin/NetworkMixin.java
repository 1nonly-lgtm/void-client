package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.Tps;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TPS serwera: mierzymy odstępy między pakietami czasu świata (serwer wysyła je co 20 ticków). */
@Mixin(ClientPacketListener.class)
public abstract class NetworkMixin {
    @Inject(method = "handleSetTime", at = @At("TAIL"), require = 0)
    private void voidclient$time(ClientboundSetTimePacket packet, CallbackInfo ci) {
        Tps.onTimePacket();
    }
}
