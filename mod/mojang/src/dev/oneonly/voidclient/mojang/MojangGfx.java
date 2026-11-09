package dev.oneonly.voidclient.mojang;

import dev.oneonly.voidclient.core.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/** Rysowanie przez GuiGraphicsExtractor (26.1+, dawniej GuiGraphics). */
public final class MojangGfx implements Gfx {
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("voidclient", "textures/gui/logo.png");
    private GuiGraphicsExtractor g;
    private static Method itemMethod;
    private static boolean itemLookupDone;
    private Font font;

    public MojangGfx set(GuiGraphicsExtractor g, Font font) {
        this.g = g;
        this.font = font;
        return this;
    }

    @Override public void fill(int x1, int y1, int x2, int y2, int argb) { g.fill(x1, y1, x2, y2, argb); }

    @Override public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) { g.fillGradient(x1, y1, x2, y2, top, bottom); }

    @Override public void text(String text, int x, int y, int argb, boolean shadow) { g.text(font, text, x, y, argb, shadow); }

    @Override public int width(String text) { return font.width(text); }

    @Override public String trim(String text, int maxWidth) { return font.plainSubstrByWidth(text, maxWidth); }

    /** Rysowanie przedmiotu – nazwa metody zmienia się między wersjami, więc szukamy jej po parametrach. */
    @Override
    public void item(Object stack, int x, int y) {
        if (!itemLookupDone) {
            itemLookupDone = true;
            for (Method m : GuiGraphicsExtractor.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0] == ItemStack.class && p[1] == int.class && p[2] == int.class) {
                    itemMethod = m;
                    break;
                }
            }
        }
        if (itemMethod == null) return;
        try {
            itemMethod.invoke(g, stack, x, y);
        } catch (ReflectiveOperationException ignored) {
            itemMethod = null;
        }
    }

    @Override public void scissor(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }

    @Override public void noScissor() { g.disableScissor(); }

    @Override public void logo(int x, int y, int size) { g.blit(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0f, 0f, size, size, 128, 128, 128, 128); }
}
