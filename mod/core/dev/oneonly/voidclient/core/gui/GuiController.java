package dev.oneonly.voidclient.core.gui;

import dev.oneonly.voidclient.core.AutoText;
import dev.oneonly.voidclient.core.Config;
import dev.oneonly.voidclient.core.Gfx;
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
    private static final int PW = 420;
    private static final int PH = 268;

    // Kolory z launchera (#120a22, #f3e8ff, #b9a6d6, #7c3aed…)
    private static final int PANEL = 0xF5120A22;
    private static final int SIDEBAR = 0xFF0A0614;
    private static final int CARD = 0xFF170E2C;
    private static final int CARD_HOVER = 0xFF22153F;
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
        text(g, s, x + 2, y + 2, 0xFF4C1D95, false);
        text(g, s, x, y, TEXT, false);
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
        if (alpha > 0.1f) {
            text(g, "by: ", 6, h - 12, 0xFFB9A6D6, true);
            text(g, "1nonly $", 6 + g.width("by: "), h - 12, Theme.accent(), true);
        }
    }

    private void renderPanel(Gfx g, int mx, int my) {
        int px = (width - PW) / 2;
        int py = (height - PH) / 2 + (int) ((1f - alpha) * 14);
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 420.0);

        // panel z poświatą
        round(g, px - 5, py - 5, px + PW + 5, py + PH + 5, 12, a(Theme.accent((int) (10 + 10 * pulse))));
        round(g, px - 2, py - 2, px + PW + 2, py + PH + 2, 9, a(Theme.accent(45)));
        round(g, px, py, px + PW, py + PH, 8, a(PANEL));
        g.gradient(px + 6, py + 1, px + PW - 6, py + 44, a(Theme.accent(26)), a(0x00000000));

        // ── górny pasek: logo, nazwa, wyszukiwarka, Edytuj HUD, zamknij ──
        int ty = py + 10;
        round(g, px + 9, ty - 2, px + 33, ty + 22, 12, a(Theme.accent((int) (40 + 60 * pulse))));
        if (alpha > 0.55f) g.logo(px + 11, ty, 20);
        text(g, "VOID", px + 40, ty + 2, TEXT, true);
        text(g, "CLIENT", px + 40, ty + 12, Theme.accent(), false);

        // przycisk zamknięcia
        int cxb = px + PW - 28, cyb = ty;
        boolean cHover = mx >= cxb && mx < cxb + 20 && my >= cyb && my < cyb + 20;
        float chv = anim("closeBtn", cHover ? 1 : 0, 14f);
        round(g, cxb, cyb, cxb + 20, cyb + 20, 6, a(lerpColor(0x14FFFFFF, 0xFFB42318, chv)));
        for (int i = 0; i < 7; i++) {
            g.fill(cxb + 7 + i, cyb + 7 + i, cxb + 8 + i, cyb + 8 + i, a(TEXT));
            g.fill(cxb + 13 - i, cyb + 7 + i, cxb + 14 - i, cyb + 8 + i, a(TEXT));
        }
        hits.add(new Hit(cxb, cyb, cxb + 20, cyb + 20, this::requestClose, "Zamknij menu"));

        // Edytuj HUD
        String el = "Edytuj HUD";
        int bw = g.width(el) + 18, bx = cxb - 6 - bw;
        boolean bHover = mx >= bx && mx < bx + bw && my >= ty && my < ty + 20;
        float bhv = anim("editBtn", bHover ? 1 : 0, 14f);
        round(g, bx, ty, bx + bw, ty + 20, 6, a(lerpColor(0xFF7C3AED, 0xFF9F67FF, bhv)));
        text(g, el, bx + 9, ty + 6, 0xFFFFFFFF, false);
        hits.add(new Hit(bx, ty, bx + bw, ty + 20, () -> { editHud = true; editing = -1; }, "Przeciągaj elementy HUD myszką – przyciągają się do środka i krawędzi"));

        // wyszukiwarka
        int sx = px + 100, sw = bx - 10 - sx;
        boolean searchable = tab == Tab.ALL || tab == Tab.HUD || tab == Tab.GAME;
        round(g, sx, ty, sx + sw, ty + 20, 7, a(0xFF0B0617));
        roundOutline(g, sx, ty, sx + sw, ty + 20, 7, a(search.isEmpty() ? 0x1FFFFFFF : Theme.accent(170)));
        icon(g, new int[]{0b11100, 0b10100, 0b11100, 0b00010, 0b00001}, sx + 7, ty + 7, 1, a(MUTED));
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (search.isEmpty()) {
            text(g, searchable ? "Szukaj modułu…" : "Wyszukiwarka – w zakładkach z modułami", sx + 17, ty + 6, DIM, false);
        } else {
            String fit = search;
            while (g.width(fit) > sw - 30 && fit.length() > 1) fit = fit.substring(1);
            text(g, fit, sx + 17, ty + 6, TEXT, false);
            if (blink) g.fill(sx + 18 + g.width(fit), ty + 5, sx + 19 + g.width(fit), ty + 15, a(TEXT));
        }
        hits.add(new Hit(sx, ty, sx + sw, ty + 20, () -> { if (!searchable) switchTab(Tab.ALL); }, "Zacznij pisać, żeby wyszukać moduł"));

        // ── kategorie (pigułki) z przesuwanym podświetleniem ──
        int pyRow = py + 40;
        int pxp = px + 12;
        Tab[] tabs = Tab.values();
        int[] pillX = new int[tabs.length];
        int[] pillW = new int[tabs.length];
        int cur = pxp;
        for (int i = 0; i < tabs.length; i++) {
            pillX[i] = cur;
            pillW[i] = g.width(tabs[i].label) + 22;
            cur += pillW[i] + 6;
        }
        float ix = anim("pillX", pillX[tab.ordinal()], 16f);
        float iw = anim("pillW", pillW[tab.ordinal()], 16f);
        round(g, (int) ix, pyRow, (int) (ix + iw), pyRow + 18, 9, a(0xFF7C3AED));
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            boolean hover = mx >= pillX[i] && mx < pillX[i] + pillW[i] && my >= pyRow && my < pyRow + 18;
            float hv = anim("pill" + i, hover && t != tab ? 1 : 0, 14f);
            if (t != tab) round(g, pillX[i], pyRow, pillX[i] + pillW[i], pyRow + 18, 9, a(lerpColor(0x12FFFFFF, 0x26FFFFFF, hv)));
            icon(g, t.icon, pillX[i] + 7, pyRow + 7, 1, a(t == tab ? 0xFFFFFFFF : MUTED));
            text(g, t.label, pillX[i] + 15, pyRow + 5, t == tab ? 0xFFFFFFFF : lerpColor(MUTED, TEXT, hv), false);
            hits.add(new Hit(pillX[i], pyRow, pillX[i] + pillW[i], pyRow + 18, () -> switchTab(t), t.subtitle));
        }
        String chip = chipText();
        if (!chip.isEmpty()) text(g, chip, px + PW - 12 - g.width(chip), pyRow + 5, DIM, false);

        // ── treść ──
        int cx = px + 12;
        int cw = PW - 24;
        int top = py + 66;
        int ch = PH - 66 - 22;
        int sc = (int) anim("scroll", scroll, 18f);
        g.scissor(cx - 2, top, cx + cw + 2, top + ch);
        int contentH;
        switch (tab) {
            case OPTIONS: contentH = renderOptions(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            case AUTOTEXT: contentH = renderAutoText(g, mx, my, cx, top - sc, cw, top, top + ch); break;
            default: contentH = renderModules(g, mx, my, cx, top - sc, cw, top, top + ch, tab);
        }
        g.noScissor();
        maxScroll = Math.max(0, contentH - ch);
        if (maxScroll > 0) {
            int barH = Math.max(16, ch * ch / Math.max(1, contentH));
            int barY = top + (int) ((ch - barH) * (sc / (float) maxScroll));
            round(g, cx + cw + 4, barY, cx + cw + 7, barY + barH, 1, a(Theme.accent(120)));
        }

        // stopka
        g.fill(cx, py + PH - 21, cx + cw, py + PH - 20, a(0x16FFFFFF));
        String tip = tab == Tab.AUTOTEXT
                ? "Kliknij pole i wpisz tekst. Klawisz: Sterowanie → Void Client."
                : "Kliknij kafelek, aby włączyć lub wyłączyć. Pisz, aby szukać.";
        for (Hit h : hits) if (h.tooltip != null && h.contains(mx, my)) tip = h.tooltip;
        String close = "Prawy Shift – zamknij";
        text(g, g.trim(tip, cw - g.width(close) - 10), cx, py + PH - 14, MUTED, false);
        text(g, close, cx + cw - g.width(close), py + PH - 14, DIM, false);
    }

    private String chipText() {
        if (tab == Tab.ALL || tab == Tab.HUD || tab == Tab.GAME) {
            List<Module> list = tab == Tab.ALL ? Modules.all() : Modules.byCategory(tab == Tab.HUD ? Module.Category.HUD : Module.Category.GAME);
            int on = 0;
            for (Module m : list) if (m.isEnabled()) on++;
            return on + "/" + list.size() + " aktywne";
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
        if (t == Tab.AUTOTEXT || t == Tab.OPTIONS) search = "";
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
        int cols = 4;
        int gap = 8;
        int cardW = (w - gap * (cols - 1)) / cols;
        int cardH = 80;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float st = stagger(i);
            int cx = x + (i % cols) * (cardW + gap);
            int cy = y + (i / cols) * (cardH + gap) + (int) ((1 - st) * 10);
            boolean visible = cy + cardH > clipTop && cy < clipBottom;
            boolean hover = visible && mx >= cx && mx < cx + cardW && my >= cy && my < cy + cardH && my >= clipTop && my < clipBottom;
            float hv = anim("h" + m.id, hover ? 1 : 0, 14f);
            float on = anim("t" + m.id, m.isEnabled() ? 1 : 0, 12f);
            int lift = (int) (hv * 2);
            int top = cy - lift;

            // karta
            round(g, cx, top, cx + cardW, top + cardH, 7, a(lerpColor(CARD, CARD_HOVER, hv), st));
            roundOutline(g, cx, top, cx + cardW, top + cardH, 7,
                    a(lerpColor(0x16FFFFFF, Theme.accent(200), Math.max(hv * 0.8f, on * 0.45f)), st));

            // ikona w kafelku z poświatą
            int tileS = 30;
            int tx = cx + (cardW - tileS) / 2;
            int tyy = top + 9;
            if (on > 0.02f) round(g, tx - 3, tyy - 3, tx + tileS + 3, tyy + tileS + 3, 9, a(Theme.accent((int) (50 * on)), st));
            round(g, tx, tyy, tx + tileS, tyy + tileS, 7, a(lerpColor(0xFF2A1F45, 0xFF7C3AED, on), st));
            g.gradient(tx + 2, tyy + 1, tx + tileS - 2, tyy + tileS / 2, a(0x22FFFFFF, st), a(0x00FFFFFF));
            int[] ic = ICONS.getOrDefault(m.id, Tab.ALL.icon);
            icon(g, ic, tx + 5, tyy + 5, 4, a(lerpColor(0xFFB9A6D6, 0xFFFFFFFF, on), st));

            // nazwa
            String name = g.trim(m.name, cardW - 8);
            text(g, name, cx + (cardW - g.width(name)) / 2, top + 45, TEXT, false, st);

            // pasek Włączony / Wyłączony na dole karty
            int by = top + cardH - 20;
            round(g, cx + 5, by, cx + cardW - 5, by + 15, 5,
                    a(lerpColor(lerpColor(0xFF231838, 0xFF2E2150, hv), lerpColor(0xFF6D28D9, 0xFF8B45F5, hv), on), st));
            String state = m.isEnabled() ? "WŁĄCZONY" : "WYŁĄCZONY";
            text(g, state, cx + (cardW - g.width(state)) / 2, by + 4, lerpColor(DIM, 0xFFFFFFFF, on), false, st);

            if (visible) {
                hits.add(new Hit(cx, Math.max(top, clipTop), cx + cardW, Math.min(top + cardH, clipBottom), () -> {
                    m.toggle();
                    Config.save();
                }, m.name + " – " + m.description));
            }
        }
        int rows = (list.size() + cols - 1) / cols;
        return rows * (cardH + gap) - gap;
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
        optionRow(g, mx, my, x, y + (h + gap) * row, w, h, row++, "Pozycje HUD", "Resetuj",
                () -> { for (HudModule m : Modules.hud()) { m.x = m.defaultX; m.y = m.defaultY; } }, clipTop, clipBottom, null);
        int ty = y + (h + gap) * row + 2;
        if (!fpsStatus.isEmpty()) { text(g, g.trim(fpsStatus, w), x, ty, Theme.accent(), false); ty += 11; }
        String st = SkinUploader.status;
        if (st != null && !st.isEmpty()) { text(g, g.trim(st, w), x, ty, Theme.accent(), false); ty += 11; }
        text(g, g.trim("Klawisze: Opcje → Sterowanie → Void Client", w), x, ty, DIM, false);
        return ty - y + 12;
    }

    private void optionRow(Gfx g, int mx, int my, int x, int y, int w, int h, int idx, String label, String value,
                           Runnable action, int clipTop, int clipBottom, String tip) {
        float st = stagger(idx);
        y += (int) ((1 - st) * 8);
        boolean visible = y + h > clipTop && y < clipBottom;
        boolean hover = visible && mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        float hv = anim("o" + idx, hover ? 1 : 0, 14f);
        round(g, x, y, x + w, y + h, 6, a(lerpColor(CARD, CARD_HOVER, hv), st));
        roundOutline(g, x, y, x + w, y + h, 6, a(hv > 0.02f ? Theme.accent((int) (hv * 110)) : 0x14FFFFFF, st));
        text(g, label, x + 10, y + (h - 8) / 2, TEXT, false, st);
        int vw = g.width(value) + 16;
        int vx = x + w - vw - 5;
        round(g, vx, y + 4, x + w - 5, y + h - 4, 5, a(lerpColor(0xFF7C3AED, 0xFF9F67FF, hv), st));
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
        for (Hit h : new ArrayList<>(hits)) {
            if (h.contains(mx, my)) {
                h.action.run();
                return true;
            }
        }
        if (before >= 0) Config.save();
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
        Config.save();
    }

    /** Wpisany znak (pole AutoText). */
    public boolean charTyped(int codepoint) {
        if (editing < 0) {
            if (editHud || closingAt >= 0 || tab == Tab.AUTOTEXT || tab == Tab.OPTIONS) return false;
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
