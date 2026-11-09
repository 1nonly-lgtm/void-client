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
}
