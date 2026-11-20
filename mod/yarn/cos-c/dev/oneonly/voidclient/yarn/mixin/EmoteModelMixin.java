package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.cosmetic.Emotes;
import dev.oneonly.voidclient.yarn.Ids;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotki: po ustawieniu póz przez grę nadpisujemy kąty głowy, rąk i nóg (rękawy i nogawki są ich dziećmi). */
@Mixin(BipedEntityModel.class)
public abstract class EmoteModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"), require = 0)
    private void voidclient$emote(BipedEntityRenderState state, CallbackInfo ci) {
        try {
            if (!((Object) this instanceof PlayerEntityModel) || !(state instanceof PlayerEntityRenderState ps)) return;
            float[] r = Emotes.modelAngles(Ids.uuid(ps.id));
            if (r == null) return;
            BipedEntityModel<?> m = (BipedEntityModel<?>) (Object) this;
            set(m.head, r, 0);
            set(m.rightArm, r, 3);
            set(m.leftArm, r, 6);
            if (r[15] > 0) { set(m.rightLeg, r, 9); set(m.leftLeg, r, 12); }
        } catch (Throwable ignored) {
        }
    }

    private static void set(ModelPart p, float[] r, int i) { p.pitch = r[i]; p.yaw = r[i + 1]; p.roll = r[i + 2]; }
}
