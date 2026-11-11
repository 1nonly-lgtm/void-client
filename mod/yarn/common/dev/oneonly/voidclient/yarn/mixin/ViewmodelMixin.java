package dev.oneonly.voidclient.yarn.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.oneonly.voidclient.core.View;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Viewmodel: przesuwa, obraca i skaluje przedmiot w ręce (lewa ręka – lustrzanie). */
@Mixin(HeldItemRenderer.class)
public abstract class ViewmodelMixin {
    @Unique private int voidclient$pushed;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void voidclient$vmStart(CallbackInfo ci, @Local(argsOnly = true) MatrixStack m, @Local(argsOnly = true) Hand hand) {
        if (!View.viewmodel()) return;
        float side = hand == Hand.MAIN_HAND ? 1f : -1f;
        m.push();
        voidclient$pushed++;
        m.translate(View.x * side, View.y, View.z);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(View.rotX));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(View.rotY * side));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(View.rotZ * side));
        m.scale(View.scale, View.scale, View.scale);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void voidclient$vmEnd(CallbackInfo ci, @Local(argsOnly = true) MatrixStack m) {
        if (voidclient$pushed > 0) {
            voidclient$pushed--;
            m.pop();
        }
    }
}
