package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: ruch myszy obraca kamerę zamiast gracza. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void voidclient$freelook(double dx, double dy, CallbackInfo ci) {
        if (Freelook.active() && (Object) this == MinecraftClient.getInstance().player) {
            Freelook.turn(dx, dy);
            ci.cancel();
        }
    }
}
