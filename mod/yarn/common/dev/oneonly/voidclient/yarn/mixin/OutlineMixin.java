package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.Look;
import dev.oneonly.voidclient.core.module.Modules;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Kolor ramki wskazanego bloku.
 * 1.20 – 1.21.1: kolor to 4 liczby (r, g, b, a) przy drawCuboidShapeOutline.
 * 1.21.2 – 1.21.8: kolor to jeden int (ARGB) – parametr drawBlockOutline.
 */
@Mixin(WorldRenderer.class)
public abstract class OutlineMixin {
    private static final String SHAPE = "Lnet/minecraft/client/render/WorldRenderer;drawCuboidShapeOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/shape/VoxelShape;DDDFFFF)V";

    private static boolean on() { return Modules.OUTLINE.isEnabled(); }

    @ModifyArg(method = "drawBlockOutline", at = @At(value = "INVOKE", target = SHAPE), index = 6, require = 0)
    private float voidclient$r(float v) { return on() ? ((Look.outlineArgb() >> 16) & 255) / 255f : v; }

    @ModifyArg(method = "drawBlockOutline", at = @At(value = "INVOKE", target = SHAPE), index = 7, require = 0)
    private float voidclient$g(float v) { return on() ? ((Look.outlineArgb() >> 8) & 255) / 255f : v; }

    @ModifyArg(method = "drawBlockOutline", at = @At(value = "INVOKE", target = SHAPE), index = 8, require = 0)
    private float voidclient$b(float v) { return on() ? (Look.outlineArgb() & 255) / 255f : v; }

    @ModifyArg(method = "drawBlockOutline", at = @At(value = "INVOKE", target = SHAPE), index = 9, require = 0)
    private float voidclient$a(float v) { return on() ? ((Look.outlineArgb() >>> 24) & 255) / 255f : v; }

    @ModifyVariable(method = "drawBlockOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int voidclient$argb(int color) { return on() ? Look.outlineArgb() : color; }
}
