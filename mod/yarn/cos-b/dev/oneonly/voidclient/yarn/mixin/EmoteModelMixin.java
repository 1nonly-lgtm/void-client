package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.cosmetic.Emotes;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotki: po ustawieniu póz przez grę nadpisujemy kąty głowy, rąk i nóg. */
@Mixin(PlayerEntityModel.class)
public abstract class EmoteModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void voidclient$emote(LivingEntity e, float a, float b, float c, float d, float f, CallbackInfo ci) {
        try {
            float[] r = Emotes.modelAngles(e.getUuid());
            if (r == null) return;
            PlayerEntityModel<?> m = (PlayerEntityModel<?>) (Object) this;
            set(m.head, r, 0);
            set(m.rightArm, r, 3);
            set(m.leftArm, r, 6);
            if (r[15] > 0) { set(m.rightLeg, r, 9); set(m.leftLeg, r, 12); }
            m.hat.copyTransform(m.head);
            m.leftSleeve.copyTransform(m.leftArm);
            m.rightSleeve.copyTransform(m.rightArm);
            m.leftPants.copyTransform(m.leftLeg);
            m.rightPants.copyTransform(m.rightLeg);
        } catch (Throwable ignored) {
        }
    }

    private static void set(ModelPart p, float[] r, int i) { p.pitch = r[i]; p.yaw = r[i + 1]; p.roll = r[i + 2]; }
}
