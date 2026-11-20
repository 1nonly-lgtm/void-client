package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.Nametag;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Własny nick nad głową w F5 (gra go chowa). */
@Mixin(LivingEntityRenderer.class)
public abstract class NametagMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$self(LivingEntity e, double dist, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValue() && Nametag.showSelf(e.getUUID())) cir.setReturnValue(true);
        } catch (Throwable ignored) {
        }
    }
}
