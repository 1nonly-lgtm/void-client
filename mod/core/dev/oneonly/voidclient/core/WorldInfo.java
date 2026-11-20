package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

import java.util.List;

/** Informacje rysowane w świecie: licznik TNT, postęp kopania i nasycenie przy pasku głodu. */
public final class WorldInfo {
    private WorldInfo() {}

    // ───────────── TNT ─────────────

    public static void tnt(Gfx g, Game game, int w, int h) {
        List<double[]> list;
        try { list = game.tntFuses(); } catch (Throwable t) { return; }
        if (list.isEmpty()) return;
        double[] eye = game.eye();
        for (double[] t : list) {
            double dx = t[0] - eye[0], dy = t[1] - eye[1], dz = t[2] - eye[2];
            if (dx * dx + dy * dy + dz * dz > 64 * 64) continue;
            int[] p = Waypoints.project(game, t[0], t[1] + 1.35, t[2], w, h);
            if (p == null) continue;
            float sec = (float) Math.max(0, t[3]) / 20f;
            String s = String.format(java.util.Locale.ROOT, "%.1f s", sec);
            // kolor: zielony (4 s) → żółty → czerwony (0 s), miga pod koniec
            float k = Math.max(0f, Math.min(1f, sec / 4f));
            int col = k > 0.5f ? mix(0xFFFACC15, 0xFF4ADE80, (k - 0.5f) * 2f) : mix(0xFFF87171, 0xFFFACC15, k * 2f);
            if (sec < 1f && (System.currentTimeMillis() / 120) % 2 == 0) col = 0xFFFFFFFF;
            int tw = g.uiWidth(s, true);
            int x = p[0] - tw / 2 - 5, y = p[1] - 6;
            g.fill(x, y, x + tw + 10, y + 13, 0xC0100820);
            g.fill(x, y + 12, x + tw + 10, y + 13, col);
            g.uiText(s, x + 5, y + 3, col, true);
            if (Modules.TNT.radius) {
                // promień wybuchu (4 bloki) – kropki na ziemi
                for (int i = 0; i < 24; i++) {
                    double a = i / 24.0 * Math.PI * 2;
                    int[] q = Waypoints.project(game, t[0] + Math.cos(a) * 4, t[1], t[2] + Math.sin(a) * 4, w, h);
                    if (q != null) g.fill(q[0] - 1, q[1] - 1, q[0] + 1, q[1] + 1, (col & 0x00FFFFFF) | 0xB0000000);
                }
            }
        }
    }

    // ───────────── Kopanie ─────────────

    private static float lastProgress, speed, shown;
    private static long lastAt;
    private static int lastX = Integer.MIN_VALUE, lastY, lastZ;
    private static String lastText = "";

    public static void mining(Gfx g, Game game, int w, int h) {
        float[] m;
        try { m = game.mining(); } catch (Throwable t) { m = null; }
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastAt) / 1000f);
        lastAt = now;
        boolean active = m != null && m[0] > 0f;
        shown += ((active ? 1 : 0) - shown) * Math.min(1f, dt * (active ? 14f : 6f));
        if (active) {
            int bx = (int) m[1], by = (int) m[2], bz = (int) m[3];
            if (bx != lastX || by != lastY || bz != lastZ || m[0] < lastProgress) { speed = 0; lastProgress = 0; }
            if (m[0] > lastProgress && dt > 0) speed += ((m[0] - lastProgress) / dt - speed) * 0.25f;
            lastProgress = m[0];
            lastX = bx; lastY = by; lastZ = bz;
            int pct = Math.min(100, Math.round(m[0] * 100));
            if (Modules.MINING.time && speed > 0.01f) {
                float left = (1f - m[0]) / speed;
                lastText = pct + "%  ·  " + String.format(java.util.Locale.ROOT, "%.1f s", Math.max(0, left));
            } else {
                lastText = pct + "%";
            }
        }
        if (shown < 0.03f || lastText.isEmpty()) return;
        int alpha = (int) (255 * shown);
        int tw = g.uiWidth(lastText, true);
        int x, y;
        int[] above = Modules.MINING.aboveBlock && lastX != Integer.MIN_VALUE ? Waypoints.project(game, lastX + 0.5, lastY + 1.25, lastZ + 0.5, w, h) : null;
        if (above != null) { x = above[0] - tw / 2; y = above[1] - 6; }
        else { x = w / 2 - tw / 2; y = h / 2 + 10; }
        g.fill(x - 5, y - 3, x + tw + 5, y + 10 + (Modules.MINING.bar ? 4 : 0), (int) (0xB0 * shown) << 24 | 0x100820);
        g.uiText(lastText, x, y, alpha << 24 | (Theme.accent() & 0xFFFFFF), true);
        if (Modules.MINING.bar) {
            int bw = tw + 6;
            g.fill(x - 3, y + 10, x - 3 + bw, y + 12, (alpha / 3) << 24 | 0xFFFFFF);
            g.fill(x - 3, y + 10, x - 3 + (int) (bw * Math.min(1f, lastProgress)), y + 12, alpha << 24 | (Theme.accent() & 0xFFFFFF));
        }
    }

    // ───────────── Nasycenie ─────────────

    public static void saturation(Gfx g, Game game, int w, int h) {
        float sat;
        try { sat = game.saturation(); } catch (Throwable t) { return; }
        if (sat < 0) return;
        // ikony jedzenia: od prawej, 10 sztuk po 8 px, 39 px nad dołem ekranu (jak w grze)
        int right = w / 2 + 91, y = h - 39;
        int col = 0xFFFACC15;
        for (int i = 0; i < 10; i++) {
            float part = Math.max(0f, Math.min(1f, (sat - i * 2) / 2f));
            if (part <= 0) break;
            int x2 = right - i * 8, x1 = x2 - 9;
            int fillW = Math.round(9 * part);
            // złota obwódka ikony (część = ułamek nasycenia)
            g.fill(x2 - fillW, y, x2, y + 1, col);
            g.fill(x2 - fillW, y + 8, x2, y + 9, col);
            if (part >= 1f) g.fill(x1, y, x1 + 1, y + 9, col);
            g.fill(x2 - 1, y, x2, y + 9, col);
        }
    }

    private static int mix(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }
}
