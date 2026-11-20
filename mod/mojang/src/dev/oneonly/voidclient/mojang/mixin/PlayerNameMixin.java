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
            java.util.UUID id = ((Player) (Object) this).getUUID();
            Component name = cir.getReturnValue();
            if (dev.oneonly.voidclient.core.Nametag.gradientFor(id)) {
                String s = name.getString();
                net.minecraft.network.chat.MutableComponent g = Component.literal("");
                for (int i = 0; i < s.length(); i++) {
                    final int c = dev.oneonly.voidclient.core.Nametag.color(i, s.length());
                    g.append(Component.literal(String.valueOf(s.charAt(i))).withStyle(st -> st.withColor(c)));
                }
                name = g;
                cir.setReturnValue(name);
            }
            if (VoidNet.isVoid(id)) {
                cir.setReturnValue(Component.literal(VoidNet.ICON + " ").withStyle(ChatFormatting.WHITE).append(name));
            }
        } catch (Throwable ignored) {
            // zostaje zwykły nick
        }
    }
}
