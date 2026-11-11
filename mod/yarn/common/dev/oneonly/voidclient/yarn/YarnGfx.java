package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.Gfx;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** Rysowanie przez DrawContext (1.20 – 1.21.11). Jeden obiekt, podmieniany co klatkę – zero śmieci. */
public final class YarnGfx implements Gfx {
    private DrawContext ctx;
    private TextRenderer tr;

    public YarnGfx set(DrawContext ctx, TextRenderer tr) {
        this.ctx = ctx;
        this.tr = tr;
        return this;
    }

    @Override public void fill(int x1, int y1, int x2, int y2, int argb) { ctx.fill(x1, y1, x2, y2, argb); }

    @Override public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) { ctx.fillGradient(x1, y1, x2, y2, top, bottom); }

    @Override public void text(String text, int x, int y, int argb, boolean shadow) { ctx.drawText(tr, text, x, y, argb, shadow); }

    @Override public int width(String text) { return tr.getWidth(text); }

    @Override public String trim(String text, int maxWidth) { return tr.trimToWidth(text, maxWidth); }

    @Override public void item(Object stack, int x, int y) { ctx.drawItem((ItemStack) stack, x, y); }

    @Override public void scissor(int x1, int y1, int x2, int y2) { ctx.enableScissor(x1, y1, x2, y2); }

    @Override public void noScissor() { ctx.disableScissor(); }

    @Override public void logo(int x, int y, int size) { Tex.draw(ctx, Tex.LOGO, x, y, size, 128); }

    private static final java.util.Map<String, net.minecraft.util.Identifier> IDS = new java.util.HashMap<>();

    @Override
    public void image(String id, int x, int y, int w, int h, int texW, int texH) {
        net.minecraft.util.Identifier i = IDS.computeIfAbsent(id, net.minecraft.util.Identifier::tryParse);
        if (i != null) Tex.rect(ctx, i, x, y, w, h, texW, texH);
    }

    @Override
    public void itemWithCount(Object stack, int x, int y) {
        ctx.drawItem((ItemStack) stack, x, y);
        Tex.overlay(ctx, tr, (ItemStack) stack, x, y);
    }
}
