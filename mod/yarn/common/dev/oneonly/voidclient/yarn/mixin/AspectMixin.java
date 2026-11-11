package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.View;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rozciągnięty obraz (np. 4:3 na monitorze 16:9) – poszerzamy projekcję w poziomie. */
@Mixin(GameRenderer.class)
public abstract class AspectMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), require = 0)
    private void voidclient$aspect(CallbackInfoReturnable<Matrix4f> cir) {
        if (View.aspect <= 0) return;
        var w = MinecraftClient.getInstance().getWindow();
        if (w.getFramebufferHeight() <= 0) return;
        float k = View.stretch(w.getFramebufferWidth() / (float) w.getFramebufferHeight());
        Matrix4f m = cir.getReturnValue();
        if (k != 1f && m != null) m.m00(m.m00() * k);
    }
}
