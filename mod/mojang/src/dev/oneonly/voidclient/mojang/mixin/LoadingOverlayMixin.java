package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.LoadingArt;
import dev.oneonly.voidclient.mojang.MojangGfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plansza Void Client zamiast „Mojang Studios” (26.1+). */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"), require = 0)
    private void voidclient$splash(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            Minecraft c = Minecraft.getInstance();
            LoadingArt.splash(new MojangGfx().set(graphics, c.font), this, c.getWindow().getGuiScaledWidth(), c.getWindow().getGuiScaledHeight());
        } catch (Throwable ignored) {
            // nigdy nie psujemy startu gry
        }
    }
}
