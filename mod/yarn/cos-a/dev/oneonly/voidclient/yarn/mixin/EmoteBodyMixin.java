package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.cosmetic.Emotes;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotki: przesunięcie i obrót całej postaci (ukłon, salto, skok). */
@Mixin(LivingEntityRenderer.class)
public abstract class EmoteBodyMixin {
    @Inject(method = "setupTransforms", at = @At("TAIL"), require = 0)
    private void voidclient$body(LivingEntity e, MatrixStack m, float a, float b, float c, CallbackInfo ci) {
        try {
            float[] t = Emotes.rootTransform(e.getUuid());
            if (t == null) return;
            m.translate(t[0], t[1], t[2]);
            m.multiply(new Quaternionf(t[3], t[4], t[5], t[6]));
        } catch (Throwable ignored) {
        }
    }
}
