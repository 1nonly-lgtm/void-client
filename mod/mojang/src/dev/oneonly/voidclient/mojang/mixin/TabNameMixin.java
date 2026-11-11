package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.VoidNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Logo Void przed nickiem na liście graczy (Tab) – 26.1+. */
@Mixin(PlayerTabOverlay.class)
public abstract class TabNameMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$icon(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        try {
            if (VoidNet.isVoid(VoidNet.idOf(info.getProfile()))) {
                cir.setReturnValue(Component.literal(VoidNet.ICON + " ").withStyle(ChatFormatting.WHITE).append(cir.getReturnValue()));
            }
        } catch (Throwable ignored) {
            // zostaje zwykły nick
        }
    }
}
