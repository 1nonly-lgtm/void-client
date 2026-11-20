package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.VoidNet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Logo Void przed nickiem nad głową gracza. */
@Mixin(PlayerEntity.class)
public abstract class PlayerNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidclient$icon(CallbackInfoReturnable<Text> cir) {
        try {
            java.util.UUID id = ((PlayerEntity) (Object) this).getUuid();
            Text name = cir.getReturnValue();
            if (dev.oneonly.voidclient.core.Nametag.gradientFor(id)) {
                String s = name.getString();
                net.minecraft.text.MutableText g = Text.literal("");
                for (int i = 0; i < s.length(); i++) {
                    final int c = dev.oneonly.voidclient.core.Nametag.color(i, s.length());
                    g.append(Text.literal(String.valueOf(s.charAt(i))).styled(st -> st.withColor(c)));
                }
                name = g;
                cir.setReturnValue(name);
            }
            if (VoidNet.isVoid(id)) {
                cir.setReturnValue(Text.literal(VoidNet.ICON + " ").formatted(Formatting.WHITE).append(name));
            }
        } catch (Throwable ignored) {
            // zostaje zwykły nick
        }
    }
}
