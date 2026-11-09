package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.LoadingArt;
import dev.oneonly.voidclient.yarn.YarnGfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tło menu w klimacie Void (gwiazdy, mgławice) zamiast panoramy/ziemi – tylko gdy nie ma wczytanego świata. */
@Mixin(Screen.class)
public abstract class MenuBgMixin {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"), require = 0)
    private void voidclient$menuBg(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.world != null) return; // w świecie zostaje zwykłe rozmyte tło gry
            Screen s = (Screen) (Object) this;
            if (s instanceof TitleScreen) return; // menu główne ma obracającą się panoramę pustki
            LoadingArt.menu(new YarnGfx().set(ctx, c.textRenderer), s.width, s.height);
        } catch (Throwable ignored) {
            // zostaje zwykłe tło
        }
    }
}
