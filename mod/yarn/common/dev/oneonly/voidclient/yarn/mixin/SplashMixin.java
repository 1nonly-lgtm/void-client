package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.LoadingArt;
import dev.oneonly.voidclient.yarn.YarnGfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plansza Void Client zamiast „Mojang Studios” – rysowana na wierzchu nakładki wczytywania. */
@Mixin(SplashOverlay.class)
public abstract class SplashMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void voidclient$splash(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            LoadingArt.splash(new YarnGfx().set(ctx, MinecraftClient.getInstance().textRenderer), this, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        } catch (Throwable ignored) {
            // nigdy nie psujemy startu gry
        }
    }
}
