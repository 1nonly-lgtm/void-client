package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.Nametag;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Własny nick nad głową w F5 (gra go chowa). */
@Mixin(LivingEntityRenderer.class)
public abstract class NametagMixin {
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$self(LivingEntity e, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValue() && Nametag.showSelf(e.getUuid())) cir.setReturnValue(true);
        } catch (Throwable ignored) {
        }
    }
}
