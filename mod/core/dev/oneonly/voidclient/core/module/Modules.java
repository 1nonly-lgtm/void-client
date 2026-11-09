package dev.oneonly.voidclient.core.module;

import dev.oneonly.voidclient.core.CpsTracker;
import dev.oneonly.voidclient.core.Game;
import dev.oneonly.voidclient.core.Gamma;
import dev.oneonly.voidclient.core.Gfx;
import dev.oneonly.voidclient.core.Theme;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Modules {
    private static final List<Module> ALL = new ArrayList<>();
    private static final List<HudModule> HUD = new ArrayList<>();

    public static final Fps FPS = add(new Fps());
    public static final Cps CPS = add(new Cps());
    public static final Ping PING = add(new Ping());
    public static final Coords COORDS = add(new Coords());
    public static final Clock CLOCK = add(new Clock());
    public static final Keystrokes KEYSTROKES = add(new Keystrokes());
    public static final Armor ARMOR = add(new Armor());
    public static final Watermark WATERMARK = add(new Watermark());
    public static final ToggleSprint TOGGLE_SPRINT = add(new ToggleSprint());
    public static final Zoom ZOOM = add(new Zoom());
    public static final Fullbright FULLBRIGHT = add(new Fullbright());
    public static final AutoTextModule AUTOTEXT = add(new AutoTextModule());
    public static final FreelookModule FREELOOK = add(new FreelookModule());

    private Modules() {}

    private static <T extends Module> T add(T m) {
        ALL.add(m);
        if (m instanceof HudModule) HUD.add((HudModule) m);
        return m;
    }

    public static List<Module> all() { return Collections.unmodifiableList(ALL); }

    public static List<HudModule> hud() { return Collections.unmodifiableList(HUD); }

    public static List<Module> byCategory(Module.Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) out.add(m);
        return out;
    }

    // ───────────── HUD ─────────────

    public static final class Fps extends HudModule.Line {
        Fps() { super("fps", "FPS", "Licznik klatek na sekundę", true, 4, 4); }
        @Override protected String label() { return "FPS "; }
        @Override protected String value(Game g) { return Integer.toString(g.fps()); }
    }

    public static final class Cps extends HudModule.Line {
        Cps() { super("cps", "CPS", "Kliknięcia na sekundę (LPM | PPM)", true, 4, 20); }
        @Override protected String label() { return "CPS "; }
        @Override protected String value(Game g) { return CpsTracker.left() + " | " + CpsTracker.right(); }
    }

    public static final class Ping extends HudModule.Line {
        Ping() { super("ping", "Ping", "Opóźnienie do serwera", true, 4, 36); }
        @Override protected String label() { return "Ping "; }
        @Override protected String value(Game g) {
            int p = g.ping();
            return p < 0 ? "-" : p + " ms";
        }
    }

    public static final class Coords extends HudModule.Line {
        Coords() { super("coords", "Koordynaty", "Pozycja XYZ i kierunek", false, 4, 52); }
        @Override protected String label() { return "XYZ "; }
        @Override protected String value(Game g) {
            if (!g.hasPlayer()) return "-";
            return g.x() + ", " + g.y() + ", " + g.z() + "  " + g.facing();
        }
    }

    public static final class Clock extends HudModule.Line {
        Clock() { super("clock", "Zegar", "Aktualna godzina", false, 4, 68); }
        @Override protected String label() { return "Godz. "; }
        @Override protected String value(Game g) {
            LocalTime t = LocalTime.now();
            int m = t.getMinute();
            return t.getHour() + ":" + (m < 10 ? "0" : "") + m;
        }
    }

    public static final class Keystrokes extends HudModule {
        private static final int K = 22;
        private static final int G = 2;

        Keystrokes() { super("keystrokes", "Keystrokes", "Podgląd klawiszy WASD, myszy i spacji", true, 4, 90); }

        @Override public int width(Gfx g) { return K * 3 + G * 2; }

        @Override public int height(Gfx g) { return K * 3 + G * 3 + 10; }

        @Override
        public void render(Gfx g, Game game) {
            key(g, "W", x + K + G, y, K, K, game.key(Game.Key.FORWARD));
            key(g, "A", x, y + K + G, K, K, game.key(Game.Key.LEFT));
            key(g, "S", x + K + G, y + K + G, K, K, game.key(Game.Key.BACK));
            key(g, "D", x + (K + G) * 2, y + K + G, K, K, game.key(Game.Key.RIGHT));
            int half = (width(g) - G) / 2;
            int my = y + (K + G) * 2;
            key(g, "LPM " + CpsTracker.left(), x, my, half, K, game.key(Game.Key.ATTACK));
            key(g, "PPM " + CpsTracker.right(), x + half + G, my, half, K, game.key(Game.Key.USE));
            key(g, "", x, my + K + G, width(g), 10, game.key(Game.Key.JUMP));
        }

        private void key(Gfx g, String label, int kx, int ky, int w, int h, boolean down) {
            g.fill(kx, ky, kx + w, ky + h, down ? Theme.accent(200) : (Theme.hudBackground ? 0x90000000 : 0x40000000));
            if (h == 10) {
                g.fill(kx + w / 2 - 10, ky + 4, kx + w / 2 + 10, ky + 5, down ? 0xFF000000 : 0xFFFFFFFF);
                return;
            }
            int tw = g.width(label);
            g.text(label, kx + (w - tw) / 2, ky + (h - 8) / 2, down ? 0xFF000000 : 0xFFFFFFFF, !down && Theme.textShadow);
        }
    }

    public static final class Armor extends HudModule {
        Armor() { super("armor", "Zbroja", "Zbroja i przedmiot w ręce z wytrzymałością", true, 4, 170); }

        @Override public int width(Gfx g) { return 18 + g.width("0000") + 6; }

        @Override public int height(Gfx g) { return 5 * 18; }

        @Override
        public void render(Gfx g, Game game) {
            if (!game.hasPlayer()) return;
            box(g, width(g), height(g));
            for (int i = 0; i < 5; i++) draw(g, game.slot(i), i);
        }

        private void draw(Gfx g, Game.Slot s, int row) {
            if (s.empty()) return;
            int sy = y + row * 18 + 1;
            g.item(s.stack(), x + 1, sy);
            String t;
            int color = 0xFFFFFFFF;
            if (s.damageable() && s.max() > 0) {
                t = Integer.toString(s.left());
                float pct = s.left() / (float) s.max();
                if (pct < 0.15f) color = 0xFFFF5555;
                else if (pct < 0.4f) color = 0xFFFFC94D;
            } else {
                t = s.count() > 1 ? Integer.toString(s.count()) : "";
            }
            g.text(t, x + 20, sy + 4, color, Theme.textShadow);
        }
    }

    public static final class Watermark extends HudModule {
        Watermark() { super("watermark", "Napis Void Client", "Napis w lewym dolnym rogu", true, 0, 0); }

        @Override public boolean draggable() { return false; }

        @Override public int width(Gfx g) { return 0; }

        @Override public int height(Gfx g) { return 0; }

        @Override public void render(Gfx g, Game game) {}

        public static void draw(Gfx g, int screenH) {
            String a = "Void Client ";
            g.text(a, 4, screenH - 12, Theme.accent(), true);
            g.text("by: 1nonly $", 4 + g.width(a), screenH - 12, 0xFFCFC2E6, true);
        }
    }

    // ───────────── GRA ─────────────

    public static final class ToggleSprint extends Module {
        ToggleSprint() { super("togglesprint", "Toggle Sprint", "Automatyczny sprint przy chodzeniu do przodu", Category.GAME, false); }

        @Override
        public void onTick(Game g) {
            if (g.canSprint()) g.sprint();
        }
    }

    public static final class Zoom extends Module {
        private Integer savedFov;
        private boolean savedSmooth;

        Zoom() { super("zoom", "Zoom", "Przybliżenie pod klawiszem C (zmienisz w Sterowaniu)", Category.GAME, true); }

        public void update(Game g, boolean pressed) {
            boolean want = isEnabled() && pressed && !g.screenOpen();
            if (want && savedFov == null) {
                savedFov = g.fov();
                savedSmooth = g.smoothCamera();
                g.setFov(30);
                g.setSmoothCamera(true);
            } else if (!want && savedFov != null) {
                g.setFov(savedFov);
                g.setSmoothCamera(savedSmooth);
                savedFov = null;
            }
        }
    }

    public static final class AutoTextModule extends Module {
        AutoTextModule() { super("autotext", "AutoText", "Wiadomości i komendy pod klawiszami (zakładka AutoText)", Category.GAME, true); }
    }

    public static final class FreelookModule extends Module {
        FreelookModule() { super("freelook", "Freelook", "Trzymaj lewy Alt i rozglądaj się wokół postaci", Category.GAME, true); }
    }

    public static final class Fullbright extends Module {
        private static final double BRIGHT = 15.0;
        private Double previous;
        private Game game;

        Fullbright() { super("fullbright", "Fullbright", "Pełna jasność w jaskiniach i nocą", Category.GAME, false); }

        @Override
        public void onTick(Game g) {
            game = g;
            double cur = g.gamma();
            if (cur != BRIGHT) {
                if (cur <= 1.0) previous = cur;
                Gamma.forceSet(g.gammaOption(), BRIGHT);
            }
        }

        @Override
        protected void onDisable() {
            if (game != null) game.setGamma(previous != null ? previous : 0.5);
        }
    }
}
