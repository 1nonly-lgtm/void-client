package dev.oneonly.voidclient.core.module;

import dev.oneonly.voidclient.core.CpsTracker;
import dev.oneonly.voidclient.core.Game;
import dev.oneonly.voidclient.core.Gamma;
import dev.oneonly.voidclient.core.Gfx;
import dev.oneonly.voidclient.core.Theme;
import dev.oneonly.voidclient.core.Tps;

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
    public static final TpsLine TPS = add(new TpsLine());
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
    public static final WaypointsModule WAYPOINTS = add(new WaypointsModule());
    public static final Effects EFFECTS = add(new Effects());
    public static final ItemCounter ITEMS = add(new ItemCounter());
    public static final Direction DIRECTION = add(new Direction());
    public static final Simple CROSSHAIR = add(new Simple("crosshair", "Własny celownik", "Kształt, kolor i rozmiar w zakładce Celownik", false));
    public static final Outline OUTLINE = add(new Outline());
    public static final Simple CHAT_TIME = add(new Simple("chattime", "Godzina na czacie", "Przy każdej wiadomości [12:34]", false));
    public static final Simple CHAT_STACK = add(new Simple("chatstack", "Łączenie powtórzeń", "Ta sama wiadomość kilka razy = jedna linijka z ×N", true));
    public static final Simple CHAT_HISTORY = add(new Simple("chathistory", "Dłuższa historia czatu", "1000 wiadomości zamiast 100", true));
    public static final Simple SCREENSHOTS = add(new Simple("screenshots", "Szybkie zrzuty", "Po F2 zrzut trafia do schowka – wklejasz go Ctrl+V", true));
    public static final Simple SHULKER = add(new Simple("shulker", "Podgląd shulkera", "Najedź na shulkera w ekwipunku, żeby zobaczyć co ma w środku", true));
    public static final MotionBlur MOTION_BLUR = add(new MotionBlur());

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

    public static final class TpsLine extends HudModule.Line {
        TpsLine() { super("tps", "TPS serwera", "Ile ticków na sekundę robi serwer (20 = bez lagów)", false, 4, 52); }
        @Override protected String label() { return "TPS "; }
        @Override protected String value(Game g) {
            double t = Tps.value();
            if (t < 0) return "-";
            return String.format(java.util.Locale.ROOT, "%.1f", t);
        }
    }

    public static final class Coords extends HudModule.Line {
        Coords() { super("coords", "Koordynaty", "Pozycja XYZ i kierunek", false, 4, 68); }
        @Override protected String label() { return "XYZ "; }
        @Override protected String value(Game g) {
            if (!g.hasPlayer()) return "-";
            return g.x() + ", " + g.y() + ", " + g.z() + "  " + g.facing();
        }
    }

    public static final class Clock extends HudModule.Line {
        Clock() { super("clock", "Zegar", "Aktualna godzina", false, 4, 84); }
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

        Keystrokes() { super("keystrokes", "Keystrokes", "Podgląd klawiszy WASD, myszy i spacji", true, 4, 104); }

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
        Armor() { super("armor", "Zbroja", "Zbroja i przedmiot w ręce z wytrzymałością", true, 4, 184); }

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

        Zoom() { super("zoom", "Zoom", "Przybliżenie pod klawiszem (domyślnie C, zmienisz w Opcjach)", Category.GAME, true); }

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
        FreelookModule() { super("freelook", "Freelook", "Trzymaj klawisz (domyślnie lewy Alt) i rozglądaj się wokół postaci", Category.GAME, true); }
    }

    public static final class WaypointsModule extends Module {
        WaypointsModule() { super("waypoints", "Waypointy", "Znaczniki w świecie – dodasz je klawiszem (domyślnie B) albo w zakładce Waypointy", Category.GAME, true); }
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

    // ───────────── nowe ─────────────

    /** Moduł bez własnej logiki – sprawdzany w innych miejscach (czat, celownik, zrzuty). */
    public static final class Simple extends Module {
        Simple(String id, String name, String description, boolean on) { super(id, name, description, Category.GAME, on); }
    }

    public static final class Outline extends Module {
        Outline() { super("outline", "Kolor obramowania", "Kolor ramki wskazanego bloku (zakładka Celownik)", Category.GAME, false); }

        @Override public boolean available() {
            try { return dev.oneonly.voidclient.core.VoidCore.game().outlineSupported(); } catch (Throwable t) { return false; }
        }

        @Override public String unavailableReason() { return "Ta wersja gry rysuje ramkę inaczej – wkrótce"; }
    }

    public static final class MotionBlur extends Module {
        MotionBlur() { super("motionblur", "Rozmycie ruchu", "Smuga przy szybkim obracaniu kamerą", Category.GAME, false); }

        @Override public boolean available() { return false; }

        @Override public String unavailableReason() { return "W przygotowaniu – pojawi się w kolejnej wersji"; }
    }

    public static final class Effects extends HudModule {
        private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
        private List<Game.Effect> list = Collections.emptyList();

        Effects() { super("effects", "Efekty mikstur", "Aktywne efekty i ile jeszcze zostało", true, 4, 280); }

        private static String time(Game.Effect e) {
            if (e.infinite()) return "∞";
            int s = e.ticks() / 20;
            return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
        }

        private static String name(Game.Effect e) {
            int a = e.amplifier();
            return e.name() + (a >= 0 && a < ROMAN.length ? ROMAN[a] : " " + (a + 1));
        }

        @Override public int width(Gfx g) {
            int w = g.width("Efekty mikstur");
            for (Game.Effect e : list) w = Math.max(w, g.width(name(e) + "  " + time(e)));
            return w + 8;
        }

        @Override public int height(Gfx g) { return Math.max(1, list.size()) * 11 + 4; }

        @Override
        public void render(Gfx g, Game game) {
            try { list = game.hasPlayer() ? game.effects() : Collections.emptyList(); } catch (Throwable t) { list = Collections.emptyList(); }
            if (list.isEmpty()) return;
            box(g, width(g), height(g));
            int yy = y + 3;
            for (Game.Effect e : list) {
                String t = time(e);
                boolean ending = !e.infinite() && e.ticks() < 200 && (System.currentTimeMillis() / 300) % 2 == 0;
                g.text(name(e), x + 4, yy, Theme.accent(), Theme.textShadow);
                g.text(t, x + width(g) - 4 - g.width(t), yy, ending ? 0xFFFF7777 : 0xFFFFFFFF, Theme.textShadow);
                yy += 11;
            }
        }
    }

    public static final class ItemCounter extends HudModule {
        private static final String[] LABELS = {"Strzały", "Perły", "Złote jabłka", "Bloki", "Leczenie"};
        private final Game.Slot[] cache = new Game.Slot[LABELS.length];
        private int shown;

        ItemCounter() { super("items", "Licznik przedmiotów", "Strzały, perły, złote jabłka, bloki i mikstury leczenia", false, 60, 184); }

        private static boolean match(int i, String s) {
            String id = s.substring(0, s.indexOf('|') < 0 ? s.length() : s.indexOf('|'));
            String name = s.toLowerCase();
            switch (i) {
                case 0: return id.endsWith("arrow");
                case 1: return id.equals("minecraft:ender_pearl");
                case 2: return id.equals("minecraft:golden_apple") || id.equals("minecraft:enchanted_golden_apple");
                default: return id.contains("potion") && (name.contains("heal") || name.contains("lecz"));
            }
        }

        @Override public int width(Gfx g) { return 18 + g.width("0000") + 6; }

        @Override public int height(Gfx g) { return Math.max(1, shown) * 18; }

        @Override
        public void render(Gfx g, Game game) {
            if (!game.hasPlayer()) return;
            shown = 0;
            for (int i = 0; i < LABELS.length; i++) {
                final int k = i;
                try { cache[i] = game.countItems(s -> match(k, s), i == 3); } catch (Throwable t) { cache[i] = Game.Slot.EMPTY; }
                if (!cache[i].empty()) shown++;
            }
            if (shown == 0) return;
            box(g, width(g), height(g));
            int row = 0;
            for (Game.Slot s : cache) {
                if (s.empty()) continue;
                int sy = y + row * 18 + 1;
                g.item(s.stack(), x + 1, sy);
                g.text(Integer.toString(s.count()), x + 20, sy + 4, s.count() <= 3 ? 0xFFFF7777 : 0xFFFFFFFF, Theme.textShadow);
                row++;
            }
        }
    }

    public static final class Direction extends HudModule {
        private static final String[] NAMES = {"Południe", "Pd-Zach.", "Zachód", "Pn-Zach.", "Północ", "Pn-Wsch.", "Wschód", "Pd-Wsch."};
        private String a = "", b = "";

        Direction() { super("direction", "Kierunek i biom", "Strona świata, kąt i biom, w którym stoisz", false, 4, 240); }

        @Override public int width(Gfx g) { return Math.max(g.width(a), g.width(b)) + 8; }

        @Override public int height(Gfx g) { return b.isEmpty() ? 14 : 25; }

        @Override
        public void render(Gfx g, Game game) {
            if (!game.hasPlayer()) return;
            float yaw = ((game.playerYaw() % 360) + 360) % 360;
            a = NAMES[Math.round(yaw / 45f) % 8] + " " + Math.round(yaw) + "°";
            try { b = game.biome(); } catch (Throwable t) { b = ""; }
            box(g, width(g), height(g));
            g.text(a, x + 4, y + 3, Theme.accent(), Theme.textShadow);
            if (!b.isEmpty()) g.text(b, x + 4, y + 14, 0xFFFFFFFF, Theme.textShadow);
        }
    }
}
