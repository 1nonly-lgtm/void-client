package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.VoidCore;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gdy włączony jest własny celownik, ten z gry nie jest rysowany (26.1+). */
@Mixin(Gui.class)
public abstract class CrosshairMixin {
    @Inject(method = {"extractCrosshair", "renderCrosshair"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void voidclient$crosshair(CallbackInfo ci) {
        if (VoidCore.customCrosshair()) ci.cancel();
    }
}
