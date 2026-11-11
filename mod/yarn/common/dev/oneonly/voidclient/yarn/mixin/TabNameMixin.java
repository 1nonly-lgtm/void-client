package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.VoidNet;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Logo Void przed nickiem na liście graczy (Tab). */
@Mixin(PlayerListHud.class)
public abstract class TabNameMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$icon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        try {
            if (VoidNet.isVoid(VoidNet.idOf(entry.getProfile()))) {
                cir.setReturnValue(Text.literal(VoidNet.ICON + " ").formatted(Formatting.WHITE).append(cir.getReturnValue()));
            }
        } catch (Throwable ignored) {
            // zostaje zwykły nick
        }
    }
}
