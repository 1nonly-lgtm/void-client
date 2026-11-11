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

    static void rect(DrawContext ctx, Identifier id, int x, int y, int w, int h, int texW, int texH) {
        ctx.drawTexture(id, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
    }

    /** Liczba sztuk / pasek wytrzymałości na przedmiocie. */
    static void overlay(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, net.minecraft.item.ItemStack stack, int x, int y) {
        ctx.drawItemInSlot(tr, stack, x, y);
    }

    /** Przesunięcie wszystkiego, co gra narysuje do pop() (scoreboard w innym miejscu). */
    static void push(DrawContext ctx, int dx, int dy) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(dx, dy, 0);
    }

    static void pop(DrawContext ctx) {
        ctx.getMatrices().pop();
    }
}
