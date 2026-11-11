package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.View;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rozciągnięty obraz (26.1+). */
@Mixin(GameRenderer.class)
public abstract class AspectMixin {
    @Inject(method = "getProjectionMatrix", at = @At("RETURN"), require = 0)
    private void voidclient$aspect(CallbackInfoReturnable<Matrix4f> cir) {
        if (View.aspect <= 0) return;
        var w = Minecraft.getInstance().getWindow();
        if (w.getHeight() <= 0) return;
        float k = View.stretch(w.getWidth() / (float) w.getHeight());
        Matrix4f m = cir.getReturnValue();
        if (k != 1f && m != null) m.m00(m.m00() * k);
    }
}
