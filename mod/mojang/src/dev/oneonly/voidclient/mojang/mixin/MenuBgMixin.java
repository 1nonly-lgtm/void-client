package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.LoadingArt;
import dev.oneonly.voidclient.mojang.MojangGfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tło menu w klimacie Void zamiast panoramy – tylko gdy nie ma wczytanego świata (26.1+). */
@Mixin(Screen.class)
public abstract class MenuBgMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"), require = 0)
    private void voidclient$menuBg(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            Minecraft c = Minecraft.getInstance();
            if (c.level != null) return;
            Screen s = (Screen) (Object) this;
            if (s instanceof TitleScreen) return; // menu główne ma obracającą się panoramę pustki
            LoadingArt.menu(new MojangGfx().set(graphics, c.font), s.width, s.height);
        } catch (Throwable ignored) {
            // zostaje zwykłe tło
        }
    }
}
