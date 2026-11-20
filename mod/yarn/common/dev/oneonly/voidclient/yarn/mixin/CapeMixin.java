package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.Cosmetics;
import dev.oneonly.voidclient.yarn.CapeSwap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Animowane peleryny Void: podmieniamy teksturę peleryny w danych skina gracza. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class CapeMixin {
    // 1.20.1
    @Inject(method = "getCapeTexture", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$cape(CallbackInfoReturnable<Identifier> cir) {
        try {
            String f = Cosmetics.capeFrame(((Entity) (Object) this).getUuid());
            if (f != null) cir.setReturnValue(Identifier.tryParse("voidclient:textures/cape/" + f + ".png"));
        } catch (Throwable ignored) {
        }
    }

    // 1.20.2+ (SkinTextures)
    @Inject(method = {"getSkinTextures", "getSkin"}, at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$skin(CallbackInfoReturnable<Object> cir) {
        try {
            String f = Cosmetics.capeFrame(((Entity) (Object) this).getUuid());
            if (f == null) return;
            Object n = CapeSwap.swap(cir.getReturnValue(), f);
            if (n != null) cir.setReturnValue(n);
        } catch (Throwable ignored) {
        }
    }
}
