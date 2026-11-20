package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.cosmetic.Emotes;
import dev.oneonly.voidclient.mojang.CosRender;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotki: po ustawieniu póz przez grę nadpisujemy kąty głowy, rąk i nóg. */
@Mixin(HumanoidModel.class)
public abstract class EmoteModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"), require = 0)
    private void voidclient$emote(HumanoidRenderState state, CallbackInfo ci) {
        try {
            if (!((Object) this instanceof PlayerModel) || !(state instanceof AvatarRenderState ps)) return;
            float[] r = Emotes.modelAngles(CosRender.uuid(ps.id));
            if (r == null) return;
            HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
            set(m.head, r, 0);
            set(m.rightArm, r, 3);
            set(m.leftArm, r, 6);
            if (r[15] > 0) { set(m.rightLeg, r, 9); set(m.leftLeg, r, 12); }
        } catch (Throwable ignored) {
        }
    }

    private static void set(ModelPart p, float[] r, int i) { p.xRot = r[i]; p.yRot = r[i + 1]; p.zRot = r[i + 2]; }
}
