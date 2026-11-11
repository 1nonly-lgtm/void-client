package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.VoidNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Logo Void przed nickiem nad głową gracza – 26.1+. */
@Mixin(Player.class)
public abstract class PlayerNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$icon(CallbackInfoReturnable<Component> cir) {
        try {
            if (VoidNet.isVoid(((Player) (Object) this).getUUID())) {
                cir.setReturnValue(Component.literal(VoidNet.ICON + " ").withStyle(ChatFormatting.WHITE).append(cir.getReturnValue()));
            }
        } catch (Throwable ignored) {
            // zostaje zwykły nick
        }
    }
}
