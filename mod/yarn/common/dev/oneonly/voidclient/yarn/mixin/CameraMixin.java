package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Freelook: kamera używa własnego kierunku, a gra sama odsuwa ją za postać. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"), index = 0, require = 0)
    private float voidclient$yaw(float yaw) {
        return Freelook.active() ? Freelook.yaw() : yaw;
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"), index = 1, require = 0)
    private float voidclient$pitch(float pitch) {
        return Freelook.active() ? Freelook.pitch() : pitch;
    }
}
