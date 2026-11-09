package dev.oneonly.voidclient.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Ekran wczytywania Void Client: zastępuje planszę „Mojang Studios” przy starcie gry
 * oraz tło ekranów łączenia z serwerem i wczytywania świata.
 * Rysuje wyłącznie prostokątami (fill), bo przy starcie gry tekstury i czcionki
 * mogą jeszcze nie być wczytane – dzięki temu działa w każdej wersji i od pierwszej klatki.
 */
public final class LoadingArt {
    private LoadingArt() {}

    // ───────────── Plansza startowa (zamiast „Mojang Studios”) ─────────────

    private static Class<?> fieldsOf;
    private static Field progressF, completeF, startF, reloadingF;

    /**
     * Rysuje planszę na nakładce wczytywania gry. Postęp i zanikanie czytamy z pól nakładki
     * po typach (float = postęp, pierwsze long = koniec, drugie long = start, boolean = przeładowanie),
     * więc działa bez względu na nazwy pól w danej wersji gry.
     */
    public static void splash(Gfx g, Object overlay, int w, int h) {
        float progress = -1f, alpha = 1f;
        try {
            lookup(overlay.getClass());
            long now = System.nanoTime() / 1_000_000L;
            long complete = completeF != null ? completeF.getLong(overlay) : -1L;
            long start = startF != null ? startF.getLong(overlay) : -1L;
            boolean reloading = reloadingF != null && reloadingF.getBoolean(overlay);
            if (progressF != null) progress = progressF.getFloat(overlay);
            float f = complete > -1L ? (now - complete) / 1000f : -1f;
            float s = start > -1L ? (now - start) / 500f : -1f;
            if (f >= 1f) alpha = 1f - clamp(f - 1f);
            else if (reloading) alpha = clamp(s);
        } catch (Throwable ignored) {
            // brak pól – zostaje pełna plansza z animowanym paskiem
        }
        if (alpha <= 0.01f) return;
        draw(g, w, h, alpha, progress, null, false);
    }

