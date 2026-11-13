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
    public static final ArmorBar ARMOR_BAR = add(new ArmorBar());
    public static final AttackIndicator ATTACK = add(new AttackIndicator());
    public static final Scoreboard SCOREBOARD = add(new Scoreboard());
    public static final InvPreview INVENTORY = add(new InvPreview());
    public static final Simple CROSSHAIR = add(new Simple("crosshair", "Własny celownik", "Kształt, kolor i rozmiar w zakładce Celownik", false));
    public static final Outline OUTLINE = add(new Outline());
    public static final Simple CHAT_TIME = add(new Simple("chattime2", "Godzina na czacie", "Przy każdej wiadomości [12:34]", true));
    public static final Simple CHAT_STACK = add(new Simple("chatstack", "Łączenie powtórzeń", "Ta sama wiadomość kilka razy = jedna linijka z ×N", true));
    public static final Simple CHAT_HISTORY = add(new Simple("chathistory", "Dłuższa historia czatu", "1000 wiadomości zamiast 100", true));
    public static final Simple SCREENSHOTS = add(new Simple("screenshots", "Szybkie zrzuty", "Po F2 zrzut trafia do schowka – wklejasz go Ctrl+V", true));
    public static final Simple SHULKER = add(new Simple("shulker", "Podgląd shulkera", "Najedź na shulkera w ekwipunku, żeby zobaczyć co ma w środku", true));
    public static final MotionBlur MOTION_BLUR = add(new MotionBlur());
    public static final Simple VIEWMODEL = add(new Simple("viewmodel", "Viewmodel", "Położenie, rozmiar i obrót przedmiotu w ręce (zakładka Widok)", false));
    public static final Simple VOID_ICON = add(new Simple("voidicon", "Ikonka Void przy nicku", "Logo Void przy graczach, którzy też używają Void Client", true));

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
        /** Przybliżenie po wciśnięciu klawisza i granice dla scrolla (krotność). */
        private static final float START = 3f, MIN = 1.5f, MAX = 50f, STEP = 1.25f;
        private boolean active;
        private int savedFov;
        private boolean savedSmooth;
        private float target = START, current = 1f;
        private int lastFov = -1;
        private long lastFrame, scrolledAt;

        Zoom() { super("zoom", "Zoom", "Przybliżenie pod klawiszem (domyślnie C) – scrollem przybliżasz i oddalasz", Category.GAME, true); }

        /** Co tick: wciśnięcie / puszczenie klawisza. */
        public void update(Game g, boolean pressed) {
            boolean want = isEnabled() && pressed && !g.screenOpen();
            if (want && !active) {
                active = true;
                if (current <= 1.001f) { // nowy zoom (nie w trakcie wygaszania poprzedniego)
                    savedFov = g.fov();
                    savedSmooth = g.smoothCamera();
                }
                target = START;
                g.setSmoothCamera(true);
            } else if (!want && active) {
                active = false;
                g.setSmoothCamera(savedSmooth);
            }
            if (!active && current > 1.001f && g.screenOpen()) restore(g); // menu – od razu normalny widok
        }

        /** Scroll podczas zoomu. Zwraca true, gdy go zużyliśmy (wtedy pasek przedmiotów się nie przewija). */
        public boolean scroll(double amount) {
            if (!active || amount == 0) return false;
            target = amount > 0 ? target * STEP : target / STEP;
            target = Math.max(MIN, Math.min(MAX, target));
            scrolledAt = System.currentTimeMillis();
            return true;
        }

        public boolean zooming() { return active || current > 1.001f; }

        /** Co klatkę: płynne przejście do docelowego przybliżenia. */
        public void frame(Game g) {
            if (!active && current <= 1.001f) { lastFrame = 0; return; }
            long now = System.nanoTime();
            float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1e9f);
            lastFrame = now;
            float goal = active ? target : 1f;
            // interpolacja w skali logarytmicznej – każdy krok scrolla wygląda tak samo
            float lc = (float) Math.log(current), lg = (float) Math.log(goal);
            lc += (lg - lc) * Math.min(1f, dt * 14f);
            current = (float) Math.exp(lc);
            if (Math.abs(lc - lg) < 0.002f) current = goal;
            if (!active && current <= 1.001f) { restore(g); return; }
            int fov = Math.max(1, Math.round((float) Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(savedFov) / 2) / current))));
            if (fov != lastFov) { g.setFov(fov); lastFov = fov; }
        }

        /** Wyjście z gry – przywraca FOV i płynną kamerę od razu. */
        public void reset(Game g) {
            if (active) { active = false; g.setSmoothCamera(savedSmooth); }
            if (current > 1.001f || lastFov != -1) restore(g);
        }

        private void restore(Game g) {
            current = 1f;
            lastFrame = 0;
            if (lastFov != -1) { g.setFov(savedFov); lastFov = -1; }
        }

        /** Napis „Zoom 4.0×” przez chwilę po scrollu. */
        public void hud(Gfx g, int sw, int sh) {
            if (!active) return;
            long since = System.currentTimeMillis() - scrolledAt;
            if (since > 1200) return;
            float a = since < 900 ? 1f : 1f - (since - 900) / 300f;
            String t = "Zoom " + String.format(java.util.Locale.ROOT, "%.1f", target) + "×";
            int w = g.width(t) + 12, x = (sw - w) / 2, y = sh / 2 + 22;
            int alpha = (int) (a * 0xB0);
            g.fill(x, y, x + w, y + 13, alpha << 24 | 0x100820);
            g.fill(x, y + 12, x + (int) (w * Math.log(target / MIN) / Math.log(MAX / MIN)), y + 13, (int) (a * 255) << 24 | (Theme.accent() & 0xFFFFFF));
            g.text(t, x + 6, y + 3, (int) (a * 255) << 24 | 0xFFFFFF, true);
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

    /** Pasek zbroi jak pasek życia: punkty pancerza + wytrzymałość każdej części. */
    public static final class ArmorBar extends HudModule {
        ArmorBar() { super("armorbar", "Pasek zbroi", "Ile masz pancerza i jak bardzo zniszczony jest set", false, AUTO, AUTO); }

        @Override protected int[] autoPos(Gfx g, int sw, int sh) { return new int[]{sw / 2 - 91, sh - 70}; }

        @Override public int width(Gfx g) { return 92; }

        @Override public int height(Gfx g) { return 18; }

        @Override
        public void render(Gfx g, Game game) {
            if (!game.hasPlayer()) return;
            int pts;
            try { pts = game.armorPoints(); } catch (Throwable t) { pts = 0; }
            box(g, width(g), height(g));
            // 10 segmentów jak serca – każdy to 2 punkty pancerza
            for (int i = 0; i < 10; i++) {
                int sx = x + 2 + i * 9, sy = y + 2;
                g.fill(sx, sy, sx + 8, sy + 6, 0xFF1A1030);
                int fill = Math.max(0, Math.min(2, pts - i * 2));
                if (fill == 2) g.fill(sx, sy, sx + 8, sy + 6, 0xFFB8C4D6);
                else if (fill == 1) g.fill(sx, sy, sx + 4, sy + 6, 0xFFB8C4D6);
                if (fill > 0) g.fill(sx, sy, sx + (fill == 2 ? 8 : 4), sy + 2, 0xFFE8EEF8);
            }
            // wytrzymałość 4 części (hełm, napierśnik, spodnie, buty)
            for (int i = 0; i < 4; i++) {
                Game.Slot s = game.slot(i);
                int sx = x + 2 + i * 22, sy = y + 11;
                g.fill(sx, sy, sx + 20, sy + 4, 0xFF1A1030);
                if (s.empty() || !s.damageable() || s.max() <= 0) continue;
                float pct = s.left() / (float) s.max();
                int c = pct < 0.15f ? 0xFFFF5555 : pct < 0.4f ? 0xFFFFC94D : Theme.accent();
                g.fill(sx, sy, sx + Math.max(1, Math.round(20 * pct)), sy + 4, c);
            }
        }
    }

    /** Wskaźnik ataku: naładowanie ciosu i sygnał, kiedy wejdzie cios krytyczny. */
    public static final class AttackIndicator extends HudModule {
        AttackIndicator() { super("attack", "Wskaźnik ataku", "Naładowanie ciosu i KRYT, gdy uderzenie będzie krytyczne", false, AUTO, AUTO); }

        @Override protected int[] autoPos(Gfx g, int sw, int sh) { return new int[]{sw / 2 - 16, sh / 2 + 9}; }

        @Override public int width(Gfx g) { return 32; }

        @Override public int height(Gfx g) { return 14; }

        @Override
        public void render(Gfx g, Game game) {
            if (!game.hasPlayer()) return;
            float p;
            boolean crit;
            try { p = game.attackCooldown(); crit = game.critReady(); } catch (Throwable t) { return; }
            boolean ready = p >= 0.999f;
            if (p > 0.9f && crit) { // gra liczy krytyka już od 90% naładowania
                String t = "KRYT";
                int tx = x + (32 - g.width(t)) / 2;
                g.fill(x, y, x + 32, y + 12, 0x90000000);
                g.text(t, tx, y + 2, 0xFFFFD84D, true);
                return;
            }
            g.fill(x, y + 4, x + 32, y + 8, 0xA0000000);
            g.fill(x, y + 4, x + Math.max(1, Math.round(32 * Math.min(1f, p))), y + 8, ready ? Theme.accent() : 0xFFB9A6D6);
        }
    }

    /** Scoreboard z gry – w edytorze HUD przeciągasz jego ramkę, gra rysuje go w nowym miejscu. */
    public static final class Scoreboard extends HudModule {
        Scoreboard() { super("scoreboard", "Scoreboard", "Przesuń tablicę wyników serwera w Edytuj HUD", true, AUTO, AUTO); }

        private int defX, defY;

        @Override protected int[] autoPos(Gfx g, int sw, int sh) {
            defX = sw - width(g) - 1;
            defY = sh / 2 - height(g) / 2;
            return new int[]{defX, defY};
        }

        @Override public boolean available() {
            try { return dev.oneonly.voidclient.core.VoidCore.game().scoreboardMovable(); } catch (Throwable t) { return false; }
        }

        @Override public String unavailableReason() { return "Na tej wersji gry scoreboard zostaje na miejscu"; }

        @Override public int width(Gfx g) { return 110; }

        @Override public int height(Gfx g) { return 100; }

        /** O ile przesunąć scoreboard względem miejsca z gry. */
        public int offsetX() { return isEnabled() && !atAuto() ? x - defX : 0; }

        public int offsetY() { return isEnabled() && !atAuto() ? y - defY : 0; }

        @Override public void render(Gfx g, Game game) { /* rysuje gra – my tylko przesuwamy */ }
    }

    /** Podgląd plecaka (27 slotów) na ekranie – widać, co masz, bez otwierania ekwipunku. */
    public static final class InvPreview extends HudModule {
        InvPreview() { super("inventory", "Podgląd ekwipunku", "27 slotów plecaka na ekranie", false, AUTO, AUTO); }

        @Override protected int[] autoPos(Gfx g, int sw, int sh) { return new int[]{sw / 2 - 82, sh - 110}; } // nad sercami i hotbarem

        @Override public int width(Gfx g) { return 9 * 18 + 2; }

        @Override public int height(Gfx g) { return 3 * 18 + 2; }

        @Override
        public void render(Gfx g, Game game) {
            if (!game.hasPlayer()) return;
            if (Theme.hudBackground) g.fill(x, y, x + width(g), y + height(g), 0x70000000);
            for (int i = 0; i < 27; i++) {
                int sx = x + 1 + (i % 9) * 18, sy = y + 1 + (i / 9) * 18;
                g.fill(sx, sy, sx + 18, sy + 1, 0x30FFFFFF);
                g.fill(sx, sy, sx + 1, sy + 18, 0x30FFFFFF);
                Object st;
                try { st = game.invStack(9 + i); } catch (Throwable t) { st = null; }
                if (st != null) g.itemWithCount(st, sx + 1, sy + 1);
            }
        }
    }
}
