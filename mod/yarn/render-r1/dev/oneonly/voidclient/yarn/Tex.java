package dev.oneonly.voidclient.yarn;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Rysowanie tekstur dla 1.20 – 1.21.1. */
final class Tex {
    static final Identifier LOGO = Identifier.tryParse("voidclient:textures/gui/logo.png");

    private Tex() {}

    static void draw(DrawContext ctx, Identifier id, int x, int y, int size, int texSize) {
        ctx.drawTexture(id, x, y, size, size, 0f, 0f, texSize, texSize, texSize, texSize);
    }
}
