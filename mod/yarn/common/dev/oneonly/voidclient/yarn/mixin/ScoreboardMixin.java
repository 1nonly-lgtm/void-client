package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.module.Modules;
import dev.oneonly.voidclient.yarn.TexBridge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scoreboard w miejscu ustawionym w Edytuj HUD – przesuwamy rysowanie o różnicę od pozycji z gry. */
@Mixin(InGameHud.class)
public abstract class ScoreboardMixin {
    @Unique private boolean voidclient$moved;

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), require = 0)
    private void voidclient$before(DrawContext ctx, ScoreboardObjective objective, CallbackInfo ci) {
        int dx = Modules.SCOREBOARD.offsetX(), dy = Modules.SCOREBOARD.offsetY();
        voidclient$moved = dx != 0 || dy != 0;
        if (voidclient$moved) TexBridge.push(ctx, dx, dy);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("RETURN"), require = 0)
    private void voidclient$after(DrawContext ctx, ScoreboardObjective objective, CallbackInfo ci) {
        if (voidclient$moved) TexBridge.pop(ctx);
        voidclient$moved = false;
    }
}
