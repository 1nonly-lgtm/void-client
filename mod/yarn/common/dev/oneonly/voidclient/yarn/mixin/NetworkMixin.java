package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.Tps;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TPS serwera: mierzymy odstępy między pakietami czasu świata (serwer wysyła je co 20 ticków). */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class NetworkMixin {
    // TAIL – na wątku sieci metoda kończy się wcześniej (przerzuca pakiet na wątek gry), więc liczymy tylko raz
    @Inject(method = "onWorldTimeUpdate", at = @At("TAIL"), require = 0)
    private void voidclient$time(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        Tps.onTimePacket();
    }
}
