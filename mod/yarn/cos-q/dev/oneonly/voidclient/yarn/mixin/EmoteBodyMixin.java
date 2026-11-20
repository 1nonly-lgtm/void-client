package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.cosmetic.Emotes;
import dev.oneonly.voidclient.yarn.Ids;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotki: przesunięcie i obrót całej postaci (ukłon, salto, skok). */
@Mixin(LivingEntityRenderer.class)
public abstract class EmoteBodyMixin {
    @Inject(method = "setupTransforms", at = @At("TAIL"), require = 0)
    private void voidclient$body(LivingEntityRenderState s, MatrixStack m, float bodyYaw, float baseHeight, CallbackInfo ci) {
        try {
            if (!(s instanceof PlayerEntityRenderState ps)) return;
            float[] t = Emotes.rootTransform(Ids.uuid(ps.id));
            if (t == null) return;
            m.translate(t[0], t[1], t[2]);
            m.multiply(new Quaternionf(t[3], t[4], t[5], t[6]));
        } catch (Throwable ignored) {
        }
    }
}
