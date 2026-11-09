package dev.oneonly.voidclient.yarn;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Rysowanie tekstur dla 1.21.6 – 1.21.11. */
final class Tex {
    static final Identifier LOGO = Identifier.tryParse("voidclient:textures/gui/logo.png");

    private Tex() {}

    static void draw(DrawContext ctx, Identifier id, int x, int y, int size, int texSize) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, texSize, texSize, texSize, texSize);
    }
}
