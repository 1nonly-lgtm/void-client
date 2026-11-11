package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

import java.util.List;

/** Siatka 9×3 z zawartością shulkera nad kursorem. */
public final class ShulkerPreview {
    private ShulkerPreview() {}

    public static void draw(Gfx g, Game game, Object stack, int mx, int my, int sw, int sh) {
        if (!Modules.SHULKER.isEnabled() || stack == null) return;
        List<Object> items;
        try { items = game.containerItems(stack); } catch (Throwable t) { return; }
        if (items == null || items.isEmpty()) return;
        int filled = 0;
        for (Object o : items) if (o != null) filled++;
        if (filled == 0) return;
        int w = 9 * 18 + 8, h = 3 * 18 + 8 + 12;
        int x = Math.min(mx + 12, sw - w - 4);
        int y = my - h - 16;
        if (y < 4) y = Math.min(my + 20, sh - h - 4);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Theme.accent(200));
        g.fill(x, y, x + w, y + h, 0xF0120A22);
        g.gradient(x, y, x + w, y + 12, 0x40A855F7, 0x00000000);
        g.text("Zawartość (" + filled + ")", x + 4, y + 3, 0xFFE9D5FF, false);
        for (int i = 0; i < 27 && i < items.size(); i++) {
            int sx = x + 4 + (i % 9) * 18, sy = y + 14 + (i / 9) * 18;
            g.fill(sx, sy, sx + 18, sy + 18, 0x30FFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x60000000);
            Object o = items.get(i);
            if (o != null) g.itemWithCount(o, sx + 1, sy + 1);
        }
    }
}
