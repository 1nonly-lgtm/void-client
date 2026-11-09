package dev.oneonly.voidclient.core.module;

import dev.oneonly.voidclient.core.Game;
import dev.oneonly.voidclient.core.Gfx;
import dev.oneonly.voidclient.core.Theme;

/** Element HUD, który można przeciągać w edytorze HUD. */
public abstract class HudModule extends Module {
    public int x;
    public int y;
    public final int defaultX;
    public final int defaultY;

    protected HudModule(String id, String name, String description, boolean defaultEnabled, int x, int y) {
        super(id, name, description, Category.HUD, defaultEnabled);
        this.x = this.defaultX = x;
        this.y = this.defaultY = y;
    }

    public abstract int width(Gfx g);

    public abstract int height(Gfx g);

    public abstract void render(Gfx g, Game game);

    public boolean draggable() { return true; }

    public void clamp(Gfx g, int screenW, int screenH) {
        x = Math.max(0, Math.min(x, screenW - width(g)));
        y = Math.max(0, Math.min(y, screenH - height(g)));
    }

    protected void box(Gfx g, int w, int h) {
        if (Theme.hudBackground) g.fill(x, y, x + w, y + h, 0x90000000);
    }

    /** Pojedyncza linijka: etykieta w kolorze akcentu + wartość na biało. */
    public abstract static class Line extends HudModule {
        private String cached = "";

        protected Line(String id, String name, String description, boolean defaultEnabled, int x, int y) {
            super(id, name, description, defaultEnabled, x, y);
        }

        protected abstract String label();

        protected abstract String value(Game game);

        @Override
        public int width(Gfx g) { return g.width(label()) + g.width(cached) + 8; }

        @Override
        public int height(Gfx g) { return 14; }

        @Override
        public void render(Gfx g, Game game) {
            cached = value(game);
            box(g, width(g), 14);
            String l = label();
            g.text(l, x + 4, y + 3, Theme.accent(), Theme.textShadow);
            g.text(cached, x + 4 + g.width(l), y + 3, 0xFFFFFFFF, Theme.textShadow);
        }
    }
}
