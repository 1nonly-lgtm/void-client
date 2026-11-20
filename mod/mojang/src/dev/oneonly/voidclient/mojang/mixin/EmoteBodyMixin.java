package dev.oneonly.voidclient.mojang.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.oneonly.voidclient.core.cosmetic.Emotes;
import dev.oneonly.voidclient.mojang.CosRender;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotki: przesunięcie i obrót całej postaci (ukłon, salto, skok). */
@Mixin(LivingEntityRenderer.class)
public abstract class EmoteBodyMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"), require = 0)
    private void voidclient$body(LivingEntityRenderState s, PoseStack m, float bodyRot, float scale, CallbackInfo ci) {
        try {
            if (!(s instanceof AvatarRenderState ps)) return;
            float[] t = Emotes.rootTransform(CosRender.uuid(ps.id));
            if (t == null) return;
            m.translate(t[0], t[1], t[2]);
            m.mulPose(new Quaternionf(t[3], t[4], t[5], t[6]));
        } catch (Throwable ignored) {
        }
    }
}
