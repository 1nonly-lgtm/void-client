package dev.oneonly.voidclient.core.gui;

import dev.oneonly.voidclient.core.AutoText;
import dev.oneonly.voidclient.core.Config;
import dev.oneonly.voidclient.core.Game;
import dev.oneonly.voidclient.core.Gfx;
import dev.oneonly.voidclient.core.Keybinds;
import dev.oneonly.voidclient.core.Look;
import dev.oneonly.voidclient.core.Screenshots;
import dev.oneonly.voidclient.core.Sky;
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
        GAME("Gra", "Dodatki do rozgrywki", new int[]{0b00001, 0b00010, 0b10100, 0b01000, 0b10100}),
        AUTOTEXT("AutoText", "Wiadomości pod klawiszami", new int[]{0b11111, 0b10001, 0b11111, 0b01000, 0b10000}),
        WAYPOINTS("Waypointy", "Znaczniki w świecie", new int[]{0b01110, 0b11011, 0b01110, 0b00100, 0b00100}),
        SKY("Niebo", "Własne nieba Void", new int[]{0b00100, 0b01110, 0b11111, 0b01110, 0b00100}),
        LOOK("Celownik", "Celownik i ramka bloku", new int[]{0b00100, 0b00100, 0b11011, 0b00100, 0b00100}),
        OPTIONS("Opcje", "Wygląd, FPS i skin", new int[]{0b10101, 0b01110, 0b11011, 0b01110, 0b10101});

        final String label;
        final String subtitle;
        final int[] icon;

        Tab(String label, String subtitle, int[] icon) {
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon;
        }
    }

    private static final class Hit {
        final int x1, y1, x2, y2;
        final Runnable action;
        final String tooltip;

        Hit(int x1, int y1, int x2, int y2, Runnable action, String tooltip) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.action = action; this.tooltip = tooltip;
        }

        boolean contains(double mx, double my) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    }

    private static Tab lastTab = Tab.ALL;

    // Wymiary w pikselach GUI (gra mnoży je przez skalę interfejsu).
    private static final int PW = 470;
    private static final int PH = 280;

    // Kolory z launchera (#120a22, #f3e8ff, #b9a6d6, #7c3aed…)
    private static final int PANEL = 0xF5120A22;
    private static final int SIDEBAR = 0xFF0A0614;
    private static final int CARD = 0xFF170D2E;
    private static final int CARD_HOVER = 0xFF221442;
    private static final int TEXT = 0xFFF3E8FF;
    private static final int MUTED = 0xFFB9A6D6;
    private static final int DIM = 0xFF7A6A99;

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

    private void text(Gfx g, String s, int x, int y, int c, boolean shadow) {
        int col = a(c);
        if ((col >>> 24) < 0x10) return; // gra pokazuje zbyt przezroczysty tekst jako pełny – pomijamy
        g.text(s, x, y, col, shadow);
    }

    private void text(Gfx g, String s, int x, int y, int c, boolean shadow, float extra) {
        int col = a(c, extra);
        if ((col >>> 24) < 0x10) return;
        g.text(s, x, y, col, shadow);
    }

    /** Nagłówek jak w launcherze: wielkie litery z fioletowym cieniem. */
    private void title(Gfx g, String s, int x, int y) {
        text(g, s, x + 1, y + 1, 0xFF4C1D95, false);
        text(g, s, x, y, TEXT, false);
    }

    /** Przycina tekst do szerokości i dodaje „…”, jeśli coś ucięto. */
    private static String ellipsis(Gfx g, String s, int w) {
        if (g.width(s) <= w) return s;
        String t = g.trim(s, Math.max(0, w - g.width("…")));
        return t.trim() + "…";
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
        round(g, x, y, x + 22, y + 12, 6, a(lerpColor(0xFF3A2D55, 0xFF7C3AED, on), extra));
        int kx = x + 2 + (int) (on * 10);
        round(g, kx, y + 2, kx + 8, y + 10, 4, a(0xFFFFFFFF, extra));
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
        ICONS.put("motionblur", new int[]{0b00111, 0b01110, 0b11100, 0b01110, 0b00111});
    }

    private static final int[] TILE_COLORS = {0xFFC084FC, 0xFFE9D5FF, 0xFFD946EF, 0xFFA78BFA, 0xFFF0ABFC, 0xFF8B5CF6, 0xFFDDD6FE};

    // ───────────── Tło jak w launcherze ─────────────

    private void background(Gfx g) {
        g.gradient(0, 0, width, height, a(0xC81A0838), a(0xEE05020F));
        long t = System.currentTimeMillis();
        // orbita (wirujący łuk w prawym górnym rogu)
        double cxo = width * 0.80, cyo = -height * 0.12, rad = height * 0.78;
        double base = t / 2600.0;
        for (int i = 0; i < 70; i++) {
            double ang = base + i * 0.018;
            int x = (int) (cxo + Math.cos(ang) * rad);
            int y = (int) (cyo + Math.sin(ang) * rad);
            g.fill(x, y, x + 2, y + 2, a(Theme.accent((int) (10 + 150 * (i / 70f)))));
        }
        for (int i = 0; i < 90; i++) {
            double ang = -base * 0.6 + i * 0.07;
            int x = (int) (cxo + Math.cos(ang) * rad * 0.82);
            int y = (int) (cyo + Math.sin(ang) * rad * 0.82);
            g.fill(x, y, x + 1, y + 1, a(Theme.accent(40)));
        }
        // unoszące się cząsteczki
        for (int i = 0; i < 18; i++) {
            float speed = 18 + (i * 7) % 15;
            float y = height + 10 - ((t / 1000f * speed + i * 53) % (height + 20));
            int x = (int) (width * (((i * 61 + 17) % 100) / 100f));
            int size = 1 + (i % 3 == 0 ? 1 : 0);
            float fade = Math.min(1f, (height - y) / 60f) * Math.min(1f, y / 80f);
            if (fade <= 0) continue;
            int col = i % 4 == 0 ? 0xFFD946EF : (i % 3 == 0 ? 0xFFE9D5FF : Theme.accent());
            g.fill(x, (int) y, x + size, (int) y + size, a(col, 0.8f * fade));
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

        if (editHud) {
            g.fill(0, 0, w, h, a(0x40000000));
            renderEditor(g, mx, my);
        } else {
            background(g);
            renderPanel(g, mx, my);
        }
        if (alpha > 0.1f && editHud) {
            text(g, "by: ", 6, h - 12, 0xFFB9A6D6, true);
            text(g, "1nonly $", 6 + g.width("by: "), h - 12, Theme.accent(), true);
        }
    }

    /** Wymiary panelu – dopasowane do okna (małe okno / duża skala GUI). */
    private int panelW() { return Math.max(300, Math.min(PW, width - 16)); }
    private int panelH() { return Math.max(200, Math.min(PH, height - 30)); }

    private static final int SIDE_W = 104;

    private void renderPanel(Gfx g, int mx, int my) {
        int pw = panelW(), ph = panelH();
        int px = (width - pw) / 2;
        int py = (height - ph) / 2 + (int) ((1f - alpha) * 14);
        long nowMs = System.currentTimeMillis();
        float pulse = 0.5f + 0.5f * (float) Math.sin(nowMs / 420.0);

        // ── panel: poświata, szklane tło z gradientem, gwiazdy, świecąca linia u góry (jak w launcherze) ──
        round(g, px - 7, py - 7, px + pw + 7, py + ph + 7, 14, a(Theme.accent((int) (6 + 8 * pulse))));
        round(g, px - 4, py - 4, px + pw + 4, py + ph + 4, 12, a(Theme.accent((int) (14 + 10 * pulse))));
        round(g, px - 1, py - 1, px + pw + 1, py + ph + 1, 10, a(Theme.accent(70)));
        round(g, px, py, px + pw, py + ph, 9, a(0xF6120A22));
        g.gradient(px + 3, py + 3, px + pw - 3, py + ph - 3, a(0x40221048), a(0x00000000));
        for (int i = 0; i < 34; i++) {
            int sx = px + 6 + (int) (((i * 97 + 31) % 1000) / 1000f * (pw - 12));
            int sy = py + 6 + (int) (((i * 57 + 11) % 1000) / 1000f * (ph - 12));
            float tw = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(nowMs / 700.0 + i * 1.9));
            g.fill(sx, sy, sx + 1, sy + 1, a(i % 3 == 0 ? 0xFFF0ABFC : 0xFFE9D5FF, 0.35f * tw));
        }
        // linia u góry: jasna w środku, gaśnie na boki
        int lineW = pw - 40;
        for (int i = 0; i < lineW; i += 2) {
            float t = 1f - Math.abs(i / (float) lineW * 2f - 1f);
            g.fill(px + 20 + i, py, px + 22 + i, py + 1, a(Theme.accent((int) (230 * t))));
        }

        // ── pasek boczny (jak menu w launcherze) ──
        int sbx = px + 6, sby = py + 6, sbw = SIDE_W - 6, sbh = ph - 12;
        round(g, sbx, sby, sbx + sbw, sby + sbh, 7, a(0xC00A0614));
        g.fill(sbx + sbw, sby + 10, sbx + sbw + 1, sby + sbh - 10, a(Theme.accent(50)));

        // logo z pulsującą aureolą + nazwa
        int lx = sbx + 8, ly = sby + 9;
        round(g, lx - 3, ly - 3, lx + 25, ly + 25, 14, a(Theme.accent((int) (40 + 70 * pulse))));
        if (alpha > 0.55f) g.logo(lx, ly, 22);
        text(g, "VOID", lx + 30, ly + 3, TEXT, true);
        text(g, "CLIENT", lx + 30, ly + 13, Theme.accent(), false);

        // zakładki pionowo, z płynnie przesuwanym podświetleniem
        Tab[] tabs = Tab.values();
        int rowH = 20, rowGap = 3, listY = ly + 36;
        float iy = anim("sideY", listY + tab.ordinal() * (rowH + rowGap), 16f);
        round(g, sbx + 5, (int) iy, sbx + sbw - 5, (int) iy + rowH, 6, a(Theme.accent(70)));
        g.gradient(sbx + 6, (int) iy + 1, sbx + sbw - 6, (int) iy + rowH / 2, a(0x26FFFFFF), a(0x00FFFFFF));
        round(g, sbx + 1, (int) iy + 4, sbx + 4, (int) iy + rowH - 4, 1, a(Theme.accent(255)));
        round(g, sbx, (int) iy + 2, sbx + 6, (int) iy + rowH - 2, 3, a(Theme.accent(60)));
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int ry = listY + i * (rowH + rowGap);
            boolean hover = mx >= sbx + 5 && mx < sbx + sbw - 5 && my >= ry && my < ry + rowH;
            float hv = anim("side" + i, hover && t != tab ? 1 : 0, 14f);
            if (hv > 0.01f) round(g, sbx + 5, ry, sbx + sbw - 5, ry + rowH, 6, a(0x16FFFFFF, hv));
            boolean act = t == tab;
            int ix = sbx + 12 + (int) (hv * 2);
            icon(g, t.icon, ix, ry + 7, 1, a(act ? 0xFFFFFFFF : lerpColor(MUTED, TEXT, hv)));
            text(g, t.label, ix + 10, ry + 6, act ? 0xFFFFFFFF : lerpColor(MUTED, TEXT, hv), false);
            hits.add(new Hit(sbx + 5, ry, sbx + sbw - 5, ry + rowH, () -> switchTab(t), t.subtitle));
        }
        // stopka paska: podpis jak w launcherze
        if (listY + tabs.length * (rowH + rowGap) < sby + sbh - 16) {
            text(g, "by:", sbx + 9, sby + sbh - 13, DIM, false);
            text(g, "1nonly $", sbx + 9 + g.width("by: "), sby + sbh - 13, Theme.accent(), false);
        }

        // ── nagłówek: tytuł zakładki, wyszukiwarka, Edytuj HUD, zamknij ──
        int mxL = px + SIDE_W + 8, mxR = px + pw - 10;
        int ty = py + 10;
        title(g, tab.label.toUpperCase(), mxL, ty + 1);
        text(g, tab.subtitle, mxL, ty + 12, DIM, false);

        int cxb = mxR - 20, cyb = ty;
        boolean cHover = mx >= cxb && mx < cxb + 20 && my >= cyb && my < cyb + 20;
        float chv = anim("closeBtn", cHover ? 1 : 0, 14f);
        round(g, cxb, cyb, cxb + 20, cyb + 20, 6, a(lerpColor(0x14FFFFFF, 0xFFB42318, chv)));
        for (int i = 0; i < 7; i++) {
            g.fill(cxb + 7 + i, cyb + 7 + i, cxb + 8 + i, cyb + 8 + i, a(TEXT));
            g.fill(cxb + 13 - i, cyb + 7 + i, cxb + 14 - i, cyb + 8 + i, a(TEXT));
        }
        hits.add(new Hit(cxb, cyb, cxb + 20, cyb + 20, this::requestClose, "Zamknij menu"));

        String el = "Edytuj HUD";
        int bw = g.width(el) + 18, bx = cxb - 6 - bw;
        boolean bHover = mx >= bx && mx < bx + bw && my >= ty && my < ty + 20;
        float bhv = anim("editBtn", bHover ? 1 : 0, 14f);
        if (bhv > 0.01f) round(g, bx - 2, ty - 2, bx + bw + 2, ty + 22, 8, a(Theme.accent((int) (70 * bhv))));
        round(g, bx, ty, bx + bw, ty + 20, 6, a(lerpColor(0xFF7C3AED, 0xFF9F67FF, bhv)));
        g.gradient(bx + 2, ty + 1, bx + bw - 2, ty + 9, a(0x30FFFFFF), a(0x00FFFFFF));
        text(g, el, bx + 9, ty + 6, 0xFFFFFFFF, false);
        hits.add(new Hit(bx, ty, bx + bw, ty + 20, () -> { editHud = true; editing = -1; }, "Przeciągaj elementy HUD myszką – przyciągają się do środka i krawędzi"));

        int titleW = Math.max(g.width(tab.label.toUpperCase()) + 2, g.width(tab.subtitle));
        int sx = Math.max(mxL + titleW + 10, mxL + 70), sw = bx - 8 - sx;
        boolean searchable = tab == Tab.ALL || tab == Tab.HUD || tab == Tab.GAME;
        if (sw > 60) {
            boolean focus = !search.isEmpty();
            if (focus) round(g, sx - 2, ty - 2, sx + sw + 2, ty + 22, 9, a(Theme.accent(40)));
            round(g, sx, ty, sx + sw, ty + 20, 7, a(0xFF0B0617));
            roundOutline(g, sx, ty, sx + sw, ty + 20, 7, a(focus ? Theme.accent(200) : 0x1FFFFFFF));
            icon(g, new int[]{0b11100, 0b10100, 0b11100, 0b00010, 0b00001}, sx + 7, ty + 7, 1, a(focus ? Theme.accent() : MUTED));
            boolean blink = (nowMs / 500) % 2 == 0;
            if (search.isEmpty()) {
                text(g, g.trim(searchable ? "Szukaj modułu…" : "Szukaj w modułach…", sw - 24), sx + 17, ty + 6, DIM, false);
            } else {
                String fit = search;
                while (g.width(fit) > sw - 30 && fit.length() > 1) fit = fit.substring(1);
                text(g, fit, sx + 17, ty + 6, TEXT, false);
                if (blink) g.fill(sx + 18 + g.width(fit), ty + 5, sx + 19 + g.width(fit), ty + 15, a(TEXT));
            }
            hits.add(new Hit(sx, ty, sx + sw, ty + 20, () -> { if (!searchable) switchTab(Tab.ALL); }, "Zacznij pisać, żeby wyszukać moduł"));
        }

        // licznik pod nagłówkiem
        int cx = mxL, cw = mxR - mxL;
        int lineY = ty + 26;
        g.fill(cx, lineY, cx + cw, lineY + 1, a(0x12FFFFFF));
        String chip = chipText();
        if (!chip.isEmpty()) {
            int cwid = g.width(chip) + 12;
            round(g, cx + cw - cwid, lineY - 5, cx + cw, lineY + 6, 5, a(0xFF1D1235));
            text(g, chip, cx + cw - cwid + 6, lineY - 3, MUTED, false);
        }

        // ── treść ──
        int top = lineY + 9;
        int ch = py + ph - 20 - top;
        int sc = (int) anim("scroll", scroll, 18f);
        g.scissor(cx - 3, top, cx + cw + 3, top + ch);
        int contentH;
        switch (tab) {
            case OPTIONS: contentH = renderOptions(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case AUTOTEXT: contentH = renderAutoText(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case WAYPOINTS: contentH = renderWaypoints(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case SKY: contentH = renderSky(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case LOOK: contentH = renderLook(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            default: contentH = renderModules(g, mx, my, cx, top - sc, cw, top, top + ch, tab);
        }
        g.noScissor();
        maxScroll = Math.max(0, contentH - ch);
        if (maxScroll > 0) {
            int barH = Math.max(16, ch * ch / Math.max(1, contentH));
            int barY = top + (int) ((ch - barH) * (sc / (float) maxScroll));
            round(g, cx + cw + 4, top, cx + cw + 7, top + ch, 1, a(0x10FFFFFF));
            round(g, cx + cw + 4, barY, cx + cw + 7, barY + barH, 1, a(Theme.accent(170)));
        }
        // miękkie wygaszenie treści przy dolnej krawędzi
        g.gradient(cx - 3, top + ch - 8, cx + cw + 3, top + ch, a(0x00120A22), a(0xE0120A22));

        // stopka
        String tip = tab == Tab.AUTOTEXT ? "Kliknij pole i wpisz tekst. Klawisz: Sterowanie → Void Client."
                : tab == Tab.WAYPOINTS ? "Kliknij nazwę, żeby ją zmienić · kolorowy kwadrat zmienia kolor"
                : tab == Tab.SKY ? "Kliknij niebo, żeby je ustawić – gra przeładuje zasoby"
                : tab == Tab.LOOK ? "Kliknij wartość, żeby ją zmienić – podgląd na górze"
                : tab == Tab.OPTIONS ? "Kliknij klawisz, a potem wciśnij nowy (Esc – anuluj, Backspace – usuń)"
                : "Kliknij kartę, aby włączyć lub wyłączyć. Pisz, aby szukać.";
        if (binding != null) tip = "Wciśnij klawisz albo przycisk myszy… (Esc – anuluj, Backspace – usuń)";
        for (Hit h : hits) if (h.tooltip != null && h.contains(mx, my)) tip = h.tooltip;
        String close = "Prawy Shift – zamknij";
        int fy = py + ph - 14;
        text(g, ellipsis(g, tip, cw - g.width(close) - 10), cx, fy, MUTED, false);
        text(g, close, cx + cw - g.width(close), fy, DIM, false);
    }

    private String chipText() {
        if (tab == Tab.ALL || tab == Tab.HUD || tab == Tab.GAME) {
            List<Module> list = tab == Tab.ALL ? Modules.all() : Modules.byCategory(tab == Tab.HUD ? Module.Category.HUD : Module.Category.GAME);
            int on = 0;
            for (Module m : list) if (m.isEnabled()) on++;
            return on + "/" + list.size() + " aktywne";
        }
        if (tab == Tab.WAYPOINTS) {
            Game g = VoidCore.game();
            return g.hasPlayer() ? Waypoints.here(g).size() + " w tym świecie" : "";
        }
        if (tab == Tab.AUTOTEXT) {
            int used = 0;
            for (String s : AutoText.TEXTS) if (s != null && !s.trim().isEmpty()) used++;
            return used + "/" + AutoText.SLOTS + " zajęte";
        }
        return "";
    }

    private void switchTab(Tab t) {
        if (t == tab) return;
        tab = t;
        lastTab = t;
        scroll = 0;
        anims.put("scroll", 0f);
        editing = -1;
        binding = null;
        finishWpEdit();
        if (t != Tab.ALL && t != Tab.HUD && t != Tab.GAME) search = "";
        tabChangedAt = System.currentTimeMillis();
    }

    // ───────────── Zakładki ─────────────

    private int renderModules(Gfx g, int mx, int my, int x, int y, int w, int clipTop, int clipBottom, Tab which) {
        List<Module> source = which == Tab.ALL ? Modules.all()
                : Modules.byCategory(which == Tab.HUD ? Module.Category.HUD : Module.Category.GAME);
        List<Module> list = new ArrayList<>();
        String q = search.trim().toLowerCase();
        for (Module m : source) {
            if (q.isEmpty() || m.name.toLowerCase().contains(q) || m.description.toLowerCase().contains(q)) list.add(m);
        }
        if (list.isEmpty()) {
            text(g, "Brak modułów dla „" + search + "”", x + (w - g.width("Brak modułów dla „" + search + "”")) / 2, y + 40, MUTED, false);
            return 60;
        }
        int gap = 7;
        int cols = w >= 330 ? 3 : 2;
        int cardW = (w - gap * (cols - 1)) / cols;
        int cardH = 64;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float st = stagger(i);
            int cx = x + (i % cols) * (cardW + gap);
            int cy = y + (i / cols) * (cardH + gap) + (int) ((1 - st) * 10);
            boolean ok = m.available();
            if (!ok) st *= 0.45f; // niedostępne w tej wersji – wyszarzone
            boolean visible = cy + cardH > clipTop && cy < clipBottom;
            boolean hover = visible && mx >= cx && mx < cx + cardW && my >= cy && my < cy + cardH && my >= clipTop && my < clipBottom;
            float hv = anim("h" + m.id, hover ? 1 : 0, 14f);
            float on = anim("t" + m.id, m.isEnabled() ? 1 : 0, 12f);
            int top = cy - (int) (hv * 2);

            // poświata pod kartą (włączony / najechany)
            float glow = Math.max(on * 0.6f, hv);
            if (glow > 0.02f) round(g, cx - 2, top - 2, cx + cardW + 2, top + cardH + 2, 9, a(Theme.accent((int) (38 * glow)), st));
            // szklana karta z jaśniejszą górą
            round(g, cx, top, cx + cardW, top + cardH, 7, a(lerpColor(CARD, CARD_HOVER, hv), st));
            g.gradient(cx + 2, top + 1, cx + cardW - 2, top + cardH / 2, a(lerpColor(0x0CFFFFFF, Theme.accent(26), on), st), a(0x00000000));
            g.fill(cx + 6, top, cx + cardW - 6, top + 1, a(lerpColor(0x18FFFFFF, Theme.accent(170), on), st));
            roundOutline(g, cx, top, cx + cardW, top + cardH, 7, a(lerpColor(0x14FFFFFF, Theme.accent(210), Math.max(hv * 0.85f, on * 0.4f)), st));

            // ikona w kafelku z gradientem
            int tileS = 24, tx = cx + 8, tyy = top + 8;
            if (on > 0.02f) round(g, tx - 2, tyy - 2, tx + tileS + 2, tyy + tileS + 2, 8, a(Theme.accent((int) (60 * on)), st));
            round(g, tx, tyy, tx + tileS, tyy + tileS, 6, a(lerpColor(0xFF2A1F45, 0xFF7C3AED, on), st));
            g.gradient(tx + 2, tyy + 1, tx + tileS - 2, tyy + tileS / 2, a(0x2AFFFFFF, st), a(0x00FFFFFF));
            int[] ic = ICONS.getOrDefault(m.id, Tab.ALL.icon);
            icon(g, ic, tx + 4, tyy + 4, 3, a(lerpColor(0xFFB9A6D6, 0xFFFFFFFF, on), st));

            // nazwa i stan
            int nx = tx + tileS + 7, nw = cx + cardW - 6 - nx;
            text(g, ellipsis(g, m.name, nw), nx, tyy + 3, TEXT, false, st);
            text(g, !ok ? "Niedostępny" : m.isEnabled() ? "Włączony" : "Wyłączony", nx, tyy + 14, lerpColor(DIM, Theme.accent(), on), false, st);

            // opis (jedna linia) i przełącznik jak w launcherze
            int by = top + cardH - 18;
            String tag = m.category == Module.Category.HUD ? "HUD" : "GRA";
            int tw = g.width(tag) + 10;
            round(g, cx + 8, by, cx + 8 + tw, by + 12, 4, a(lerpColor(0xFF231838, Theme.accent(60), on), st));
            text(g, tag, cx + 13, by + 2, lerpColor(DIM, 0xFFE9D5FF, on), false, st);
            int swx = cx + cardW - 30;
            if (on > 0.02f) round(g, swx - 2, by - 2, swx + 24, by + 14, 8, a(Theme.accent((int) (70 * on)), st));
            toggle(g, swx, by, on, st);

            if (visible) {
                hits.add(new Hit(cx, Math.max(top, clipTop), cx + cardW, Math.min(top + cardH, clipBottom), () -> {
                    if (!m.available()) return;
                    m.toggle();
                    Config.save();
                }, m.name + " – " + (ok ? m.description : m.unavailableReason())));
            }
        }
        int rows = (list.size() + cols - 1) / cols;
        return rows * (cardH + gap) - gap;
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
        if (!fpsStatus.isEmpty()) { text(g, g.trim(fpsStatus, w), x, ty, Theme.accent(), false); ty += 11; }
        String st = SkinUploader.status;
        if (st != null && !st.isEmpty()) { text(g, g.trim(st, w), x, ty, Theme.accent(), false); ty += 11; }
        text(g, g.trim("Menu i AutoText: Opcje gry → Sterowanie → Void Client", w), x, ty, DIM, false);
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
            text(g, m, x + (w - g.width(m)) / 2, y + 40, MUTED, false);
            return 60;
        }
        // przycisk „Dodaj tutaj”
        String add = "+  Dodaj waypoint tutaj";
        String hint = "klawisz: " + Keybinds.name(Keybinds.waypoint);
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        float hv = anim("wpAdd", hov ? 1 : 0, 14f);
        round(g, x, y, x + w, y + h, 6, a(lerpColor(0xFF6D28D9, 0xFF8B45F5, hv)));
        text(g, add, x + 10, y + 8, 0xFFFFFFFF, false);
        text(g, hint, x + w - 10 - g.width(hint), y + 8, 0xFFE9D5FF, false);
        if (y + h > clipTop && y < clipBottom) {
            hits.add(new Hit(x, Math.max(y, clipTop), x + w, Math.min(y + h, clipBottom), () -> {
                finishWpEdit();
                editingWp = Waypoints.addHere(game);
                if (!Modules.WAYPOINTS.isEnabled()) Modules.WAYPOINTS.setEnabled(true);
            }, "Zapisuje Twoją obecną pozycję – od razu możesz wpisać nazwę"));
        }

        List<Waypoints.Point> list = Waypoints.here(game);
        int ry = y + h + gap + 2;
        if (list.isEmpty()) {
            String m = "Brak waypointów w tym świecie";
            text(g, m, x + (w - g.width(m)) / 2, ry + 14, DIM, false);
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
            if (visible) hits.add(new Hit(x + 4, Math.max(yy, clipTop), x + 20, Math.min(yy + h, clipBottom), () -> {
                p.color = (p.color + 1) % Waypoints.COLORS.length;
                Config.save();
            }, "Zmień kolor"));

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
            String info = p.x + " " + p.y + " " + p.z + "  ·  " + Waypoints.distance(game, p) + " m";
            int iw = g.width(info);
            int infoX = ex - 8 - iw;
            text(g, info, infoX, yy + 8, MUTED, false, st);

            // nazwa (klik = edycja)
            int nx = x + 26;
            int nw = infoX - 8 - nx;
            String name = p.name == null ? "" : p.name;
            String fit = name;
            while (g.width(fit) > nw - 4 && fit.length() > 1) fit = ed ? fit.substring(1) : fit.substring(0, fit.length() - 1);
            text(g, fit, nx, yy + 8, TEXT, false, st);
            if (ed && blink) g.fill(nx + g.width(fit) + 1, yy + 6, nx + g.width(fit) + 2, yy + 17, a(TEXT));
            if (visible) hits.add(new Hit(nx - 2, Math.max(yy, clipTop), infoX - 4, Math.min(yy + h, clipBottom), () -> {
                finishWpEdit();
                editingWp = p;
            }, "Kliknij, żeby zmienić nazwę (Enter – zapisz)"));
        }
        int end = ry + list.size() * (h + gap) + 2;
        String note = Modules.WAYPOINTS.isEnabled() ? "Znaczniki pokazują się na ekranie z nazwą i odległością"
                : "Moduł Waypointy jest wyłączony (zakładka Gra) – znaczniki są ukryte";
        text(g, g.trim(note, w), x, end, DIM, false);
        return end - y + 12;
    }

    private void optionRow(Gfx g, int mx, int my, int x, int y, int w, int h, int idx, String label, String value,
                           Runnable action, int clipTop, int clipBottom, String tip) {
        float st = stagger(idx);
        y += (int) ((1 - st) * 8);
        boolean visible = y + h > clipTop && y < clipBottom;
        boolean hover = visible && mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        float hv = anim("o" + idx, hover ? 1 : 0, 14f);
        if (hv > 0.02f) round(g, x - 2, y - 2, x + w + 2, y + h + 2, 8, a(Theme.accent((int) (34 * hv)), st));
        round(g, x, y, x + w, y + h, 6, a(lerpColor(CARD, CARD_HOVER, hv), st));
        g.fill(x + 6, y, x + w - 6, y + 1, a(0x16FFFFFF, st));
        roundOutline(g, x, y, x + w, y + h, 6, a(hv > 0.02f ? Theme.accent((int) (hv * 150)) : 0x14FFFFFF, st));
        g.fill(x + 1, y + 5, x + 3, y + h - 5, a(Theme.accent((int) (60 + 150 * hv)), st));
        text(g, label, x + 10, y + (h - 8) / 2, TEXT, false, st);
        int vw = g.width(value) + 16;
        int vx = x + w - vw - 5;
        round(g, vx, y + 4, x + w - 5, y + h - 4, 5, a(lerpColor(0xFF7C3AED, 0xFF9F67FF, hv), st));
        g.gradient(vx + 2, y + 5, x + w - 7, y + h / 2, a(0x2AFFFFFF, st), a(0x00FFFFFF));
        text(g, value, vx + 8, y + (h - 8) / 2, TEXT, false, st);
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
            int keyW = Math.max(46, g.width(key) + 12);
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
            while (g.width(fit) > boxW - 14 && fit.length() > 1) fit = fit.substring(1);
            text(g, fit, boxX + 6, ry + 8, placeholder ? DIM : TEXT, false, st);
            if (on && blink) {
                int cx = boxX + 6 + (placeholder ? 0 : g.width(fit)) + 1;
                g.fill(cx, ry + 6, cx + 1, ry + 17, a(TEXT));
            }

            round(g, x + w - keyW, ry, x + w, ry + h, 6, a(CARD, st));
            boolean bound = !key.equals("-");
            text(g, g.trim(bound ? key : "brak klawisza", keyW - 10), x + w - keyW + 6, ry + 8, bound ? Theme.accent() : DIM, false, st);

            if (visible) {
                hits.add(new Hit(boxX, Math.max(ry, clipTop), boxX + boxW, Math.min(ry + h, clipBottom), () -> editing = slot,
                        "Klawisz dla slotu " + (slot + 1) + ": Sterowanie → Void Client → AutoText " + (slot + 1)));
            }
        }
        int endY = y + AutoText.SLOTS * (h + gap) + 2;
        text(g, g.trim(Modules.AUTOTEXT.isEnabled() ? "Enter – zapisz · Ctrl+V – wklej · Ctrl+Backspace – wyczyść"
                : "Moduł AutoText jest wyłączony (zakładka Gra)", w), x, endY, DIM, false);
        return endY - y + 12;
    }

    // ───────────── Edytor HUD ─────────────

    private void renderEditor(Gfx g, int mx, int my) {
        String info = "Przeciągaj elementy HUD  ·  ESC – powrót do menu";
        int iw = g.width(info) + 20;
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
        if (binding != null) {
            if (button >= 2) { setBinding(Keybinds.MOUSE + button); return true; }
            binding = null; // klik gdzie indziej – anuluj
            if (button != 0) return true;
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
        for (Hit h : new ArrayList<>(hits)) {
            if (h.contains(mx, my)) {
                h.action.run();
                return true;
            }
        }
        if (before >= 0) Config.save();
        if (wpBefore != null) { editingWp = wpBefore; finishWpEdit(); }
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
        if (editHud) return false;
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - vertical * 20));
        return true;
    }

    /**
     * ESC: w edytorze wraca do menu, w menu zaczyna animację zamykania.
     * Zawsze obsłużone – ekran zamyka się sam po animacji (closeFinished).
     */
    public boolean escape() {
        if (binding != null) { binding = null; return true; }
        if (editingWp != null) { finishWpEdit(); return true; }
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
        Config.save();
    }

    /** Wpisany znak (pole AutoText). */
    public boolean charTyped(int codepoint) {
        if (binding != null) return true;
        if (editingWp != null) {
            if (codepoint < 32 || codepoint == 127) return true;
            String n = editingWp.name == null ? "" : editingWp.name;
            if (n.length() < Waypoints.MAX_NAME) editingWp.name = n + new String(Character.toChars(codepoint));
            return true;
        }
        if (editing < 0) {
            if (editHud || closingAt >= 0 || (tab != Tab.ALL && tab != Tab.HUD && tab != Tab.GAME)) return false;
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
        if (binding != null) {
            if (key == 256) binding = null;                                 // Esc – anuluj
            else if (key == 259 || key == 261) setBinding(Keybinds.NONE);    // Backspace / Delete – usuń
            else setBinding(key);
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
