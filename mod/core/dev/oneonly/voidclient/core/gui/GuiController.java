package dev.oneonly.voidclient.core.gui;

import dev.oneonly.voidclient.core.AutoText;
import dev.oneonly.voidclient.core.Config;
import dev.oneonly.voidclient.core.Game;
import dev.oneonly.voidclient.core.Gfx;
import dev.oneonly.voidclient.core.Keybinds;
import dev.oneonly.voidclient.core.Look;
import dev.oneonly.voidclient.core.Screenshots;
import dev.oneonly.voidclient.core.Sky;
import dev.oneonly.voidclient.core.View;
import dev.oneonly.voidclient.core.Waypoints;
import dev.oneonly.voidclient.core.SkinUploader;
import dev.oneonly.voidclient.core.Theme;
import dev.oneonly.voidclient.core.VoidCore;
import dev.oneonly.voidclient.core.module.HudModule;
import dev.oneonly.voidclient.core.module.Module;
import dev.oneonly.voidclient.core.module.Modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu Void Client (prawy Shift). Cała logika i animacje są tutaj – ekran danej wersji gry
 * tylko przekazuje rysowanie i wejście. Animacje liczone są od czasu klatki, więc są tak samo
 * płynne przy 60 i 500 FPS.
 */
public final class GuiController {
    private enum Tab {
        ALL("Wszystkie", "Wszystkie moduły", new int[]{0b11011, 0b11011, 0b00000, 0b11011, 0b11011}),
        HUD("HUD", "Elementy na ekranie", new int[]{0b11111, 0b10001, 0b10001, 0b11111, 0b00100}),
        COMBAT("Walka", "PvP i informacje w walce", new int[]{0b00001, 0b00010, 0b10100, 0b01000, 0b10100}),
        GAME("Gra", "Dodatki do rozgrywki", new int[]{0b01110, 0b10101, 0b10111, 0b10001, 0b01110}),
        LOOK("Wygląd", "Niebo, celownik, widok i efekty", new int[]{0b00100, 0b01110, 0b11111, 0b01110, 0b00100}),
        WAYPOINTS("Waypointy", "Znaczniki w świecie", new int[]{0b01110, 0b11011, 0b01110, 0b00100, 0b00100}),
        AUTOTEXT("AutoText", "Wiadomości pod klawiszami", new int[]{0b11111, 0b10001, 0b11111, 0b01000, 0b10000}),
        OPTIONS("Ustawienia", "Klawisze, kolory, FPS i skin", new int[]{0b10101, 0b01110, 0b11011, 0b01110, 0b10101});

        final String label;
        final String subtitle;
        final int[] icon;

        Tab(String label, String subtitle, int[] icon) {
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon;
        }

        boolean modules() { return this == ALL || this == HUD || this == COMBAT || this == GAME; }
    }

