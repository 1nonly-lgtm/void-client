package dev.oneonly.voidclient.mojang.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.oneonly.voidclient.core.View;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Viewmodel (26.1+): przesuwa, obraca i skaluje przedmiot w ręce. */
@Mixin(ItemInHandRenderer.class)
public abstract class ViewmodelMixin {
    @Unique private int voidclient$pushed;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void voidclient$vmStart(CallbackInfo ci, @Local(argsOnly = true) PoseStack m, @Local(argsOnly = true) InteractionHand hand) {
        if (!View.viewmodel()) return;
        float side = hand == InteractionHand.MAIN_HAND ? 1f : -1f;
        m.pushPose();
        voidclient$pushed++;
        m.translate(View.x * side, View.y, View.z);
        m.mulPose(Axis.XP.rotationDegrees(View.rotX));
        m.mulPose(Axis.YP.rotationDegrees(View.rotY * side));
        m.mulPose(Axis.ZP.rotationDegrees(View.rotZ * side));
        m.scale(View.scale, View.scale, View.scale);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void voidclient$vmEnd(CallbackInfo ci, @Local(argsOnly = true) PoseStack m) {
        if (voidclient$pushed > 0) {
            voidclient$pushed--;
            m.popPose();
        }
    }
}
