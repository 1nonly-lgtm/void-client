package dev.oneonly.voidclient.yarn;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/** Rysowanie tekstur dla 1.21.2 – 1.21.5. */
final class Tex {
    static final Identifier LOGO = Identifier.tryParse("voidclient:textures/gui/logo.png");

    private Tex() {}

    static void draw(DrawContext ctx, Identifier id, int x, int y, int size, int texSize) {
        ctx.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0f, 0f, size, size, texSize, texSize, texSize, texSize);
    }

    static void rect(DrawContext ctx, Identifier id, int x, int y, int w, int h, int texW, int texH) {
        ctx.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
    }

    /** Liczba sztuk / pasek wytrzymałości na przedmiocie. */
    static void overlay(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, net.minecraft.item.ItemStack stack, int x, int y) {
        ctx.drawStackOverlay(tr, stack, x, y);
    }
}
