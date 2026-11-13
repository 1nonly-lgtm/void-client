package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.VoidCore;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scroll podczas zoomu zmienia przybliżenie (26.1+). */
@Mixin(MouseHandler.class)
public abstract class ScrollMixin {
    @ModifyVariable(method = "onScroll", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private double voidclient$zoomScroll(double vertical) {
        return VoidCore.scroll(vertical);
    }
}
