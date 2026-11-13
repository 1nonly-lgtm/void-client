package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.VoidCore;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scroll podczas zoomu zmienia przybliżenie zamiast przewijać pasek przedmiotów. */
@Mixin(Mouse.class)
public abstract class ScrollMixin {
    // onMouseScroll(long window, double horizontal, double vertical) – podmieniamy „vertical”
    @ModifyVariable(method = "onMouseScroll", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private double voidclient$zoomScroll(double vertical) {
        return VoidCore.scroll(vertical);
    }
}