    /** Podzakładki „Wyglądu”. */
    private static final String[] LOOK_SUBS = {"Moduły", "Niebo", "Celownik", "Widok"};
    private static final java.util.Set<String> COMBAT_IDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "cps", "keystrokes", "armor", "armorbar", "attack", "togglesprint", "effects", "items", "tntfuse", "mining"));
    private static final java.util.Set<String> LOOK_IDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "crosshair", "outline", "motionblur", "fullbright", "viewmodel", "voidicon", "watermark", "nametag", "itemphysics"));

    private static boolean inTab(Module m, Tab t) {
        switch (t) {
            case ALL: return true;
            case HUD: return m.category == Module.Category.HUD && !COMBAT_IDS.contains(m.id) && !LOOK_IDS.contains(m.id);
            case COMBAT: return COMBAT_IDS.contains(m.id);
            case GAME: return m.category == Module.Category.GAME && !COMBAT_IDS.contains(m.id) && !LOOK_IDS.contains(m.id);
            case LOOK: return LOOK_IDS.contains(m.id);
            default: return false;
        }
    }

    private static List<Module> modulesOf(Tab t) {
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.all()) if (inTab(m, t)) out.add(m);
        return out;
    }

    private static final class Hit {
        final int x1, y1, x2, y2;
        final Runnable action;
        final String tooltip;
        Runnable right;

        Hit(int x1, int y1, int x2, int y2, Runnable action, String tooltip) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.action = action; this.tooltip = tooltip;
        }

        Hit right(Runnable r) { this.right = r; return this; }

        boolean contains(double mx, double my) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    }

    private static Tab lastTab = Tab.ALL;

    // Wymiary w pikselach GUI (gra mnoży je przez skalę interfejsu).
    private static final int PW = 620;
    private static final int PH = 320;

    // Kolory jak w launcherze 2.0 (prawie czarny fiolet, jeden akcent)
    private static final int PANEL = 0xF60E0A17;
    private static final int SIDEBAR = 0xFF0A0711;
    private static final int CARD = 0xFF15111F;
    private static final int CARD_HOVER = 0xFF1C1629;
    private static final int LINE = 0x14FFFFFF;
    private static final int TEXT = 0xFFF3F0FA;
    private static final int MUTED = 0xFFA59DBB;
    private static final int DIM = 0xFF6F6787;

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Float> anims = new HashMap<>();
    private Tab tab = lastTab;
    private boolean editHud;
    private long openedAt = System.currentTimeMillis();
    private long tabChangedAt = openedAt;
    private long closingAt = -1;
    private long lastFrame = System.nanoTime();
    private float dt;
    private float alpha = 1f;
    private int scroll;
    private int maxScroll;
    /** Filtr kart: 0 – wszystkie, 1 – włączone, 2 – wyłączone. */
    private int filter;
    private static int lookSub;
    /** Moduł pokazany w panelu ustawień po prawej. */
    private static Module selected;
    private int width = 1;
    private int height = 1;
    private Gfx lastGfx;
    private int editing = -1;
    private String fpsStatus = "";
    private String search = "";
    /** Który klawisz właśnie zmieniamy: "freelook", "zoom", "waypoint" albo null. */
    private String binding;
    /** Waypoint, którego nazwę edytujemy. */
    private Waypoints.Point editingWp;
    /** Waypoint, którego współrzędne wpisujemy, i wpisywany tekst „x y z”. */
    private Waypoints.Point editingXyz;
    /** Osobne pola X, Y, Z, które pole jest aktywne i czy pierwszy znak ma zastąpić starą wartość. */
    private final String[] xyzBuf = {"", "", ""};
    private int xyzField;
    private boolean xyzFresh;
    private int xyzBad = -1;
    private static final int XYZ_MAX = 30_000_000;
    private static final String[] XYZ_NAMES = {"X", "Y", "Z"};
    /** Ustawiane przez klik w samo pole kordów – wtedy nie kończymy wpisywania. */
    private boolean keepXyz;

    private HudModule dragging;
    private int dragDx, dragDy;
    private boolean snapX, snapY;

    // ───────────── Animacje ─────────────

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static float easeOut(float t) { float u = 1 - t; return 1 - u * u * u; }

    /** Płynnie dąży do celu – stan pamiętany pod kluczem. */
    private float anim(String key, float target, float speed) {
        Float cur = anims.get(key);
        float c = cur == null ? target : cur;
        c += (target - c) * Math.min(1f, dt * speed);
        if (Math.abs(target - c) < 0.001f) c = target;
        anims.put(key, c);
        return c;
    }

    private float openProgress() {
        long now = System.currentTimeMillis();
        float k = easeOut(clamp01((now - openedAt) / 220f));
        if (closingAt >= 0) k *= 1f - easeOut(clamp01((now - closingAt) / 150f));
        return k;
    }

    /** Pojawianie się kafelków jeden po drugim po zmianie zakładki. */
    private float stagger(int i) {
        return easeOut(clamp01((System.currentTimeMillis() - tabChangedAt - i * 35L) / 240f));
    }

    private static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Kolor z uwzględnieniem przezroczystości całego menu. */
    private int a(int argb) {
        int al = (int) ((argb >>> 24) * alpha);
        return (al << 24) | (argb & 0x00FFFFFF);
    }

    private int a(int argb, float extra) {
        int al = (int) ((argb >>> 24) * alpha * extra);
        return (al << 24) | (argb & 0x00FFFFFF);
    }

    // ───────────── Rysowanie – pomocnicze ─────────────

    /** Prostokąt z zaokrąglonymi rogami (promień w pikselach GUI). */
    private void round(Gfx g, int x1, int y1, int x2, int y2, int r, int c) {
        if ((c >>> 24) == 0 || x2 <= x1 || y2 <= y1) return;
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            g.fill(x1 + in, y1 + i, x2 - in, y1 + i + 1, c);
            g.fill(x1 + in, y2 - i - 1, x2 - in, y2 - i, c);
        }
        g.fill(x1, y1 + r, x2, y2 - r, c);
    }

    private static int inset(int r, int row) {
        double d = r - row - 0.5;
        return (int) Math.round(r - Math.sqrt(r * r - d * d));
    }

    /** Obwódka z zaokrąglonymi rogami. */
    private void roundOutline(Gfx g, int x1, int y1, int x2, int y2, int r, int c) {
        if ((c >>> 24) == 0 || x2 <= x1 || y2 <= y1) return;
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) { g.border(x1, y1, x2 - x1, y2 - y1, c); return; }
        int[] in = new int[r + 1];
        for (int i = 0; i < r; i++) in[i] = inset(r, i);
        in[r] = 0;
        for (int i = 0; i < r; i++) {
            int from = in[i];
            int to = Math.max(in[i + 1] + 1, from + 1);
            g.fill(x1 + from, y1 + i, x1 + to, y1 + i + 1, c);
            g.fill(x2 - to, y1 + i, x2 - from, y1 + i + 1, c);
            g.fill(x1 + from, y2 - i - 1, x1 + to, y2 - i, c);
            g.fill(x2 - to, y2 - i - 1, x2 - from, y2 - i, c);
        }
        g.fill(x1 + in[0], y1, x2 - in[0], y1 + 1, c);
        g.fill(x1 + in[0], y2 - 1, x2 - in[0], y2, c);
        g.fill(x1, y1 + r, x1 + 1, y2 - r, c);
        g.fill(x2 - 1, y1 + r, x2, y2 - r, c);
    }

    // Cały tekst menu idzie gładką czcionką (Inter) – gra bez niej rysuje zwykłą.
    private void text(Gfx g, String s, int x, int y, int c, boolean shadow) {
        int col = a(c);
        if ((col >>> 24) < 0x10) return; // gra pokazuje zbyt przezroczysty tekst jako pełny – pomijamy
        g.uiText(s, x, y, col, false);
    }

    private void text(Gfx g, String s, int x, int y, int c, boolean shadow, float extra) {
        int col = a(c, extra);
        if ((col >>> 24) < 0x10) return;
        g.uiText(s, x, y, col, false);
    }

    private void bold(Gfx g, String s, int x, int y, int c) {
        int col = a(c);
        if ((col >>> 24) < 0x10) return;
        g.uiText(s, x, y, col, true);
    }

    private void bold(Gfx g, String s, int x, int y, int c, float extra) {
        int col = a(c, extra);
        if ((col >>> 24) < 0x10) return;
        g.uiText(s, x, y, col, true);
    }

    private static int tw(Gfx g, String s) { return g.uiWidth(s, false); }

    private static int bw(Gfx g, String s) { return g.uiWidth(s, true); }

    private static String cut(Gfx g, String s, int w) { return g.uiTrim(s, w, false); }

    /** Nagłówek sekcji. */
    private void title(Gfx g, String s, int x, int y) {
        bold(g, s, x, y, TEXT);
    }

    /** Przycina tekst do szerokości i dodaje „…”, jeśli coś ucięto. */
    private static String ellipsis(Gfx g, String s, int w) {
        if (tw(g, s) <= w) return s;
        String t = cut(g, s, Math.max(0, w - tw(g, "…")));
        return t.trim() + "…";
    }

    /** Dzieli opis na linie mieszczące się w szerokości. */
    private static List<String> wrap(Gfx g, String s, int w, int maxLines) {
        List<String> out = new ArrayList<>();
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (tw(g, test) <= w) { line.setLength(0); line.append(test); continue; }
            if (line.length() > 0) out.add(line.toString());
            line.setLength(0);
            line.append(word);
            if (out.size() == maxLines) break;
        }
        if (line.length() > 0 && out.size() < maxLines) out.add(line.toString());
        if (out.size() == maxLines && out.size() > 0) out.set(maxLines - 1, ellipsis(g, out.get(maxLines - 1), w));
        return out;
    }

    private void icon(Gfx g, int[] rows, int x, int y, int scale, int c) {
        for (int r = 0; r < rows.length; r++) {
            for (int b = 0; b < 5; b++) {
                if ((rows[r] >> (4 - b) & 1) != 0) g.fill(x + b * scale, y + r * scale, x + (b + 1) * scale, y + (r + 1) * scale, c);
            }
        }
    }

    /** Przełącznik jak w launcherze: zaokrąglony tor i okrągła gałka. */
    private void toggle(Gfx g, int x, int y, float on, float extra) {
        if (on > 0.02f) round(g, x - 2, y - 2, x + 22, y + 14, 8, a(Theme.accent((int) (50 * on)), extra));
        round(g, x, y, x + 20, y + 12, 6, a(lerpColor(0xFF2A2340, deep(), on), extra));
        int kx = x + 2 + (int) (on * 8);
        round(g, kx, y + 2, kx + 8, y + 10, 4, a(lerpColor(0xFF8A82A0, 0xFFFFFFFF, on), extra));
    }

    /** Pikselowe ikony modułów 5×5. */
    private static final Map<String, int[]> ICONS = new HashMap<>();
    static {
        ICONS.put("fps", new int[]{0b00001, 0b00011, 0b00111, 0b01111, 0b11111});
        ICONS.put("cps", new int[]{0b01110, 0b10101, 0b11111, 0b10001, 0b01110});
        ICONS.put("ping", new int[]{0b00001, 0b00001, 0b00101, 0b10101, 0b10101});
        ICONS.put("coords", new int[]{0b00100, 0b01110, 0b11011, 0b01110, 0b00100});
        ICONS.put("clock", new int[]{0b01110, 0b10101, 0b10111, 0b10001, 0b01110});
        ICONS.put("keystrokes", new int[]{0b01110, 0b01010, 0b11111, 0b10101, 0b11111});
        ICONS.put("armor", new int[]{0b11011, 0b11111, 0b11111, 0b01110, 0b01110});
        ICONS.put("watermark", new int[]{0b00100, 0b11111, 0b01110, 0b01010, 0b10001});
        ICONS.put("togglesprint", new int[]{0b10100, 0b01010, 0b00101, 0b01010, 0b10100});
        ICONS.put("zoom", new int[]{0b11100, 0b10100, 0b11100, 0b00010, 0b00001});
        ICONS.put("fullbright", new int[]{0b10101, 0b01110, 0b11111, 0b01110, 0b10101});
        ICONS.put("autotext", new int[]{0b11111, 0b10001, 0b11111, 0b01000, 0b10000});
        ICONS.put("freelook", new int[]{0b00000, 0b01110, 0b11011, 0b01110, 0b00000});
        ICONS.put("tps", new int[]{0b11111, 0b00100, 0b00100, 0b10101, 0b01110});
        ICONS.put("waypoints", new int[]{0b01110, 0b11011, 0b01110, 0b00100, 0b00100});
        ICONS.put("effects", new int[]{0b01110, 0b00100, 0b01110, 0b11111, 0b01110});
        ICONS.put("items", new int[]{0b11111, 0b10001, 0b10101, 0b10001, 0b11111});
        ICONS.put("direction", new int[]{0b00100, 0b01110, 0b10101, 0b00100, 0b00100});
        ICONS.put("crosshair", new int[]{0b00100, 0b00100, 0b11011, 0b00100, 0b00100});
        ICONS.put("outline", new int[]{0b11111, 0b10001, 0b10001, 0b10001, 0b11111});
        ICONS.put("chattime", new int[]{0b01110, 0b10101, 0b10111, 0b10001, 0b01110});
        ICONS.put("chatstack", new int[]{0b11110, 0b00000, 0b11110, 0b00000, 0b11010});
        ICONS.put("chathistory", new int[]{0b11111, 0b10001, 0b11111, 0b01000, 0b10000});
        ICONS.put("screenshots", new int[]{0b01100, 0b11111, 0b10101, 0b10001, 0b11111});
        ICONS.put("shulker", new int[]{0b11111, 0b10001, 0b11111, 0b10001, 0b11111});
        ICONS.put("inventory", new int[]{0b11111, 0b10101, 0b11111, 0b10101, 0b11111});
        ICONS.put("viewmodel", new int[]{0b00011, 0b00110, 0b01100, 0b11000, 0b10000});
        ICONS.put("voidicon", new int[]{0b01110, 0b10001, 0b10101, 0b10001, 0b01110});
        ICONS.put("armorbar", new int[]{0b11011, 0b11111, 0b11111, 0b01110, 0b00100});
        ICONS.put("attack", new int[]{0b00001, 0b00010, 0b10100, 0b01000, 0b10100});
        ICONS.put("scoreboard", new int[]{0b11111, 0b10001, 0b11101, 0b10001, 0b11111});
        ICONS.put("motionblur", new int[]{0b00111, 0b01110, 0b11100, 0b01110, 0b00111});
    }

    private static final int[] TILE_COLORS = {0xFFC084FC, 0xFFE9D5FF, 0xFFD946EF, 0xFFA78BFA, 0xFFF0ABFC, 0xFF8B5CF6, 0xFFDDD6FE};

    // ───────────── Tło ─────────────

    private static int deep() { return lerpColor(Theme.accent(), 0xFF5B21B6, 0.45f); }

    private void background(Gfx g) {
        g.gradient(0, 0, width, height, a(0xB80A0614), a(0xE6040209));
        long t = System.currentTimeMillis();
        // delikatna zorza u góry
        for (int i = 0; i < 3; i++) {
            int y0 = (int) (height * 0.12 + i * 10 + Math.sin(t / 3000.0 + i) * 6);
            g.gradient(0, y0, width, y0 + 26, a(Theme.accent(0)), a(Theme.accent(10 - i * 3)));
            g.gradient(0, y0 + 26, width, y0 + 52, a(Theme.accent(10 - i * 3)), a(Theme.accent(0)));
        }
        // gwiazdy
        for (int i = 0; i < 60; i++) {
            int sx = (int) (((i * 9301 + 49297) % 233280) / 233280f * width);
            int sy = (int) (((i * 4271 + 1013) % 100000) / 100000f * height);
            float tw = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(t / 900.0 + i * 1.7));
            g.fill(sx, sy, sx + 1, sy + 1, a(i % 4 == 0 ? 0xFFF0ABFC : 0xFFE9D5FF, 0.45f * tw));
        }
    }

    // ───────────── Główne rysowanie ─────────────

    public void render(Gfx g, int mx, int my, int w, int h) {
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        width = w;
        height = h;
        lastGfx = g;
        hits.clear();
        alpha = openProgress();

        if (dialogOnly) {
            if (dialog) renderDialog(g, mx, my);
            return;
        }
        if (editHud) {
            g.fill(0, 0, w, h, a(0x40000000));
            renderEditor(g, mx, my);
        } else {
            background(g);
            renderPanel(g, mx, my);
            if (dialog) renderDialog(g, mx, my);
        }
    }

    /** Wymiary panelu – dopasowane do okna (małe okno / duża skala GUI). */
    private int panelW() { return Math.max(320, Math.min(PW, width - 20)); }
    private int panelH() { return Math.max(210, Math.min(PH, height - 46)); }

    private static final int SIDE_W = 112;
    private static final int RIGHT_W = 150;

    private boolean showRight() { return panelW() >= 540 && tab.modules() || panelW() >= 540 && tab == Tab.LOOK && lookSub == 0; }

    private void renderPanel(Gfx g, int mx, int my) {
        int pw = panelW(), ph = panelH();
        int px = (width - pw) / 2;
        int py = (height - ph) / 2 - 6 + (int) ((1f - alpha) * 14);
        long nowMs = System.currentTimeMillis();

        // ── szklany panel ──
        round(g, px - 6, py - 6, px + pw + 6, py + ph + 6, 16, a(0x22000000));
        round(g, px - 1, py - 1, px + pw + 1, py + ph + 1, 12, a(0x1CFFFFFF));
        round(g, px, py, px + pw, py + ph, 11, a(PANEL));
        g.gradient(px + 4, py + 1, px + pw - 4, py + 40, a(0x10FFFFFF), a(0x00FFFFFF));

        // ── pasek boczny ──
        int sbx = px, sbw = SIDE_W;
        round(g, sbx + 1, py + 1, sbx + sbw, py + ph - 1, 10, a(SIDEBAR));
        g.fill(sbx + sbw - 1, py + 1, sbx + sbw, py + ph - 1, a(LINE));

        // logo + nazwa
        int lx = sbx + 12, ly = py + 13;
        float pulse = 0.5f + 0.5f * (float) Math.sin(nowMs / 520.0);
        round(g, lx - 2, ly - 2, lx + 20, ly + 20, 7, a(Theme.accent((int) (40 + 50 * pulse))));
        if (alpha > 0.55f) g.logo(lx, ly, 18);
        bold(g, "VOID", lx + 25, ly + 1, TEXT);
        text(g, "Client 2.0", lx + 25, ly + 11, DIM, false);

        // zakładki z przesuwanym podświetleniem
        Tab[] tabs = Tab.values();
        int rowH = 20, rowGap = 2, listY = ly + 32;
        float iy = anim("sideY", listY + tab.ordinal() * (rowH + rowGap), 16f);
        round(g, sbx + 7, (int) iy, sbx + sbw - 8, (int) iy + rowH, 6, a(Theme.accent(36)));
        round(g, sbx + 7, (int) iy + 5, sbx + 9, (int) iy + rowH - 5, 1, a(Theme.accent(255)));
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int ry = listY + i * (rowH + rowGap);
            boolean hover = mx >= sbx + 7 && mx < sbx + sbw - 8 && my >= ry && my < ry + rowH;
            float hv = anim("side" + i, hover && t != tab ? 1 : 0, 14f);
            if (hv > 0.01f) round(g, sbx + 7, ry, sbx + sbw - 8, ry + rowH, 6, a(0x12FFFFFF, hv));
            boolean act = t == tab;
            int col = act ? 0xFFFFFFFF : lerpColor(MUTED, TEXT, hv);
            int ix = sbx + 15 + (int) (hv * 1.5f);
            icon(g, t.icon, ix, ry + 7, 1, a(act ? Theme.accent() : col));
            text(g, t.label, ix + 11, ry + 6, col, false);
            String cnt = t.modules() || t == Tab.LOOK ? String.valueOf(modulesOf(t).size())
                    : t == Tab.WAYPOINTS && VoidCore.game().hasPlayer() ? String.valueOf(Waypoints.here(VoidCore.game()).size()) : "";
            if (!cnt.isEmpty()) text(g, cnt, sbx + sbw - 15 - tw(g, cnt), ry + 6, act ? MUTED : DIM, false);
            hits.add(new Hit(sbx + 7, ry, sbx + sbw - 8, ry + rowH, () -> switchTab(t), t.subtitle));
        }
        // karta na dole paska
        int cardY = py + ph - 34;
        if (listY + tabs.length * (rowH + rowGap) < cardY - 4) {
            round(g, sbx + 7, cardY, sbx + sbw - 8, py + ph - 8, 7, a(0xFF15101F));
            text(g, "by: 1nonly $", sbx + 14, cardY + 5, TEXT, false);
            text(g, "Prawy Shift – menu", sbx + 14, cardY + 15, DIM, false);
        }

        // ── środek ──
        boolean right = showRight();
        int cx = px + sbw + 12;
        int cxR = px + pw - (right ? RIGHT_W + 12 : 12);
        int cw = cxR - cx;
        int ty = py + 12;

        String head = tab.label;
        bold(g, head, cx, ty + 2, TEXT);
        String chip = chipText();
        if (!chip.isEmpty()) text(g, chip, cx + bw(g, head) + 8, ty + 3, DIM, false);

        // wyszukiwarka (moduły)
        boolean searchable = tab.modules() || (tab == Tab.LOOK && lookSub == 0);
        int sw = Math.min(130, cw - bw(g, head) - tw(g, chip) - 24);
        if (sw > 60 && searchable) {
            int sx = cxR - sw;
            boolean focus = !search.isEmpty();
            round(g, sx, ty - 2, cxR, ty + 15, 6, a(0xFF0B0814));
            roundOutline(g, sx, ty - 2, cxR, ty + 15, 6, a(focus ? Theme.accent(200) : LINE));
            icon(g, new int[]{0b11100, 0b10100, 0b11100, 0b00010, 0b00001}, sx + 6, ty + 4, 1, a(focus ? Theme.accent() : DIM));
            boolean blink = (nowMs / 500) % 2 == 0;
            if (search.isEmpty()) {
                text(g, ellipsis(g, "Szukaj modułu…", sw - 22), sx + 15, ty + 3, DIM, false);
            } else {
                String fit = search;
                while (tw(g, fit) > sw - 26 && fit.length() > 1) fit = fit.substring(1);
                text(g, fit, sx + 15, ty + 3, TEXT, false);
                if (blink) g.fill(sx + 16 + tw(g, fit), ty + 1, sx + 17 + tw(g, fit), ty + 12, a(TEXT));
            }
            hits.add(new Hit(sx, ty - 2, cxR, ty + 15, () -> { if (!searchable) switchTab(Tab.ALL); }, "Zacznij pisać, żeby wyszukać moduł"));
        }

        // pigułki: filtr kart albo podzakładki Wyglądu
        int pyl = ty + 22;
        String[] pills = tab.modules() ? new String[]{"Wszystkie", "Włączone", "Wyłączone"} : tab == Tab.LOOK ? LOOK_SUBS : null;
        int top;
        if (pills != null) {
            int pxl = cx;
            int cur = tab == Tab.LOOK ? lookSub : filter;
            for (int i = 0; i < pills.length; i++) {
                final int idx = i;
                int pwid = tw(g, pills[i]) + 14;
                boolean on = i == cur;
                boolean hov = mx >= pxl && mx < pxl + pwid && my >= pyl && my < pyl + 15;
                float hv = anim("pill" + i, hov && !on ? 1 : 0, 14f);
                round(g, pxl, pyl, pxl + pwid, pyl + 15, 5, a(on ? 0xFFF3F0FA : lerpColor(0x0DFFFFFF, 0x1CFFFFFF, hv)));
                if (!on) roundOutline(g, pxl, pyl, pxl + pwid, pyl + 15, 5, a(LINE));
                text(g, pills[i], pxl + 7, pyl + 4, on ? 0xFF120E1C : MUTED, false);
                hits.add(new Hit(pxl, pyl, pxl + pwid, pyl + 15, () -> {
                    if (tab == Tab.LOOK) { lookSub = idx; scroll = 0; anims.put("scroll", 0f); tabChangedAt = System.currentTimeMillis(); }
                    else { filter = idx; tabChangedAt = System.currentTimeMillis(); }
                }, null));
                pxl += pwid + 4;
            }
            top = pyl + 22;
        } else {
            g.fill(cx, ty + 20, cxR, ty + 21, a(LINE));
            top = ty + 27;
        }

        // ── treść ──
        int ch = py + ph - 10 - top;
        int sc = (int) anim("scroll", scroll, 18f);
        g.scissor(cx - 3, top, cxR + 3, top + ch);
        int contentH;
        switch (tab) {
            case OPTIONS: contentH = renderOptions(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case AUTOTEXT: contentH = renderAutoText(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case WAYPOINTS: contentH = renderWaypoints(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case LOOK:
                if (lookSub == 1) contentH = renderSky(g, mx, my, cx, top - sc, cw, top, top + ch);
                else if (lookSub == 2) contentH = renderLook(g, mx, my, cx, top - sc, cw, top, top + ch);
                else if (lookSub == 3) contentH = renderView(g, mx, my, cx, top - sc, cw, top, top + ch);
                else contentH = renderModules(g, mx, my, cx, top - sc, cw, top, top + ch, tab);
                break;
            default: contentH = renderModules(g, mx, my, cx, top - sc, cw, top, top + ch, tab);
        }
        g.noScissor();
        maxScroll = Math.max(0, contentH - ch);
        if (maxScroll > 0) {
            int barH = Math.max(16, ch * ch / Math.max(1, contentH));
            int barY = top + (int) ((ch - barH) * (sc / (float) maxScroll));
            round(g, cxR + 4, barY, cxR + 6, barY + barH, 1, a(Theme.accent(120)));
        }
        g.gradient(cx - 3, top + ch - 8, cxR + 3, top + ch, a(PANEL & 0x00FFFFFF), a(PANEL));

        // ── panel ustawień po prawej ──
        if (right) renderSettingsPanel(g, mx, my, px + pw - RIGHT_W, py, RIGHT_W, ph);

        // ── podpowiedź pod panelem ──
        String tip = tab == Tab.AUTOTEXT ? "Kliknij pole i wpisz tekst. Klawisz: Sterowanie → Void Client."
                : tab == Tab.WAYPOINTS ? "Kliknij waypoint, żeby go edytować · B w grze – nowy waypoint"
                : tab == Tab.LOOK && lookSub == 1 ? "Kliknij niebo, żeby je ustawić – gra przeładuje zasoby"
                : tab == Tab.LOOK && lookSub >= 2 ? "Kliknij wartość, żeby ją zmienić – zmiany widać od razu"
                : tab == Tab.OPTIONS ? "Kliknij klawisz, a potem wciśnij nowy (Esc – anuluj, Backspace – usuń)"
                : "Klik – włącz/wyłącz · prawy klik – ustawienia modułu · pisz, aby szukać";
        if (binding != null) tip = "Wciśnij klawisz albo przycisk myszy… (Esc – anuluj, Backspace – usuń)";
        for (Hit h : hits) if (h.tooltip != null && h.contains(mx, my)) tip = h.tooltip;
        tip = ellipsis(g, tip, pw - 20);
        int tipW = tw(g, tip) + 16, fy = py + ph + 10;
        if (fy + 14 < height) {
            round(g, (width - tipW) / 2, fy, (width + tipW) / 2, fy + 15, 5, a(0xB0100B1A));
            text(g, tip, (width - tipW) / 2 + 8, fy + 4, MUTED, false);
        }
    }

    private String chipText() {
        if (tab.modules() || (tab == Tab.LOOK && lookSub == 0)) {
            List<Module> list = modulesOf(tab);
            int on = 0;
            for (Module m : list) if (m.isEnabled()) on++;
            return on + " z " + list.size() + " włączonych";
        }
        if (tab == Tab.WAYPOINTS) {
            Game g = VoidCore.game();
            return g.hasPlayer() ? Waypoints.here(g).size() + " w tym świecie" : "";
        }
        if (tab == Tab.AUTOTEXT) {
            int used = 0;
            for (String s : AutoText.TEXTS) if (s != null && !s.trim().isEmpty()) used++;
            return used + " z " + AutoText.SLOTS + " zajętych";
        }
        return tab == Tab.LOOK ? LOOK_SUBS[lookSub] : "";
    }

    private void switchTab(Tab t) {
        if (t == tab) return;
        tab = t;
        lastTab = t;
        scroll = 0;
        filter = 0;
        anims.put("scroll", 0f);
        editing = -1;
        binding = null;
        finishWpEdit();
        if (!t.modules() && t != Tab.LOOK) search = "";
        tabChangedAt = System.currentTimeMillis();
    }

    // ───────────── Zakładki ─────────────

    private int renderModules(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom, Tab which) {
        List<Module> list = new ArrayList<>();
        String q = search.trim().toLowerCase();
        for (Module m : q.isEmpty() ? modulesOf(which) : Modules.all()) {
            if (!q.isEmpty() && !(m.name.toLowerCase().contains(q) || m.description.toLowerCase().contains(q))) continue;
            if (filter == 1 && !m.isEnabled() || filter == 2 && m.isEnabled()) continue;
            list.add(m);
        }
        if (list.isEmpty()) {
            String msg = !q.isEmpty() ? "Brak modułów dla „" + search + "”" : filter == 1 ? "Nic tu nie jest włączone" : "Wszystko włączone";
            text(g, msg, x + (w - tw(g, msg)) / 2, y + 40, MUTED, false);
            return 60;
        }
        if (selected == null || !list.contains(selected) && q.isEmpty() && filter == 0) selected = list.get(0);
        int gap = 6;
        int cols = w >= 250 ? 2 : 1;
        int cardW = (w - gap * (cols - 1)) / cols;
        int cardH = 34;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float st = stagger(i);
            int cx = x + (i % cols) * (cardW + gap);
            int cy = y + (i / cols) * (cardH + gap) + (int) ((1 - st) * 8);
            boolean ok = m.available();
            if (!ok) st *= 0.45f;
            boolean visible = cy + cardH > clipTop && cy < clipBottom;
            boolean hover = visible && mx >= cx && mx < cx + cardW && my >= cy && my < cy + cardH && my >= clipTop && my < clipBottom;
            float hv = anim("h" + m.id, hover ? 1 : 0, 14f);
            float on = anim("t" + m.id, m.isEnabled() ? 1 : 0, 12f);
            float sel = anim("s" + m.id, m == selected && showRight() ? 1 : 0, 12f);

            int bg = lerpColor(lerpColor(CARD, CARD_HOVER, hv), lerpColor(CARD, deep(), 0.16f), on);
            round(g, cx, cy, cx + cardW, cy + cardH, 7, a(bg, st));
            roundOutline(g, cx, cy, cx + cardW, cy + cardH, 7, a(lerpColor(lerpColor(LINE, Theme.accent(70), on), Theme.accent(230), Math.max(sel, hv * 0.5f)), st));

            // ikona w kafelku
            int ts = 20, tx = cx + 7, tyy = cy + 7;
            round(g, tx, tyy, tx + ts, tyy + ts, 6, a(lerpColor(0xFF221B31, lerpColor(deep(), 0xFF000000, 0.35f), on), st));
            int[] ic = ICONS.getOrDefault(m.id, Tab.ALL.icon);
            icon(g, ic, tx + 5, tyy + 5, 2, a(lerpColor(MUTED, 0xFFFFFFFF, on), st));
            int nx = tx + ts + 7, nw = cx + cardW - 34 - nx;
            text(g, ellipsis(g, m.name, nw), nx, cy + 8, TEXT, false, st);
            text(g, ellipsis(g, ok ? m.description : m.unavailableReason(), nw), nx, cy + 19, DIM, false, st);
            toggle(g, cx + cardW - 28, cy + 11, on, st);

            if (visible) {
                hits.add(new Hit(cx, Math.max(cy, clipTop), cx + cardW, Math.min(cy + cardH, clipBottom), () -> {
                    selected = m;
                    if (!m.available()) return;
                    m.toggle();
                    Config.save();
                }, m.name + " – " + (ok ? m.description : m.unavailableReason())).right(() -> selected = m));
            }
        }
        int rows = (list.size() + cols - 1) / cols;
        return rows * (cardH + gap) - gap + 4;
    }

    // ───────────── Panel ustawień modułu ─────────────

    private void renderSettingsPanel(Gfx g, int mx, int my, int x, int y, int w, int h) {
        g.fill(x, y + 10, x + 1, y + h - 10, a(LINE));
        Module m = selected;
        int ix = x + 12, iw = w - 24, yy = y + 14;
        if (m == null) {
            text(g, "Wybierz moduł", ix, yy, MUTED, false);
            return;
        }
        bold(g, ellipsis(g, m.name, iw), ix, yy, TEXT);
        yy += 12;
        for (String line : wrap(g, m.available() ? m.description : m.unavailableReason(), iw, 3)) {
            text(g, line, ix, yy, DIM, false);
            yy += 10;
        }
        yy += 6;
        yy = settingToggle(g, mx, my, ix, yy, iw, "Włączony", m.isEnabled(), () -> { if (m.available()) m.toggle(); });

        if (m instanceof HudModule) {
            HudModule hm = (HudModule) m;
            yy = settingToggle(g, mx, my, ix, yy, iw, "Tło HUD", Theme.hudBackground, () -> Theme.hudBackground = !Theme.hudBackground);
            yy = settingToggle(g, mx, my, ix, yy, iw, "Cień tekstu", Theme.textShadow, () -> Theme.textShadow = !Theme.textShadow);
            text(g, "Kolor akcentu", ix, yy + 2, MUTED, false);
            yy += 13;
            for (int i = 0; i < Theme.ACCENTS.length; i++) {
                final int idx = i;
                int sx = ix + i * 17;
                boolean on = i == Math.floorMod(Theme.accentIndex, Theme.ACCENTS.length);
                if (on) round(g, sx - 2, yy - 2, sx + 14, yy + 14, 6, a(0xFFFFFFFF));
                round(g, sx, yy, sx + 12, yy + 12, 5, a(Theme.ACCENTS[i]));
                hits.add(new Hit(sx - 2, yy - 2, sx + 14, yy + 14, () -> { Theme.accentIndex = idx; Config.save(); }, Theme.ACCENT_NAMES[i]));
            }
            yy += 20;
            // podgląd: prawdziwy element HUD na tle świata
            int ph = 34;
            if (yy + ph + 44 < y + h) {
                text(g, "Podgląd", ix, yy, MUTED, false);
                yy += 11;
                round(g, ix, yy, ix + iw, yy + ph, 6, a(0xFF0B0814));
                g.gradient(ix + 1, yy + 1, ix + iw - 1, yy + ph - 1, a(0xFF1E2B4A), a(0xFF2D4B2A));
                if (alpha > 0.8f && hm.isEnabled()) {
                    int ox = hm.x, oy = hm.y;
                    try {
                        int ew = hm.width(g), eh = hm.height(g);
                        if (ew <= iw - 8 && eh <= ph - 4) {
                            hm.x = ix + (iw - ew) / 2;
                            hm.y = yy + (ph - eh) / 2;
                            g.scissor(ix, yy, ix + iw, yy + ph);
                            hm.render(g, VoidCore.game());
                            g.noScissor();
                        } else {
                            text(g, "Za duży na podgląd", ix + 6, yy + 13, DIM, false);
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        hm.x = ox;
                        hm.y = oy;
                    }
                } else {
                    text(g, hm.isEnabled() ? "" : "Wyłączony", ix + 6, yy + 13, DIM, false);
                }
                yy += ph + 8;
            }
            if (hm.draggable()) {
                yy = settingButton(g, mx, my, ix, yy, iw, "Przesuń na ekranie", () -> { editHud = true; editing = -1; });
                settingButton(g, mx, my, ix, yy, iw, "Wróć na miejsce", () -> { hm.x = hm.defaultX; hm.y = hm.defaultY; Config.save(); });
            }
        } else {
            Runnable open = null;
            String label = "Otwórz ustawienia";
            if (m == Modules.CROSSHAIR || m == Modules.OUTLINE) open = () -> { switchTab(Tab.LOOK); lookSub = 2; };
            else if (m == Modules.VIEWMODEL) open = () -> { switchTab(Tab.LOOK); lookSub = 3; };
            else if (m == Modules.WAYPOINTS) open = () -> switchTab(Tab.WAYPOINTS);
            else if (m == Modules.AUTOTEXT) open = () -> switchTab(Tab.AUTOTEXT);
            else if (m == Modules.ZOOM || m == Modules.FREELOOK) { open = () -> switchTab(Tab.OPTIONS); label = "Zmień klawisz"; }
            if (open != null) settingButton(g, mx, my, ix, yy + 4, iw, label, open);
        }
    }

    private int settingToggle(Gfx g, int mx, int my, int x, int y, int w, String label, boolean value, Runnable action) {
        float on = anim("st" + label, value ? 1 : 0, 12f);
        text(g, label, x, y + 3, TEXT, false);
        toggle(g, x + w - 20, y + 1, on, 1f);
        hits.add(new Hit(x - 2, y - 2, x + w + 2, y + 15, () -> { action.run(); Config.save(); }, null));
        return y + 18;
    }

    private int settingButton(Gfx g, int mx, int my, int x, int y, int w, String label, Runnable action) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 17;
        float hv = anim("sb" + label, hov ? 1 : 0, 14f);
        round(g, x, y, x + w, y + 17, 5, a(lerpColor(0x12FFFFFF, Theme.accent(60), hv)));
        roundOutline(g, x, y, x + w, y + 17, 5, a(lerpColor(LINE, Theme.accent(200), hv)));
        String t = ellipsis(g, label, w - 10);
        text(g, t, x + (w - tw(g, t)) / 2, y + 5, TEXT, false);
        hits.add(new Hit(x, y, x + w, y + 17, () -> { action.run(); Config.save(); }, null));
        return y + 21;
    }

    private int renderSky(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom) {
        Game game = VoidCore.game();
        int ty = y;
        if (!Sky.rendererPresent(game)) {
            round(g, x, ty, x + w, ty + 22, 6, a(0x40B42318));
            text(g, ellipsis(g, "Brak moda Nuit – uruchom grę z launchera Void, on go zainstaluje", w - 16), x + 8, ty + 7, 0xFFFFD0D0, false);
            ty += 28;
        }
        int gap = 7;
        int cols = w >= 330 ? 3 : 2;
        int cardW = (w - gap * (cols - 1)) / cols;
        int imgH = Math.max(36, cardW * 9 / 20);
        int cardH = imgH + 30;
        int n = Sky.ALL.length + 1;
        for (int i = 0; i < n; i++) {
            Sky.Def d = i == 0 ? null : Sky.ALL[i - 1];
            String id = d == null ? "" : d.id;
            float st = stagger(i);
            int cx = x + (i % cols) * (cardW + gap);
            int cy = ty + (i / cols) * (cardH + gap) + (int) ((1 - st) * 10);
            boolean visible = cy + cardH > clipTop && cy < clipBottom;
            boolean hover = visible && mx >= cx && mx < cx + cardW && my >= cy && my < cy + cardH && my >= clipTop && my < clipBottom;
            float hv = anim("sky" + i, hover ? 1 : 0, 14f);
            float on = anim("skyOn" + i, id.equals(Sky.selected) ? 1 : 0, 12f);
            int top = cy - (int) (hv * 2);
            float glow = Math.max(on, hv * 0.8f);
            if (glow > 0.02f) round(g, cx - 2, top - 2, cx + cardW + 2, top + cardH + 2, 9, a(Theme.accent((int) (60 * glow)), st));
            round(g, cx, top, cx + cardW, top + cardH, 7, a(lerpColor(CARD, CARD_HOVER, hv), st));
            // obrazek nieba (miniatura z jara) na gradiencie – gradient widać, gdyby tekstury nie było
            int ix = cx + 4, iy = top + 4, iw = cardW - 8;
            if (d == null) {
                g.gradient(ix, iy, ix + iw, iy + imgH, a(0xFF6FA8FF, st), a(0xFFBFD9FF, st));
                g.fill(ix, iy + imgH - 8, ix + iw, iy + imgH, a(0xFF5A9E3A, st));
            } else {
                g.gradient(ix, iy, ix + iw, iy + imgH, a(d.top, st), a(d.bottom, st));
                if (st > 0.9f) g.image("voidclient:textures/gui/sky/" + d.id + ".png", ix, iy, iw, imgH, 256, 128);
            }
            roundOutline(g, ix - 1, iy - 1, ix + iw + 1, iy + imgH + 1, 3, a(lerpColor(0x20FFFFFF, Theme.accent(230), on), st));
            String name = d == null ? "Zwykłe niebo" : d.name;
            String sub = d == null ? "Gra albo Twoja paczka tekstur" : d.desc;
            text(g, ellipsis(g, name, cardW - 26), cx + 7, iy + imgH + 5, TEXT, false, st);
            text(g, ellipsis(g, sub, cardW - 12), cx + 7, iy + imgH + 15, DIM, false, st);
            if (on > 0.05f) {
                int bx = cx + cardW - 16, by = iy + imgH + 4;
                round(g, bx, by, bx + 11, by + 11, 5, a(Theme.accent(255), on * st));
                g.fill(bx + 3, by + 5, bx + 5, by + 7, a(0xFFFFFFFF, on));
                g.fill(bx + 5, by + 6, bx + 7, by + 8, a(0xFFFFFFFF, on));
                g.fill(bx + 7, by + 3, bx + 9, by + 6, a(0xFFFFFFFF, on));
            }
            if (visible) {
                hits.add(new Hit(cx, Math.max(top, clipTop), cx + cardW, Math.min(top + cardH, clipBottom),
                        () -> Sky.select(game, id), name + " – " + sub));
            }
        }
        int rows = (n + cols - 1) / cols;
        int end = ty + rows * (cardH + gap);
        text(g, ellipsis(g, "Niebo powoli obraca się w rytm dnia w grze. Działa w każdym świecie i na serwerach.", w), x, end + 2, DIM, false);
        return end + 14 - y;
    }

    private int renderView(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom) {
        int h = 22, gap = 4, row = 0;
        text(g, "VIEWMODEL – przedmiot w ręce", x + 2, y + 2, Theme.accent(), false);
        int ry = y + 14;
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Viewmodel", Modules.VIEWMODEL.isEnabled() ? "Włączony" : "Wyłączony",
                Modules.VIEWMODEL::toggle, clipTop, clipBottom, "Włącz, żeby poniższe ustawienia działały");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "W prawo / w lewo", View.fmt(View.x),
                () -> View.x = View.next(View.x, -0.5f, 0.5f, 0.1f), clipTop, clipBottom, "Od -0.5 do +0.5");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "W górę / w dół", View.fmt(View.y),
                () -> View.y = View.next(View.y, -0.5f, 0.5f, 0.1f), clipTop, clipBottom, "Od -0.5 do +0.5");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Bliżej / dalej", View.fmt(View.z),
                () -> View.z = View.next(View.z, -0.5f, 0.5f, 0.1f), clipTop, clipBottom, "Od -0.5 do +0.5");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Rozmiar", Math.round(View.scale * 100) + "%",
                () -> View.scale = View.next(View.scale, 0.5f, 1.5f, 0.1f), clipTop, clipBottom, "Od 50% do 150%");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Obrót X", View.rotX + "°",
                () -> View.rotX = View.rotX >= 45 ? -45 : View.rotX + 15, clipTop, clipBottom, "Pochylenie do przodu / do tyłu");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Obrót Y", View.rotY + "°",
                () -> View.rotY = View.rotY >= 45 ? -45 : View.rotY + 15, clipTop, clipBottom, "Obrót w lewo / w prawo");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Obrót Z", View.rotZ + "°",
                () -> View.rotZ = View.rotZ >= 45 ? -45 : View.rotZ + 15, clipTop, clipBottom, "Przechylenie na bok");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Ustawienia viewmodelu", "Resetuj",
                View::reset, clipTop, clipBottom, "Wraca do wyglądu z gry");
        int ay = ry + (h + gap) * row + 8;
        text(g, "PROPORCJE OBRAZU", x + 2, ay, Theme.accent(), false);
        boolean noAspect = View.aspect > 0 && View.aspectHits == 0 && VoidCore.game() != null && VoidCore.game().hasPlayer();
        optionRow(g, mx, my, x, ay + 12, w, h, row++, noAspect ? "Rozciągnięty obraz (nie działa w tej wersji)" : "Rozciągnięty obraz", View.ASPECTS[View.aspect],
                () -> View.aspect = (View.aspect + 1) % View.ASPECTS.length, clipTop, clipBottom,
                "Jak rozciągnięta rozdzielczość (np. 4:3) – szersze moby i bloki, bez zmiany ustawień Windowsa");
        return ay + 12 + h + gap - y;
    }

    private int renderLook(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom) {
        // podgląd: celownik na tle „świata”
        int ph = 56;
        round(g, x, y, x + w, y + ph, 7, a(0xFF0B0617));
        g.gradient(x + 2, y + 2, x + w - 2, y + ph * 2 / 3, a(0xFF3B5BA8), a(0xFF8FB4F0));
        g.fill(x + 2, y + ph * 2 / 3, x + w - 2, y + ph - 2, a(0xFF4E8A34));
        g.fill(x + 2, y + ph * 2 / 3, x + w - 2, y + ph * 2 / 3 + 2, a(0xFF6CB24A));
        int bx = x + w / 2 + 34, by = y + ph * 2 / 3 - 18;
        g.fill(bx, by, bx + 18, by + 18, a(0xFF8A6A44));
        g.border(bx - 1, by - 1, 20, 20, a(Modules.OUTLINE.isEnabled() ? Look.outlineArgb() : 0x66000000));
        if (alpha > 0.5f) Look.draw(g, x + w / 2, y + ph / 2, Modules.CROSSHAIR.isEnabled() ? Look.crossColor() : 0xFFFFFFFF);
        text(g, Modules.CROSSHAIR.isEnabled() ? "Własny celownik" : "Celownik z gry (włącz własny niżej)", x + 6, y + 5, 0xFFFFFFFF, true);

        int h = 22, gap = 4, row = 0;
        int ry = y + ph + 6;
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Własny celownik", Modules.CROSSHAIR.isEnabled() ? "Włączony" : "Wyłączony",
                Modules.CROSSHAIR::toggle, clipTop, clipBottom, "Zastępuje celownik z gry");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Kształt", Look.SHAPES[Look.shape],
                () -> Look.shape = (Look.shape + 1) % Look.SHAPES.length, clipTop, clipBottom, null);
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Kolor", Look.COLOR_NAMES[Look.color],
                () -> Look.color = (Look.color + 1) % Look.COLOR_NAMES.length, clipTop, clipBottom, null);
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Rozmiar", Integer.toString(Look.size),
                () -> Look.size = Look.size >= 10 ? 2 : Look.size + 1, clipTop, clipBottom, "Od 2 do 10");
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Kropka w środku", Look.dot ? "Tak" : "Nie",
                () -> Look.dot = !Look.dot, clipTop, clipBottom, null);
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Czarny obrys", Look.outline ? "Tak" : "Nie",
                () -> Look.outline = !Look.outline, clipTop, clipBottom, "Celownik widać na jasnym i ciemnym tle");
        boolean outOk = Modules.OUTLINE.available();
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Kolor ramki bloku",
                !outOk ? "Niedostępne" : Modules.OUTLINE.isEnabled() ? "Włączony" : "Wyłączony",
                () -> { if (Modules.OUTLINE.available()) Modules.OUTLINE.toggle(); }, clipTop, clipBottom,
                outOk ? "Ramka wokół bloku, na który patrzysz" : Modules.OUTLINE.unavailableReason());
        optionRow(g, mx, my, x, ry + (h + gap) * row, w, h, row++, "Kolor ramki", Look.OUTLINE_NAMES[Look.outlineColor],
                () -> Look.outlineColor = (Look.outlineColor + 1) % Look.OUTLINE_NAMES.length, clipTop, clipBottom, null);
        return ry + (h + gap) * row - y;
    }

    private int renderOptions(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom) {
        int h = 24;
        int gap = 5;
        int row = 0;
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Tryb FPS (grafika)", "Zastosuj", () -> {
            try {
                VoidCore.game().applyFpsPreset();
                fpsStatus = "Ustawienia pod FPS zastosowane";
            } catch (Throwable t) {
                fpsStatus = "Nie udało się zmienić ustawień grafiki";
            }
        }, clipTop, clipBottom, "Minimalne cząsteczki, bez chmur i cieni, bez vsync i limitu klatek");
        row = bindRow(g, mx, my, x, y + (h + gap) * row, w, h, row, "Klawisz Freelook (rozglądanie)", "freelook", Keybinds.freelook, clipTop, clipBottom);
        row = bindRow(g, mx, my, x, y + (h + gap) * row, w, h, row, "Klawisz Zoom", "zoom", Keybinds.zoom, clipTop, clipBottom);
        row = bindRow(g, mx, my, x, y + (h + gap) * row, w, h, row, "Klawisz: dodaj waypoint", "waypoint", Keybinds.waypoint, clipTop, clipBottom);
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Kolor akcentu", Theme.accentName(),
                () -> Theme.accentIndex = (Theme.accentIndex + 1) % Theme.ACCENTS.length, clipTop, clipBottom, null);
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Tło elementów HUD", Theme.hudBackground ? "Włączone" : "Wyłączone",
                () -> Theme.hudBackground = !Theme.hudBackground, clipTop, clipBottom, null);
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Cień tekstu", Theme.textShadow ? "Włączony" : "Wyłączony",
                () -> Theme.textShadow = !Theme.textShadow, clipTop, clipBottom, null);
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Model skina", SkinUploader.slim ? "Smukły" : "Klasyczny",
                () -> SkinUploader.slim = !SkinUploader.slim, clipTop, clipBottom, null);
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Zmień skin (PNG 64×64)", SkinUploader.busy() ? "Wysyłanie…" : "Wybierz plik",
                () -> SkinUploader.pickAndUpload(VoidCore.game()), clipTop, clipBottom, "Skin trafi na Twoje konto – zobaczysz go po ponownym wejściu na serwer");
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Zrzuty ekranu", "Otwórz folder",
                () -> Screenshots.openFolder(VoidCore.game()), clipTop, clipBottom, "Folder screenshots tego profilu");
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Pozycje HUD", "Resetuj",
                () -> { for (HudModule m : Modules.hud()) { m.x = m.defaultX; m.y = m.defaultY; } }, clipTop, clipBottom, null);
        int ty = y + (h + gap) * row + 2;
        if (!fpsStatus.isEmpty()) { text(g, cut(g, fpsStatus, w), x, ty, Theme.accent(), false); ty += 11; }
        String st = SkinUploader.status;
        if (st != null && !st.isEmpty()) { text(g, cut(g, st, w), x, ty, Theme.accent(), false); ty += 11; }
        text(g, cut(g, "Menu i AutoText: Opcje gry → Sterowanie → Void Client", w), x, ty, DIM, false);
        return ty - y + 12;
    }

    /** Wiersz z klawiszem – po kliknięciu czeka na nowy klawisz. */
    private int bindRow(Gfx g, int mx, int my, int x, int y, int w, int h, int row, String label, String id, int code, int clipTop, int clipBottom) {
        boolean waiting = id.equals(binding);
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        String value = waiting ? (blink ? "Wciśnij klawisz…" : "") : Keybinds.name(code);
        if (waiting && value.isEmpty()) value = " ";
        optionRow(g, mx, my, x, y, w, h, row, label, value, () -> {
            binding = id;
            editing = -1;
            finishWpEdit();
        }, clipTop, clipBottom, "Kliknij i wciśnij nowy klawisz albo przycisk myszy (np. boczny)");
        if (waiting) {
            float st = stagger(row);
            int yy = y + (int) ((1 - st) * 8);
            roundOutline(g, x, yy, x + w, yy + h, 6, a(Theme.accent(255)));
        }
        return row + 1;
    }

    private void setBinding(int code) {
        if ("freelook".equals(binding)) Keybinds.freelook = code;
        else if ("zoom".equals(binding)) Keybinds.zoom = code;
        else if ("waypoint".equals(binding)) Keybinds.waypoint = code;
        binding = null;
        Config.save();
    }

    // ───────────── Waypointy ─────────────

    private void startXyz(Waypoints.Point p) {
        finishWpEdit();
        finishXyz();
        editingXyz = p;
        xyzBuf[0] = String.valueOf(p.x);
        xyzBuf[1] = String.valueOf(p.y);
        xyzBuf[2] = String.valueOf(p.z);
        xyzField = 0;
        xyzFresh = true;
        xyzBad = -1;
    }

    /** Zapisuje wpisane kordy. Zwraca false (i podświetla błędne pole), gdy któreś jest puste albo za duże. */
    private boolean finishXyz() {
        if (editingXyz == null) return true;
        int[] v = new int[3];
        for (int i = 0; i < 3; i++) {
            try {
                v[i] = Integer.parseInt(xyzBuf[i].trim());
                if (Math.abs(v[i]) > XYZ_MAX) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                xyzBad = i;
                xyzField = i;
                xyzFresh = false;
                return false;
            }
        }
        editingXyz.x = v[0];
        editingXyz.y = v[1];
        editingXyz.z = v[2];
        editingXyz = null;
        xyzBad = -1;
        Config.save();
        return true;
    }

    /** Wklejone kordy (np. z F3 albo czatu): 3 liczby → x y z, 2 liczby → x z. */
    private void pasteXyz(String clip) {
        if (clip == null) return;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(clip);
        List<String> nums = new ArrayList<>();
        while (m.find() && nums.size() < 3) {
            double d = Double.parseDouble(m.group());
            nums.add(String.valueOf((int) Math.floor(Math.max(-XYZ_MAX, Math.min(XYZ_MAX, d)))));
        }
        if (nums.size() == 3) { xyzBuf[0] = nums.get(0); xyzBuf[1] = nums.get(1); xyzBuf[2] = nums.get(2); }
        else if (nums.size() == 2) { xyzBuf[0] = nums.get(0); xyzBuf[2] = nums.get(1); }
        else if (nums.size() == 1) xyzBuf[xyzField] = nums.get(0);
        else return;
        xyzFresh = false;
        xyzBad = -1;
    }

    private void finishWpEdit() {
        if (editingWp == null) return;
        if (editingWp.name == null || editingWp.name.trim().isEmpty()) editingWp.name = "Punkt";
        editingWp = null;
        Config.save();
    }

    private int renderWaypoints(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom) {
        Game game = VoidCore.game();
        int h = 24;
        int gap = 5;
        if (!game.hasPlayer()) {
            String m = "Wejdź do świata albo na serwer, żeby dodawać waypointy";
            text(g, m, x + (w - tw(g, m)) / 2, y + 40, MUTED, false);
            return 60;
        }
        // przycisk „Dodaj tutaj”
        String add = "+  Nowy waypoint";
        String hint = "klawisz: " + Keybinds.name(Keybinds.waypoint);
        String typed = "#  Wpisz kordy";
        int bw2 = tw(g, typed) + 22;
        int w1 = w - bw2 - 6;
        boolean hov = mx >= x && mx < x + w1 && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        float hv = anim("wpAdd", hov ? 1 : 0, 14f);
        round(g, x, y, x + w1, y + h, 6, a(lerpColor(0xFF6D28D9, 0xFF8B45F5, hv)));
        text(g, add, x + 10, y + 8, 0xFFFFFFFF, false);
        if (tw(g, add) + tw(g, hint) + 30 < w1) text(g, hint, x + w1 - 10 - tw(g, hint), y + 8, 0xFFE9D5FF, false);
        int x2 = x + w1 + 6;
        boolean hov2 = mx >= x2 && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        float hv2 = anim("wpAdd2", hov2 ? 1 : 0, 14f);
        round(g, x2, y, x + w, y + h, 6, a(lerpColor(CARD, CARD_HOVER, hv2)));
        roundOutline(g, x2, y, x + w, y + h, 6, a(lerpColor(0x30FFFFFF, Theme.accent(255), hv2)));
        text(g, typed, x2 + 11, y + 8, TEXT, false);
        if (y + h > clipTop && y < clipBottom) {
            hits.add(new Hit(x, Math.max(y, clipTop), x + w1, Math.min(y + h, clipBottom), () -> openDialog(null),
                    "Okno nowego waypointu z Twoją pozycją – nazwa, kolor i wygląd znacznika"));
            hits.add(new Hit(x2, Math.max(y, clipTop), x + w, Math.min(y + h, clipBottom), () -> { openDialog(null); dlgField = 1; },
                    "Nowy waypoint na własnych kordach – wpisz X, Y i Z (Tab przechodzi dalej), Enter zapisuje"));
        }

        List<Waypoints.Point> list = Waypoints.here(game);
        int ry = y + h + gap + 2;
        if (list.isEmpty()) {
            String m = "Brak waypointów w tym świecie";
            text(g, m, x + (w - tw(g, m)) / 2, ry + 14, DIM, false);
            return ry + 30 - y;
        }
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        for (int i = 0; i < list.size(); i++) {
            Waypoints.Point p = list.get(i);
            float st = stagger(i + 1);
            int yy = ry + i * (h + gap) + (int) ((1 - st) * 8);
            boolean visible = yy + h > clipTop && yy < clipBottom;
            boolean ed = p == editingWp;
            boolean rowHover = visible && mx >= x && mx < x + w && my >= yy && my < yy + h && my >= clipTop && my < clipBottom;
            float rh = anim("wr" + i, rowHover || ed ? 1 : 0, 14f);
            round(g, x, yy, x + w, yy + h, 6, a(lerpColor(CARD, CARD_HOVER, rh), st));
            roundOutline(g, x, yy, x + w, yy + h, 6, a(ed ? Theme.accent(255) : lerpColor(0x14FFFFFF, (p.argb() & 0xFFFFFF) | 0x90000000, rh), st));

            // kolor
            int c = p.argb();
            round(g, x + 6, yy + 6, x + 18, yy + 18, 3, a(p.visible ? c : 0xFF4A3D66, st));
            if (visible) hits.add(new Hit(x + 4, Math.max(yy, clipTop), x + 20, Math.min(yy + h, clipBottom), () -> openDialog(p), "Zmień kolor i wygląd"));

            // przyciski po prawej: oko i usuń
            int bx = x + w - 22;
            boolean delHover = visible && mx >= bx && mx < bx + 18 && my >= yy + 3 && my < yy + h - 3;
            round(g, bx, yy + 3, bx + 18, yy + h - 3, 5, a(lerpColor(0x14FFFFFF, 0xFFB42318, anim("wd" + i, delHover ? 1 : 0, 14f)), st));
            for (int k = 0; k < 6; k++) {
                g.fill(bx + 6 + k, yy + 9 + k, bx + 7 + k, yy + 10 + k, a(TEXT, st));
                g.fill(bx + 11 - k, yy + 9 + k, bx + 12 - k, yy + 10 + k, a(TEXT, st));
            }
            if (visible) hits.add(new Hit(bx, yy + 3, bx + 18, yy + h - 3, () -> {
                if (p == editingWp) editingWp = null;
                if (p == editingXyz) editingXyz = null;
                Waypoints.remove(p);
            }, "Usuń waypoint"));
            int ex = bx - 22;
            round(g, ex, yy + 3, ex + 18, yy + h - 3, 5, a(p.visible ? Theme.accent(170) : 0x14FFFFFF, st));
            icon(g, p.visible ? new int[]{0b00000, 0b01110, 0b11011, 0b01110, 0b00000} : new int[]{0b10001, 0b01110, 0b11111, 0b01110, 0b10001},
                    ex + 4, yy + 7, 2, a(p.visible ? 0xFFFFFFFF : DIM, st));
            if (visible) hits.add(new Hit(ex, yy + 3, ex + 18, yy + h - 3, () -> {
                p.visible = !p.visible;
                Config.save();
            }, p.visible ? "Ukryj znacznik" : "Pokaż znacznik"));

            // współrzędne i odległość
            boolean ex2 = p == editingXyz;
            int infoX;
            if (ex2) {
                // trzy osobne pola: X [   ]  Y [   ]  Z [   ]
                int boxW = tw(g, "-30000000") + 8, lblW = tw(g, "X") + 4, gapF = 6;
                int total = 3 * (lblW + boxW) + 2 * gapF;
                infoX = ex - 8 - total;
                int fx = infoX;
                for (int k = 0; k < 3; k++) {
                    final int field = k;
                    boolean act = k == xyzField, bad = k == xyzBad;
                    text(g, XYZ_NAMES[k], fx, yy + 8, bad ? 0xFFF87171 : act ? Theme.accent(255) : MUTED, false, st);
                    int bx1 = fx + lblW, bx2 = bx1 + boxW;
                    round(g, bx1, yy + 4, bx2, yy + h - 4, 4, a(0xFF0B0716, st));
                    roundOutline(g, bx1, yy + 4, bx2, yy + h - 4, 4, a(bad ? 0xFFF87171 : act ? Theme.accent(255) : 0x30FFFFFF, st));
                    String val = xyzBuf[k];
                    if (act && xyzFresh && !val.isEmpty()) g.fill(bx1 + 3, yy + 6, bx1 + 5 + tw(g, val), yy + h - 6, a(Theme.accent(90), st));
                    text(g, val, bx1 + 4, yy + 8, bad ? 0xFFFCA5A5 : TEXT, false, st);
                    if (act && blink) g.fill(bx1 + 4 + tw(g, val) + 1, yy + 6, bx1 + 5 + tw(g, val) + 1, yy + 17, a(TEXT));
                    if (visible) hits.add(new Hit(fx, Math.max(yy, clipTop), bx2, Math.min(yy + h, clipBottom), () -> {
                        xyzField = field;
                        xyzFresh = true;
                        keepXyz = true;
                    }, "Wpisz " + XYZ_NAMES[k] + " · Tab – następne pole · Enter – zapisz · Ctrl+V – wklej z F3"));
                    fx = bx2 + gapF;
                }
            } else {
                String dist = "  ·  " + Waypoints.distance(game, p) + " m";
                String[] parts = {"X " + p.x, "Y " + p.y, "Z " + p.z};
                int fieldW = tw(g, parts[0] + "  " + parts[1] + "  " + parts[2]);
                infoX = ex - 8 - fieldW - tw(g, dist);
                boolean xh = visible && mx >= infoX - 2 && mx < infoX + fieldW + 2 && my >= yy && my < yy + h;
                int tx = infoX;
                for (int k = 0; k < 3; k++) {
                    text(g, XYZ_NAMES[k], tx, yy + 8, xh ? Theme.accent(255) : DIM, false, st);
                    String num = String.valueOf(k == 0 ? p.x : k == 1 ? p.y : p.z);
                    text(g, num, tx + tw(g, "X "), yy + 8, xh ? TEXT : MUTED, false, st);
                    tx += tw(g, parts[k] + "  ");
                }
                if (xh) g.fill(infoX, yy + 18, infoX + fieldW, yy + 19, a(Theme.accent(160), st));
                text(g, dist, infoX + fieldW, yy + 8, MUTED, false, st);
                if (visible) hits.add(new Hit(infoX - 2, Math.max(yy, clipTop), infoX + fieldW + 2, Math.min(yy + h, clipBottom), () -> {
                    openDialog(p);
                    dlgField = 1;
                }, "Kliknij, żeby zmienić kordy"));
            }

            // nazwa (klik = edycja)
            int nx = x + 26;
            int nw = infoX - 8 - nx;
            String name = p.name == null ? "" : p.name;
            String fit = name;
            while (tw(g, fit) > nw - 4 && fit.length() > 1) fit = ed ? fit.substring(1) : fit.substring(0, fit.length() - 1);
            text(g, fit, nx, yy + 8, TEXT, false, st);
            if (ed && blink) g.fill(nx + tw(g, fit) + 1, yy + 6, nx + tw(g, fit) + 2, yy + 17, a(TEXT));
            if (visible) hits.add(new Hit(nx - 2, Math.max(yy, clipTop), infoX - 4, Math.min(yy + h, clipBottom), () -> openDialog(p),
                    "Kliknij, żeby edytować: nazwa, kordy, kolor i wygląd znacznika"));
        }
        int end = ry + list.size() * (h + gap) + 2;
        String note = Modules.WAYPOINTS.isEnabled() ? "Znaczniki pokazują się na ekranie z nazwą i odległością"
                : "Moduł Waypointy jest wyłączony (zakładka Gra) – znaczniki są ukryte";
        text(g, cut(g, note, w), x, end, DIM, false);
        return end - y + 12;
    }

    private void optionRow(Gfx g, int mx, int my, int x, int y, int w, int h, int idx, String label, String value,
                           Runnable action, int clipTop, int clipBottom, String tip) {
        float st = stagger(idx);
        y += (int) ((1 - st) * 8);
        boolean visible = y + h > clipTop && y < clipBottom;
        boolean hover = visible && mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        float hv = anim("o" + idx, hover ? 1 : 0, 14f);
        round(g, x, y, x + w, y + h, 7, a(lerpColor(CARD, CARD_HOVER, hv), st));
        roundOutline(g, x, y, x + w, y + h, 7, a(lerpColor(LINE, Theme.accent(150), hv), st));
        text(g, label, x + 10, y + (h - 8) / 2, TEXT, false, st);
        boolean onOff = value.equals("Włączony") || value.equals("Włączone") || value.equals("Wyłączony") || value.equals("Wyłączone")
                || value.equals("Tak") || value.equals("Nie");
        if (onOff) {
            boolean on = value.startsWith("Włącz") || value.equals("Tak");
            toggle(g, x + w - 30, y + (h - 12) / 2, anim("ot" + label, on ? 1 : 0, 12f), st);
        } else {
            int vw = tw(g, value) + 16;
            int vx = x + w - vw - 6;
            round(g, vx, y + 4, x + w - 6, y + h - 4, 5, a(lerpColor(0x14FFFFFF, Theme.accent(70), hv), st));
            roundOutline(g, vx, y + 4, x + w - 6, y + h - 4, 5, a(lerpColor(Theme.accent(60), Theme.accent(200), hv), st));
            text(g, value, vx + 8, y + (h - 8) / 2, lerpColor(Theme.accent(), 0xFFFFFFFF, hv), false, st);
        }
        if (visible) {
            hits.add(new Hit(x, Math.max(y, clipTop), x + w, Math.min(y + h, clipBottom), () -> {
                action.run();
                Config.save();
            }, tip));
        }
    }

    private int renderAutoText(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom) {
        int h = 24;
        int gap = 5;
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        for (int i = 0; i < AutoText.SLOTS; i++) {
            final int slot = i;
            float st = stagger(i);
            int ry = y + i * (h + gap) + (int) ((1 - st) * 8);
            boolean visible = ry + h > clipTop && ry < clipBottom;
            boolean on = editing == i;
            float ed = anim("e" + i, on ? 1 : 0, 16f);
            String key = VoidCore.game().autoTextKey(i);
            int keyW = Math.max(46, tw(g, key) + 12);
            int boxX = x + 20;
            int boxW = w - 20 - keyW - 4;

            round(g, x, ry, x + 16, ry + h, 5, a(lerpColor(Theme.accent(70), Theme.accent(220), ed), st));
            text(g, Integer.toString(i + 1), x + 6, ry + 8, TEXT, false, st);

            boolean hover = visible && mx >= boxX && mx < boxX + boxW && my >= ry && my < ry + h && my >= clipTop && my < clipBottom;
            float hv = anim("ah" + i, hover ? 1 : 0, 14f);
            round(g, boxX, ry, boxX + boxW, ry + h, 6, a(lerpColor(lerpColor(CARD, CARD_HOVER, hv), 0xFF2A1A4A, ed), st));
            roundOutline(g, boxX, ry, boxX + boxW, ry + h, 6, a(ed > 0.02f ? Theme.accent((int) (ed * 255)) : 0x14FFFFFF, st));

            String t = AutoText.TEXTS[i];
            boolean placeholder = t.isEmpty() && !on;
            String fit = placeholder ? "np. /home albo gg wp" : t;
            while (tw(g, fit) > boxW - 14 && fit.length() > 1) fit = fit.substring(1);
            text(g, fit, boxX + 6, ry + 8, placeholder ? DIM : TEXT, false, st);
            if (on && blink) {
                int cx = boxX + 6 + (placeholder ? 0 : tw(g, fit)) + 1;
                g.fill(cx, ry + 6, cx + 1, ry + 17, a(TEXT));
            }

            round(g, x + w - keyW, ry, x + w, ry + h, 6, a(CARD, st));
            boolean bound = !key.equals("-");
            text(g, cut(g, bound ? key : "brak klawisza", keyW - 10), x + w - keyW + 6, ry + 8, bound ? Theme.accent() : DIM, false, st);

            if (visible) {
                hits.add(new Hit(boxX, Math.max(ry, clipTop), boxX + boxW, Math.min(ry + h, clipBottom), () -> editing = slot,
                        "Klawisz dla slotu " + (slot + 1) + ": Sterowanie → Void Client → AutoText " + (slot + 1)));
            }
        }
        int endY = y + AutoText.SLOTS * (h + gap) + 2;
        text(g, cut(g, Modules.AUTOTEXT.isEnabled() ? "Enter – zapisz · Ctrl+V – wklej · Ctrl+Backspace – wyczyść"
                : "Moduł AutoText jest wyłączony (zakładka Gra)", w), x, endY, DIM, false);
        return endY - y + 12;
    }

    // ───────────── Okno waypointu (B albo klik w waypoint na liście) ─────────────

    private static boolean pendingDialog;
    private static Waypoints.Point pendingWp;

    /** Wywoływane z VoidCore (klawisz B) – następny otwarty ekran Void Client pokaże samo okno waypointu. */
    public static void requestWaypointDialog(Waypoints.Point p) {
        pendingWp = p;
        pendingDialog = true;
    }

    public GuiController() {
        if (pendingDialog) {
            pendingDialog = false;
            dialogOnly = true;
            openDialog(pendingWp);
            pendingWp = null;
        }
    }

    private boolean dialog, dialogOnly;
    private long dialogAt;
    private Waypoints.Point dlgPoint;
    private final String[] dlg = {"", "", "", ""}; // nazwa, x, y, z
    private int dlgField, dlgColor, dlgStyle, dlgBad = -1;
    private boolean dlgFresh, dlgDist = true;
    private static final String[] DLG_LABELS = {"Nazwa", "X", "Y", "Z"};

    private void openDialog(Waypoints.Point p) {
        Game game = VoidCore.game();
        finishWpEdit();
        editingXyz = null;
        dlgPoint = p;
        if (p == null) {
            int n = game.hasPlayer() ? Waypoints.here(game).size() + 1 : 1;
            dlg[0] = "Punkt " + n;
            dlg[1] = String.valueOf(game.x());
            dlg[2] = String.valueOf(game.y());
            dlg[3] = String.valueOf(game.z());
            dlgColor = (n - 1) % Waypoints.COLORS.length;
            dlgStyle = 1;
            dlgDist = true;
        } else {
            dlg[0] = p.name == null ? "" : p.name;
            dlg[1] = String.valueOf(p.x);
            dlg[2] = String.valueOf(p.y);
            dlg[3] = String.valueOf(p.z);
            dlgColor = Math.floorMod(p.color, Waypoints.COLORS.length);
            dlgStyle = p.style;
            dlgDist = p.showDistance;
        }
        dlgField = 0;
        dlgFresh = true;
        dlgBad = -1;
        dialog = true;
        dialogAt = System.currentTimeMillis();
    }

    private void closeDialog() {
        dialog = false;
        if (dialogOnly) requestClose();
    }

    private void saveDialog() {
        int[] v = new int[3];
        for (int i = 0; i < 3; i++) {
            try {
                v[i] = Integer.parseInt(dlg[i + 1].trim());
                if (Math.abs(v[i]) > XYZ_MAX) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                dlgBad = i + 1;
                dlgField = i + 1;
                dlgFresh = false;
                return;
            }
        }
        String name = dlg[0].trim().isEmpty() ? "Punkt" : dlg[0].trim();
        Game game = VoidCore.game();
        if (dlgPoint == null) {
            Waypoints.add(game, name, v[0], v[1], v[2], dlgColor, dlgStyle, dlgDist);
            if (!Modules.WAYPOINTS.isEnabled()) Modules.WAYPOINTS.setEnabled(true);
        } else {
            dlgPoint.name = name;
            dlgPoint.x = v[0];
            dlgPoint.y = v[1];
            dlgPoint.z = v[2];
            dlgPoint.color = dlgColor;
            dlgPoint.style = dlgStyle;
            dlgPoint.showDistance = dlgDist;
        }
        Config.save();
        closeDialog();
    }

    private static String dimensionName(Game g) {
        String d;
        try { d = g.dimension(); } catch (Throwable t) { d = ""; }
        if (d == null) d = "";
        if (d.contains("nether")) return "Nether";
        if (d.contains("end")) return "End";
        if (d.contains("overworld")) return "Overworld";
        return d.isEmpty() ? "ten świat" : d;
    }

    private void renderDialog(Gfx g, int mx, int my) {
        hits.clear(); // pod oknem nic nie da się kliknąć
        float k = easeOut(clamp01((System.currentTimeMillis() - dialogAt) / 180f));
        g.fill(0, 0, width, height, a(dialogOnly ? 0x70000000 : 0x90050310, k));
        int w = Math.min(290, width - 20), h = 222;
        int x = (width - w) / 2, y = (height - h) / 2 + (int) ((1 - k) * 10);
        long nowMs = System.currentTimeMillis();
        boolean blink = (nowMs / 500) % 2 == 0;

        round(g, x - 1, y - 1, x + w + 1, y + h + 1, 12, a(0x22FFFFFF, k));
        round(g, x, y, x + w, y + h, 11, a(0xFA100B1A, k));
        g.gradient(x + 4, y + 1, x + w - 4, y + 34, a(Theme.accent(26), k), a(0x00000000, k));
        int ix = x + 14, iw = w - 28;
        Game game = VoidCore.game();
        int swC = Waypoints.COLORS[dlgColor];
        round(g, ix, y + 13, ix + 10, y + 23, 4, a(swC, k));
        bold(g, dlgPoint == null ? "Nowy waypoint" : "Edytuj waypoint", ix + 16, y + 12, TEXT, k);
        text(g, "Wymiar: " + dimensionName(game) + " · zapisze się tylko tutaj", ix + 16, y + 23, DIM, false, k);

        // nazwa
        int fy = y + 42;
        text(g, "Nazwa", ix, fy, MUTED, false, k);
        dlgField(g, mx, my, 0, ix, fy + 10, iw, blink, k);

        // kordy + moja pozycja
        fy += 34;
        int btnW = tw(g, "Moja pozycja") + 18;
        int fw = (iw - btnW - 6 - 3 * 4) / 3;
        for (int i = 1; i <= 3; i++) {
            int fx = ix + (i - 1) * (fw + 4);
            text(g, DLG_LABELS[i], fx, fy, dlgBad == i ? 0xFFF87171 : dlgField == i ? Theme.accent() : MUTED, false, k);
            dlgField(g, mx, my, i, fx, fy + 10, fw, blink, k);
        }
        int bx = ix + iw - btnW;
        boolean bh = mx >= bx && mx < bx + btnW && my >= fy + 10 && my < fy + 26;
        float bhv = anim("dlgPos", bh ? 1 : 0, 14f);
        round(g, bx, fy + 10, bx + btnW, fy + 26, 5, a(lerpColor(0x14FFFFFF, Theme.accent(70), bhv), k));
        roundOutline(g, bx, fy + 10, bx + btnW, fy + 26, 5, a(Theme.accent(120), k));
        text(g, "Moja pozycja", bx + 9, fy + 14, TEXT, false, k);
        hits.add(new Hit(bx, fy + 10, bx + btnW, fy + 26, () -> {
            dlg[1] = String.valueOf(game.x());
            dlg[2] = String.valueOf(game.y());
            dlg[3] = String.valueOf(game.z());
            dlgBad = -1;
        }, "Wpisuje kordy miejsca, w którym stoisz"));

        // kolor
        fy += 34;
        text(g, "Kolor", ix, fy, MUTED, false, k);
        for (int i = 0; i < Waypoints.COLORS.length; i++) {
            final int idx = i;
            int sx = ix + i * 19, sy = fy + 10;
            if (i == dlgColor) round(g, sx - 2, sy - 2, sx + 15, sy + 15, 6, a(0xFFFFFFFF, k));
            round(g, sx, sy, sx + 13, sy + 13, 5, a(Waypoints.COLORS[i], k));
            hits.add(new Hit(sx - 2, sy - 2, sx + 15, sy + 15, () -> dlgColor = idx, null));
        }

        // styl znacznika
        fy += 32;
        text(g, "Wygląd znacznika", ix, fy, MUTED, false, k);
        int pxl = ix;
        for (int i = 0; i < Waypoints.STYLES.length; i++) {
            final int idx = i;
            String lbl = Waypoints.STYLES[i];
            int pw = tw(g, lbl) + 16;
            boolean on = i == dlgStyle;
            round(g, pxl, fy + 10, pxl + pw, fy + 26, 5, a(on ? 0xFFF3F0FA : 0x10FFFFFF, k));
            if (!on) roundOutline(g, pxl, fy + 10, pxl + pw, fy + 26, 5, a(LINE, k));
            text(g, lbl, pxl + 8, fy + 14, on ? 0xFF120E1C : MUTED, false, k);
            hits.add(new Hit(pxl, fy + 10, pxl + pw, fy + 26, () -> dlgStyle = idx, null));
            pxl += pw + 4;
        }

        // odległość
        fy += 34;
        text(g, "Pokazuj odległość", ix, fy + 2, TEXT, false, k);
        toggle(g, ix + iw - 20, fy, anim("dlgDist", dlgDist ? 1 : 0, 12f), k);
        hits.add(new Hit(ix, fy - 2, ix + iw, fy + 14, () -> dlgDist = !dlgDist, null));

        // przyciski
        int by = y + h - 26;
        String save = "Zapisz", cancel = "Anuluj";
        int sw = tw(g, save) + 26, cw = tw(g, cancel) + 22;
        int sx = ix + iw - sw, cxb = sx - 6 - cw;
        boolean sh = mx >= sx && mx < sx + sw && my >= by && my < by + 17;
        round(g, sx, by, sx + sw, by + 17, 6, a(lerpColor(deep(), Theme.accent(), anim("dlgSave", sh ? 1 : 0, 14f) * 0.4f), k));
        text(g, save, sx + 13, by + 5, 0xFFFFFFFF, false, k);
        hits.add(new Hit(sx, by, sx + sw, by + 17, this::saveDialog, "Enter – zapisz"));
        round(g, cxb, by, cxb + cw, by + 17, 6, a(0x14FFFFFF, k));
        roundOutline(g, cxb, by, cxb + cw, by + 17, 6, a(LINE, k));
        text(g, cancel, cxb + 11, by + 5, TEXT, false, k);
        hits.add(new Hit(cxb, by, cxb + cw, by + 17, this::closeDialog, "Esc – anuluj"));
        if (dlgPoint != null) {
            String del = "Usuń";
            int dw = tw(g, del) + 22;
            round(g, ix, by, ix + dw, by + 17, 6, a(0x30B42318, k));
            roundOutline(g, ix, by, ix + dw, by + 17, 6, a(0x80F87171, k));
            text(g, del, ix + 11, by + 5, 0xFFFCA5A5, false, k);
            hits.add(new Hit(ix, by, ix + dw, by + 17, () -> { Waypoints.remove(dlgPoint); closeDialog(); }, "Usuń ten waypoint"));
        }

        // podpowiedź pod oknem
        String tip = "Tab – następne pole · Enter – zapisz · Esc – anuluj · Ctrl+V – wklej kordy z F3";
        for (Hit hh : hits) if (hh.tooltip != null && hh.contains(mx, my)) tip = hh.tooltip;
        tip = ellipsis(g, tip, width - 20);
        int tipW = tw(g, tip) + 16;
        if (y + h + 24 < height) {
            round(g, (width - tipW) / 2, y + h + 8, (width + tipW) / 2, y + h + 23, 5, a(0xC0100B1A, k));
            text(g, tip, (width - tipW) / 2 + 8, y + h + 12, MUTED, false, k);
        }
    }

    private void dlgField(Gfx g, int mx, int my, int idx, int x, int y, int w, boolean blink, float k) {
        boolean act = dlgField == idx, bad = dlgBad == idx;
        round(g, x, y, x + w, y + 16, 5, a(0xFF0A0711, k));
        roundOutline(g, x, y, x + w, y + 16, 5, a(bad ? 0xFFF87171 : act ? Theme.accent(255) : LINE, k));
        String v = dlg[idx];
        String fit = v;
        while (tw(g, fit) > w - 10 && fit.length() > 1) fit = fit.substring(1);
        if (act && dlgFresh && !v.isEmpty()) g.fill(x + 4, y + 3, x + 6 + tw(g, fit), y + 13, a(Theme.accent(80), k));
        text(g, fit, x + 5, y + 4, bad ? 0xFFFCA5A5 : TEXT, false, k);
        if (act && blink) g.fill(x + 6 + tw(g, fit), y + 3, x + 7 + tw(g, fit), y + 13, a(TEXT, k));
        hits.add(new Hit(x, y, x + w, y + 16, () -> { dlgField = idx; dlgFresh = true; }, idx == 0 ? "Nazwa widoczna nad znacznikiem" : "Wpisz " + DLG_LABELS[idx]));
    }

    private boolean dialogChar(int codepoint) {
        if (dlgField == 0) {
            if (codepoint < 32 || codepoint == 127) return true;
            String n = dlgFresh ? "" : dlg[0];
            if (n.length() < Waypoints.MAX_NAME) dlg[0] = n + new String(Character.toChars(codepoint));
            dlgFresh = false;
            return true;
        }
        char ch = codepoint < 128 ? (char) codepoint : 0;
        String v = dlgFresh ? "" : dlg[dlgField];
        if (ch >= '0' && ch <= '9') {
            String digits = v.startsWith("-") ? v.substring(1) : v;
            if (digits.equals("0")) v = v.substring(0, v.length() - 1);
            String nd = (digits.equals("0") ? "" : digits) + ch;
            if (Long.parseLong(nd) <= XYZ_MAX) v += ch;
        } else if (ch == '-' && v.isEmpty()) {
            v = "-";
        } else if ((ch == ' ' || ch == ',' || ch == ';') && dlgField < 3) {
            dlgField++;
            dlgFresh = true;
            return true;
        } else {
            return true;
        }
        dlg[dlgField] = v;
        dlgFresh = false;
        if (dlgBad == dlgField) dlgBad = -1;
        return true;
    }

    private boolean dialogKey(int key, int modifiers) {
        boolean shift = (modifiers & 1) != 0, ctrl = (modifiers & 2) != 0;
        if (key == 256) { closeDialog(); return true; }                                   // Esc
        if (key == 257 || key == 335) { saveDialog(); return true; }                      // Enter
        if (key == 258) { dlgField = (dlgField + (shift ? 3 : 1)) % 4; dlgFresh = true; return true; } // Tab
        if (key == 259) {                                                                  // Backspace
            String v = dlg[dlgField];
            dlg[dlgField] = dlgFresh || ctrl || v.isEmpty() ? "" : v.substring(0, v.offsetByCodePoints(v.length(), -1));
            dlgFresh = false;
            if (dlgBad == dlgField) dlgBad = -1;
            return true;
        }
        if (key == 86 && ctrl) {                                                           // Ctrl+V
            String clip;
            try { clip = VoidCore.game().clipboard(); } catch (Throwable t) { clip = ""; }
            if (clip == null) return true;
            if (dlgField == 0) {
                String n = (dlgFresh ? "" : dlg[0]) + clip.replace("\r", "").replace("\n", " ");
                dlg[0] = n.length() > Waypoints.MAX_NAME ? n.substring(0, Waypoints.MAX_NAME) : n;
                dlgFresh = false;
                return true;
            }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(clip);
            List<String> nums = new ArrayList<>();
            while (m.find() && nums.size() < 3) nums.add(String.valueOf((int) Math.floor(Math.max(-XYZ_MAX, Math.min(XYZ_MAX, Double.parseDouble(m.group()))))));
            if (nums.size() == 3) { dlg[1] = nums.get(0); dlg[2] = nums.get(1); dlg[3] = nums.get(2); }
            else if (nums.size() == 2) { dlg[1] = nums.get(0); dlg[3] = nums.get(1); }
            else if (nums.size() == 1) dlg[dlgField] = nums.get(0);
            dlgFresh = false;
            dlgBad = -1;
            return true;
        }
        return true;
    }

    // ───────────── Edytor HUD ─────────────

    private void renderEditor(Gfx g, int mx, int my) {
        String info = "Przeciągaj elementy HUD  ·  ESC – powrót do menu";
        int iw = tw(g, info) + 20;
        round(g, (width - iw) / 2, 6, (width + iw) / 2, 24, 6, a(0xE0120A22));
        text(g, info, (width - iw) / 2 + 10, 11, TEXT, false);

        if (dragging != null) {
            if (snapX) g.fill(width / 2, 0, width / 2 + 1, height, a(Theme.accent(150)));
            if (snapY) g.fill(0, height / 2, width, height / 2 + 1, a(Theme.accent(150)));
        }
        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 260.0);
        for (HudModule h : Modules.hud()) {
            if (!h.isEnabled() || !h.draggable()) continue;
            int w = h.width(g);
            int hh = h.height(g);
            boolean hover = h == dragging || (mx >= h.x && mx < h.x + w && my >= h.y && my < h.y + hh);
            float hv = anim("ed" + h.id, hover ? 1 : 0, 14f);
            if (hv > 0.02f) g.fill(h.x - 1, h.y - 1, h.x + w + 1, h.y + hh + 1, a(Theme.accent((int) (40 * hv))));
            if (h == Modules.SCOREBOARD) { // ramka zastępcza – prawdziwy scoreboard rysuje gra
                g.fill(h.x, h.y, h.x + w, h.y + hh, a(0x50000000));
                g.fill(h.x, h.y, h.x + w, h.y + 10, a(0x70000000));
                String sb = "Scoreboard";
                text(g, sb, h.x + (w - tw(g, sb)) / 2, h.y + 1, 0xFFFFFFFF, true);
                for (int i = 0; i < 6; i++) g.fill(h.x + 4, h.y + 16 + i * 12, h.x + 4 + 40 + (i * 23) % 50, h.y + 22 + i * 12, a(0x40FFFFFF));
            }
            roundOutline(g, h.x - 2, h.y - 2, h.x + w + 2, h.y + hh + 2, 3, a(Theme.accent((int) (hover ? 255 : 90 + 90 * pulse))));
            if (hover) text(g, h.name, h.x, Math.max(0, h.y - 12), Theme.accent(), true);
        }
    }

    // ───────────── Zamykanie ─────────────

    /** Rozpoczyna animację zamykania. */
    public void requestClose() {
        if (closingAt < 0) {
            closingAt = System.currentTimeMillis();
            editing = -1;
            Config.save();
        }
    }

    /** True, gdy animacja zamykania się skończyła – ekran może zniknąć. */
    public boolean closeFinished() {
        return closingAt >= 0 && System.currentTimeMillis() - closingAt >= 150;
    }

    // ───────────── Wejście ─────────────

    public boolean mouseClicked(double mx, double my, int button) {
        if (closingAt >= 0) return true;
        if (dialog) {
            if (button != 0) return true;
            for (Hit h : new ArrayList<>(hits)) if (h.contains(mx, my)) { h.action.run(); return true; }
            return true;
        }
        if (binding != null) {
            if (button >= 2) { setBinding(Keybinds.MOUSE + button); return true; }
            binding = null; // klik gdzie indziej – anuluj
            if (button != 0) return true;
        }
        if (button == 1 && !editHud) {
            for (Hit h : new ArrayList<>(hits)) if (h.right != null && h.contains(mx, my)) { h.right.run(); return true; }
            return false;
        }
        if (button != 0) return editHud;
        if (editHud) {
            if (lastGfx == null) return true;
            List<HudModule> hud = Modules.hud();
            for (int i = hud.size() - 1; i >= 0; i--) {
                HudModule h = hud.get(i);
                if (!h.isEnabled() || !h.draggable()) continue;
                int w = h.width(lastGfx);
                int hh = h.height(lastGfx);
                if (mx >= h.x && mx < h.x + w && my >= h.y && my < h.y + hh) {
                    dragging = h;
                    dragDx = (int) mx - h.x;
                    dragDy = (int) my - h.y;
                    return true;
                }
            }
            return true;
        }
        int before = editing;
        editing = -1;
        Waypoints.Point wpBefore = editingWp;
        Waypoints.Point xyzBefore = editingXyz;
        for (Hit h : new ArrayList<>(hits)) {
            if (h.contains(mx, my)) {
                if (xyzBefore != null && editingXyz == xyzBefore) {
                    // klik poza polem kordów: zapisz, a błędny tekst porzuć
                    keepXyz = false;
                    h.action.run();
                    if (!keepXyz && editingXyz == xyzBefore && !finishXyz()) editingXyz = null;
                    return true;
                }
                h.action.run();
                return true;
            }
        }
        if (before >= 0) Config.save();
        if (wpBefore != null) { editingWp = wpBefore; finishWpEdit(); }
        if (xyzBefore != null && !finishXyz()) editingXyz = null;
        return false;
    }

    public boolean mouseDragged(double mx, double my) {
        if (dragging == null || lastGfx == null) return false;
        int w = dragging.width(lastGfx);
        int h = dragging.height(lastGfx);
        int nx = (int) mx - dragDx;
        int ny = (int) my - dragDy;
        // przyciąganie do środka i krawędzi ekranu
        int centerX = (width - w) / 2;
        int centerY = (height - h) / 2;
        snapX = Math.abs(nx - centerX) <= 4;
        snapY = Math.abs(ny - centerY) <= 4;
        if (snapX) nx = centerX;
        if (snapY) ny = centerY;
        if (Math.abs(nx) <= 4) nx = 0;
        if (Math.abs(ny) <= 4) ny = 0;
        if (Math.abs(nx + w - width) <= 4) nx = width - w;
        if (Math.abs(ny + h - height) <= 4) ny = height - h;
        dragging.x = nx;
        dragging.y = ny;
        dragging.clamp(lastGfx, width, height);
        return true;
    }

    public boolean mouseReleased() {
        if (dragging == null) return false;
        dragging = null;
        snapX = snapY = false;
        Config.save();
        return true;
    }

    public boolean mouseScrolled(double vertical) {
        if (editHud || dialog) return dialog;
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - vertical * 20));
        return true;
    }

    /**
     * ESC: w edytorze wraca do menu, w menu zaczyna animację zamykania.
     * Zawsze obsłużone – ekran zamyka się sam po animacji (closeFinished).
     */
    public boolean escape() {
        if (dialog) { closeDialog(); return true; }
        if (binding != null) { binding = null; return true; }
        if (editingWp != null) { finishWpEdit(); return true; }
        if (editingXyz != null) { editingXyz = null; return true; }
        if (editHud) {
            editHud = false;
            dragging = null;
            openedAt = System.currentTimeMillis() - 60;
            tabChangedAt = System.currentTimeMillis();
            return true;
        }
        requestClose();
        return true;
    }

    public void closed() {
        binding = null;
        finishWpEdit();
        if (!finishXyz()) editingXyz = null;
        Config.save();
    }

    /** Wpisany znak (pole AutoText). */
    public boolean charTyped(int codepoint) {
        if (dialog) return dialogChar(codepoint);
        if (binding != null) return true;
        if (editingXyz != null) {
            char ch = codepoint < 128 ? (char) codepoint : 0;
            String v = xyzFresh ? "" : xyzBuf[xyzField];
            if (ch >= '0' && ch <= '9') {
                String digits = v.startsWith("-") ? v.substring(1) : v;
                if (digits.equals("0")) v = v.substring(0, v.length() - 1); // bez zer na początku
                if (Long.parseLong(digits.isEmpty() || digits.equals("0") ? String.valueOf(ch) : digits + ch) <= XYZ_MAX) v += ch; // świat ma ±30 mln bloków
            } else if (ch == '-' && v.isEmpty()) {
                v = "-";
            } else if ((ch == ' ' || ch == ',' || ch == ';') && !xyzFresh) {
                xyzField = (xyzField + 1) % 3; // spacja przechodzi do następnego pola
                xyzFresh = true;
                return true;
            } else {
                return true;
            }
            xyzBuf[xyzField] = v;
            xyzFresh = false;
            if (xyzBad == xyzField) xyzBad = -1;
            return true;
        }
        if (editingWp != null) {
            if (codepoint < 32 || codepoint == 127) return true;
            String n = editingWp.name == null ? "" : editingWp.name;
            if (n.length() < Waypoints.MAX_NAME) editingWp.name = n + new String(Character.toChars(codepoint));
            return true;
        }
        if (editing < 0) {
            if (editHud || closingAt >= 0 || !(tab.modules() || (tab == Tab.LOOK && lookSub == 0))) return false;
            if (codepoint < 32 || codepoint == 127 || search.length() >= 24) return true;
            search += new String(Character.toChars(codepoint));
            scroll = 0;
            tabChangedAt = System.currentTimeMillis() - 200;
            return true;
        }
        if (codepoint < 32 || codepoint == 127) return true;
        String t = AutoText.TEXTS[editing];
        if (t.length() < AutoText.MAX_LENGTH) AutoText.TEXTS[editing] = t + new String(Character.toChars(codepoint));
        return true;
    }

    /** Klawisze specjalne podczas wpisywania. Zwraca true, jeśli menu je obsłużyło. */
    public boolean keyPressed(int key, int modifiers) {
        if (dialog) return dialogKey(key, modifiers);
        if (binding != null) {
            if (key == 256) binding = null;                                 // Esc – anuluj
            else if (key == 259 || key == 261) setBinding(Keybinds.NONE);    // Backspace / Delete – usuń
            else setBinding(key);
            return true;
        }
        if (editingXyz != null) {
            boolean shift = (modifiers & 1) != 0;
            if (key == 259) { // Backspace
                String v = xyzBuf[xyzField];
                xyzBuf[xyzField] = xyzFresh || (modifiers & 2) != 0 || v.isEmpty() ? "" : v.substring(0, v.length() - 1);
                xyzFresh = false;
                if (xyzBad == xyzField) xyzBad = -1;
            } else if (key == 258) { // Tab / Shift+Tab
                xyzField = (xyzField + (shift ? 2 : 1)) % 3;
                xyzFresh = true;
            } else if (key == 262 && !shift) { // strzałka w prawo
                xyzField = Math.min(2, xyzField + 1); xyzFresh = true;
            } else if (key == 263) { // strzałka w lewo
                xyzField = Math.max(0, xyzField - 1); xyzFresh = true;
            } else if (key == 257 || key == 335) { // Enter
                if (xyzField < 2 && !xyzFresh) { xyzField++; xyzFresh = true; }
                else finishXyz();
            } else if (key == 256) {
                editingXyz = null;
            } else if (key == 86 && (modifiers & 2) != 0) { // Ctrl+V
                String clip;
                try { clip = VoidCore.game().clipboard(); } catch (Throwable t) { clip = ""; }
                pasteXyz(clip);
            }
            return true;
        }
        if (editingWp != null) {
            String n = editingWp.name == null ? "" : editingWp.name;
            if (key == 259) {
                if (!n.isEmpty()) editingWp.name = (modifiers & 2) != 0 ? "" : n.substring(0, n.offsetByCodePoints(n.length(), -1));
            } else if (key == 257 || key == 335 || key == 256) {
                finishWpEdit();
            }
            return true;
        }
        if (editing < 0) {
            if (search.isEmpty() || editHud) return false;
            if (key == 259) { // Backspace
                search = (modifiers & 2) != 0 ? "" : search.substring(0, search.offsetByCodePoints(search.length(), -1));
                return true;
            }
            if (key == 256) { // Esc czyści wyszukiwanie
                search = "";
                return true;
            }
            return false;
        }
        String t = AutoText.TEXTS[editing];
        boolean ctrl = (modifiers & 2) != 0;
        if (key == 259) { // Backspace
            if (!t.isEmpty()) AutoText.TEXTS[editing] = ctrl ? "" : t.substring(0, t.offsetByCodePoints(t.length(), -1));
        } else if (key == 257 || key == 335 || key == 256) { // Enter / Esc
            editing = -1;
            Config.save();
        } else if (ctrl && key == 86) { // Ctrl+V
            String clip = VoidCore.game().clipboard();
            if (clip != null) {
                String merged = t + clip.replace("\r", "").replace("\n", " ");
                AutoText.TEXTS[editing] = merged.length() > AutoText.MAX_LENGTH ? merged.substring(0, AutoText.MAX_LENGTH) : merged;
            }
        }
        return true;
    }
}
