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
            if (VoidNet.isVoid(((PlayerEntity) (Object) this).getUuid())) {
                cir.setReturnValue(Text.literal(VoidNet.ICON + " ").formatted(Formatting.WHITE).append(cir.getReturnValue()));
            }
        } catch (Throwable ignored) {
            // zostaje zwykły nick
        }
    }
}
