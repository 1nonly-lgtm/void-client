package dev.oneonly.voidclient.core;

/** Własny celownik i kolor obramowania bloku (ustawienia w zakładce „Celownik”). */
public final class Look {
    public static final String[] SHAPES = {"Krzyż", "Krzyż z przerwą", "Kropka", "Kwadrat", "Iks", "Okrąg"};
    public static final String[] COLOR_NAMES = {"Akcent", "Biały", "Fioletowy", "Różowy", "Zielony", "Czerwony", "Żółty", "Błękitny", "Czarny"};
    private static final int[] COLORS = {0, 0xFFFFFFFF, 0xFFA855F7, 0xFFF472B6, 0xFF4ADE80, 0xFFF87171, 0xFFFACC15, 0xFF38BDF8, 0xFF000000};

    public static int shape = 0;
    public static int color = 1;
    public static int size = 5;
    public static boolean dot = false;
    public static boolean outline = true;

    public static final String[] OUTLINE_NAMES = {"Akcent", "Biały", "Fioletowy", "Różowy", "Zielony", "Czerwony", "Żółty", "Błękitny", "Czarny"};
    public static int outlineColor = 0;

    private Look() {}

    public static int crossColor() { return color == 0 ? Theme.accent() : COLORS[color]; }

    /** Kolor obramowania bloku (ARGB, lekko przezroczysty jak w grze). */
    public static int outlineArgb() {
        int c = outlineColor == 0 ? Theme.accent() : COLORS[outlineColor];
        return (c & 0x00FFFFFF) | 0xCC000000;
    }

    /** Rysuje celownik na środku ekranu. */
    public static void drawCrosshair(Gfx g, int screenW, int screenH) {
        int cx = screenW / 2, cy = screenH / 2;
        draw(g, cx, cy, crossColor());
    }

    /** Podgląd w menu i właściwy celownik – (cx, cy) to środek. Najpierw czarny obrys, potem kolor. */
    public static void draw(Gfx g, int cx, int cy, int c) {
        if (outline) shapes(g, cx, cy, 0xC0000000, 1);
        shapes(g, cx, cy, c, 0);
    }

    /** grow = 1 rysuje każdy element o piksel większy (obrys). */
    private static void shapes(Gfx g, int cx, int cy, int c, int grow) {
        int s = size;
        switch (shape) {
            case 0:
                bar(g, cx - s, cy, cx + s + 1, cy + 1, c, grow);
                bar(g, cx, cy - s, cx + 1, cy + s + 1, c, grow);
                break;
            case 1: {
                int gap = 2;
                bar(g, cx - s - gap, cy, cx - gap, cy + 1, c, grow);
                bar(g, cx + gap + 1, cy, cx + s + gap + 1, cy + 1, c, grow);
                bar(g, cx, cy - s - gap, cx + 1, cy - gap, c, grow);
                bar(g, cx, cy + gap + 1, cx + 1, cy + s + gap + 1, c, grow);
                break;
            }
            case 2:
                bar(g, cx - 1, cy - 1, cx + 2, cy + 2, c, grow);
                break;
            case 3: {
                int r = Math.max(2, s - 1);
                bar(g, cx - r, cy - r, cx + r + 1, cy - r + 1, c, grow);
                bar(g, cx - r, cy + r, cx + r + 1, cy + r + 1, c, grow);
                bar(g, cx - r, cy - r + 1, cx - r + 1, cy + r, c, grow);
                bar(g, cx + r, cy - r + 1, cx + r + 1, cy + r, c, grow);
                break;
            }
            case 4:
                for (int i = -s; i <= s; i++) {
                    bar(g, cx + i, cy + i, cx + i + 1, cy + i + 1, c, grow);
                    bar(g, cx + i, cy - i, cx + i + 1, cy - i + 1, c, grow);
                }
                break;
            default: {
                int r = Math.max(3, s);
                for (int a = 0; a < 64; a++) {
                    double t = a / 64.0 * Math.PI * 2;
                    int px = cx + (int) Math.round(Math.cos(t) * r), py = cy + (int) Math.round(Math.sin(t) * r);
                    bar(g, px, py, px + 1, py + 1, c, grow);
                }
            }
        }
        if (dot && shape != 2) bar(g, cx, cy, cx + 1, cy + 1, c, grow);
    }

    private static void bar(Gfx g, int x1, int y1, int x2, int y2, int c, int grow) {
        g.fill(x1 - grow, y1 - grow, x2 + grow, y2 + grow, c);
    }
}