    private static void lookup(Class<?> c) {
        if (c == fieldsOf) return;
        fieldsOf = c;
        progressF = completeF = startF = reloadingF = null;
        List<Field> longs = new ArrayList<>();
        for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            Class<?> t = f.getType();
            try { f.setAccessible(true); } catch (Throwable e) { continue; }
            if (t == float.class && progressF == null) progressF = f;
            else if (t == long.class) longs.add(f);
            else if (t == boolean.class && reloadingF == null) reloadingF = f;
        }
        if (longs.size() >= 1) completeF = longs.get(0);
        if (longs.size() >= 2) startF = longs.get(1);
    }

    // ───────────── Ekrany łączenia z serwerem / wczytywania świata ─────────────

    /** Pełne tło z logo i podpisem – rysowane na ekranach wczytywania (przyciski dorysowuje adapter). */
    public static void screen(Gfx g, int w, int h, String title) {
        String t = title == null || title.isBlank() ? "Wczytywanie świata" : title;
        draw(g, w, h, 1f, -1f, t, true);
    }

    // ───────────── Tło menu (główne menu, Multiplayer, Opcje… gdy nie ma świata) ─────────────

    /** Gwiezdna pustka z mgławicami i spadającą gwiazdą – zamiast panoramy i ziemi w menu gry. */
    public static void menu(Gfx g, int w, int h) {
        long now = System.currentTimeMillis();
        g.gradient(0, 0, w, h, 0xFF07040F, 0xFF170A2E);
        // mgławice – duże, bardzo przezroczyste plamy, powoli "oddychają"
        float b = 0.5f + 0.5f * (float) Math.sin(now / 2600.0);
        nebula(g, (int) (w * 0.18), (int) (h * 0.3), Math.max(w, h) / 4, 0xFF7C3AED, 0.05f + 0.02f * b);
        nebula(g, (int) (w * 0.85), (int) (h * 0.75), Math.max(w, h) / 3, 0xFFA855F7, 0.045f + 0.02f * (1 - b));
        nebula(g, (int) (w * 0.6), (int) (h * 0.12), Math.max(w, h) / 6, 0xFFD946EF, 0.03f);
        stars(g, w, h, 1f, now, 140);
        // kula pustki w rogu
        int r = Math.max(18, Math.min(w, h) / 7);
        int ox = w - r - Math.max(10, w / 14), oy = h - r - Math.max(30, h / 7);
        circle(g, ox, oy, r + Math.max(3, r / 6), 0x14A855F7);
        circle(g, ox, oy, r, 0x507C3AED);
        circle(g, ox, oy, r - Math.max(2, r / 10), 0x602A0A55);
        circle(g, ox, oy, r - Math.max(4, r / 5), 0x700B0418);
        // spadająca gwiazda co ~7 s
        long t = now % 7000;
        if (t < 900) {
            float p = t / 900f;
            int sx = (int) (w * (0.95f - 0.5f * p)), sy = (int) (h * (0.05f + 0.25f * p));
            int fade = (int) (255 * (p < 0.2f ? p / 0.2f : 1f - (p - 0.2f) / 0.8f));
            for (int i = 0; i < 14; i++) {
                int al = Math.max(0, fade - i * 18);
                g.fill(sx + i * 2, sy - i, sx + i * 2 + 2, sy - i + 1, al << 24 | 0xF0ABFC);
            }
            g.fill(sx - 1, sy - 1, sx + 2, sy + 2, fade << 24 | 0xFFFFFF);
        }
    }

    /** Miękka plama: wiele cienkich warstw (co 2 piksele w pionie, żeby było lekko dla FPS). */
    private static void nebula(Gfx g, int cx, int cy, int r, int rgb, float a) {
        int layers = 10;
        for (int k = layers; k >= 1; k--) {
            int rr = r * k / layers;
            int col = mul(rgb, a * 0.45f);
            if ((col >>> 24) == 0) col = 1 << 24 | (rgb & 0xFFFFFF);
            for (int dy = -rr; dy <= rr; dy += 2) {
                int half = (int) Math.sqrt((double) rr * rr - (double) dy * dy);
                g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 2, col);
            }
        }
    }

    private static void stars(Gfx g, int w, int h, float a, long now, int count) {
        for (int i = 0; i < count; i++) {
            int sx = (int) (hash(i * 7 + 1) % Math.max(1, w));
            int sy = (int) (hash(i * 13 + 5) % Math.max(1, h));
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(now / 600.0 + i * 1.7));
            int size = i % 9 == 0 ? 2 : 1;
            int col = i % 4 == 0 ? 0xFFF0ABFC : i % 3 == 0 ? 0xFFC084FC : 0xFFE9D5FF;
            g.fill(sx, sy, sx + size, sy + size, mul(col, a * tw * 0.8f));
        }
    }

    // ───────────── Rysowanie ─────────────

    private static final int BG_TOP = 0xFF07040F, BG_BOTTOM = 0xFF1A0B33;

    private static void draw(Gfx g, int w, int h, float a, float progress, String title, boolean text) {
        long now = System.currentTimeMillis();
        g.gradient(0, 0, w, h, mul(BG_TOP, a), mul(BG_BOTTOM, a));

        stars(g, w, h, a, now, 90); // stałe miejsca, migoczą w czasie

        int px = Math.max(2, Math.min(w, h) / 120);           // wielkość "piksela" napisu
        int cx = w / 2;
        int wordH = 7 * px;
        int r = Math.max(14, Math.min(w, h) / 9);             // promień kuli pustki
        int blockH = r * 2 + px * 6 + wordH + px * 4 + wordH;
        int cy = h / 2 - blockH / 2 + r - (text ? h / 20 : 0);

        orb(g, cx, cy, r, a, now);

        // napis VOID CLIENT z pikseli
        int y1 = cy + r + px * 6;
        word(g, "VOID", cx, y1, px, mul(0xFFF5E9FF, a), mul(0xFF6D28D9, a));
        int px2 = Math.max(1, px * 2 / 3);
        word(g, "CLIENT", cx, y1 + wordH + px * 3, px2, mul(0xFFC084FC, a), mul(0xFF3B1170, a));

        // pasek postępu
        int barW = Math.min(w * 2 / 5, 320), barH = Math.max(4, px * 2);
        int bx = cx - barW / 2, by = y1 + wordH + px * 3 + 7 * px2 + px * 6;
        if (text) {
            int tw = g.width(title);
            g.text(title, cx - tw / 2, by, mul(0xFFE9D5FF, a), true);
            by += 14;
        }
        g.fill(bx - 1, by - 1, bx + barW + 1, by + barH + 1, mul(0xFF2A1450, a));
        g.fill(bx, by, bx + barW, by + barH, mul(0xFF0C0618, a));
        if (progress >= 0f) {
            int fw = (int) (barW * clamp(progress));
            g.gradient(bx, by, bx + fw, by + barH, mul(0xFFC084FC, a), mul(0xFF7C3AED, a));
            if (fw > 2) g.fill(bx + fw - 2, by - 1, bx + fw, by + barH + 1, mul(0xFFFFFFFF, a));
        } else {
            // brak danych o postępie – płynący fioletowy odcinek
            int seg = barW / 3;
            int off = (int) ((now / 4) % (barW + seg)) - seg;
            int x1 = Math.max(bx, bx + off), x2 = Math.min(bx + barW, bx + off + seg);
            if (x2 > x1) g.gradient(x1, by, x2, by + barH, mul(0xFFC084FC, a), mul(0xFF7C3AED, a));
        }
        if (text) {
            String by1 = "by: 1nonly $";
            g.text(by1, 6, h - 12, mul(0xFF8B7AA8, a), false);
        }
    }

    /** Kula pustki: ciemny środek, fioletowy brzeg, poświata i krążące drobinki. */
    private static void orb(Gfx g, int cx, int cy, int r, float a, long now) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 700.0);
        // poświata – kilka coraz większych, przezroczystych kręgów
        for (int k = 4; k >= 1; k--) {
            int rr = r + k * Math.max(3, r / 6);
            circle(g, cx, cy, rr, mul(0xFFA855F7, a * (0.05f + 0.03f * pulse) * (5 - k) / 2f));
        }
        circle(g, cx, cy, r, mul(0xFF7C3AED, a));
        circle(g, cx, cy, r - Math.max(2, r / 10), mul(0xFF2A0A55, a));
        circle(g, cx, cy, r - Math.max(4, r / 5), mul(0xFF0B0418, a));
        // jasny błysk na brzegu
        int hs = Math.max(2, r / 7);
        g.fill(cx - r / 2 - hs, cy - r / 2 - hs, cx - r / 2, cy - r / 2, mul(0xFFE9D5FF, a * 0.9f));
        // krążące drobinki
        for (int i = 0; i < 10; i++) {
            double ang = now / 900.0 + i * Math.PI * 2 / 10;
            double rad = r * 1.35 + Math.sin(now / 500.0 + i) * r * 0.08;
            int x = cx + (int) (Math.cos(ang) * rad), y = cy + (int) (Math.sin(ang) * rad * 0.45);
            int s = i % 3 == 0 ? 3 : 2;
            g.fill(x - s / 2, y - s / 2, x - s / 2 + s, y - s / 2 + s, mul(i % 2 == 0 ? 0xFFF0ABFC : 0xFFC084FC, a));
        }
    }

    private static void circle(Gfx g, int cx, int cy, int r, int argb) {
        if (r <= 0 || (argb >>> 24) == 0) return;
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, argb);
        }
    }

    // ───────────── Pikselowy napis ─────────────

    private static final String[] GLYPHS = {
        "V", "10001100011000110001010100101000100",
        "O", "01110100011000110001100011000101110",
        "I", "11111001000010000100001000010011111",
        "D", "11110100011000110001100011000111110",
        "C", "01111100001000010000100001000001111",
        "L", "10000100001000010000100001000011111",
        "E", "11111100001000011110100001000011111",
        "N", "10001110011010110011100011000110001",
        "T", "11111001000010000100001000010000100"
    };

    private static String glyph(char c) {
        for (int i = 0; i < GLYPHS.length; i += 2) if (GLYPHS[i].charAt(0) == c) return GLYPHS[i + 1];
        return null;
    }

    private static void word(Gfx g, String s, int cx, int y, int px, int color, int shadow) {
        int gap = px;
        int width = s.length() * 5 * px + (s.length() - 1) * gap;
        int x = cx - width / 2;
        for (int i = 0; i < s.length(); i++) {
            String bits = glyph(s.charAt(i));
            if (bits != null) {
                for (int row = 0; row < 7; row++) for (int col = 0; col < 5; col++) {
                    if (bits.charAt(row * 5 + col) != '1') continue;
                    int fx = x + col * px, fy = y + row * px;
                    g.fill(fx + px / 2 + 1, fy + px / 2 + 1, fx + px + px / 2 + 1, fy + px + px / 2 + 1, shadow);
                }
                for (int row = 0; row < 7; row++) for (int col = 0; col < 5; col++) {
                    if (bits.charAt(row * 5 + col) != '1') continue;
                    int fx = x + col * px, fy = y + row * px;
                    g.fill(fx, fy, fx + px, fy + px, color);
                }
            }
            x += 5 * px + gap;
        }
    }

    // ───────────── Pomocnicze ─────────────

    private static float clamp(float v) { return v < 0f ? 0f : v > 1f ? 1f : v; }

    private static long hash(long x) {
        x ^= x << 13; x ^= x >>> 7; x ^= x << 17;
        x = x * 0x9E3779B97F4A7C15L;
        return (x >>> 1) ^ (x >>> 33);
    }

    private static int mul(int argb, float a) {
        int al = Math.round((argb >>> 24) * Math.max(0f, Math.min(1f, a)));
        return al << 24 | (argb & 0x00FFFFFF);
    }
}
