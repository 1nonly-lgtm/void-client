package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.Cosmetics;
import dev.oneonly.voidclient.mojang.CapeSwap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Animowane peleryny Void: podmieniamy teksturę peleryny w danych skina gracza. */
@Mixin(AbstractClientPlayer.class)
public abstract class CapeMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$skin(CallbackInfoReturnable<Object> cir) {
        try {
            String f = Cosmetics.capeFrame(((Entity) (Object) this).getUUID());
            if (f == null) return;
            Object n = CapeSwap.swap(cir.getReturnValue(), f);
            if (n != null) cir.setReturnValue(n);
        } catch (Throwable ignored) {
        }
    }
}
