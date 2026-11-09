package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

import java.util.ArrayList;
import java.util.List;

/**
 * Waypointy: zapisane punkty w świecie ze znacznikiem na ekranie (nazwa + odległość).
 * Każdy punkt należy do serwera/świata i wymiaru, w którym go ustawiono.
 * Znacznik liczymy sami z pozycji i kierunku kamery – bez kodu zależnego od wersji gry.
 */
public final class Waypoints {
    public static final int[] COLORS = {0xFFC084FC, 0xFFF0ABFC, 0xFF60A5FA, 0xFF4ADE80, 0xFFFACC15, 0xFFFB923C, 0xFFF87171, 0xFFFFFFFF};
    public static final int MAX_NAME = 24;

    /** Pojedynczy punkt (pola publiczne – zapisuje je Gson). */
    public static final class Point {
        public String name;
        public int x, y, z;
        public int color;
        public boolean visible = true;
        public String world;

        public Point() {}

        Point(String name, int x, int y, int z, int color, String world) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.world = world;
        }

        public int argb() { return COLORS[Math.floorMod(color, COLORS.length)]; }
    }

    public static final List<Point> ALL = new ArrayList<>();

    // pozycja oka z dwóch ostatnich ticków – do płynnego ruchu znaczników przy wysokich FPS
    private static double px, py, pz, cx, cy, cz;
    private static long tickAt;
    private static boolean hasPos;

    private Waypoints() {}

    /** Klucz obecnego świata: serwer + wymiar. Pusty w menu. */
    public static String worldKey(Game g) {
        if (!g.hasPlayer()) return "";
        String srv;
        try { srv = g.server(); } catch (Throwable t) { srv = "?"; }
        String dim;
        try { dim = g.dimension(); } catch (Throwable t) { dim = "?"; }
        return srv + "@" + dim;
    }

    public static List<Point> here(Game g) {
        String key = worldKey(g);
        List<Point> out = new ArrayList<>();
        if (key.isEmpty()) return out;
        for (Point p : ALL) if (key.equals(p.world)) out.add(p);
        return out;
    }

    /** Nowy punkt w miejscu gracza. */
    public static Point addHere(Game g) {
        if (!g.hasPlayer()) return null;
        int n = here(g).size() + 1;
        Point p = new Point("Punkt " + n, g.x(), g.y(), g.z(), (n - 1) % COLORS.length, worldKey(g));
        ALL.add(p);
        Config.save();
        return p;
    }

    public static void remove(Point p) {
        ALL.remove(p);
        Config.save();
    }

    public static void tick(Game g) {
        if (!g.hasPlayer()) {
            hasPos = false;
            return;
        }
        double[] e = g.eye();
        if (!hasPos) {
            px = e[0]; py = e[1]; pz = e[2];
        } else {
            px = cx; py = cy; pz = cz;
        }
        cx = e[0]; cy = e[1]; cz = e[2];
        tickAt = System.nanoTime();
        hasPos = true;
    }

    public static int distance(Game g, Point p) {
        double dx = p.x + 0.5 - g.x(), dy = p.y - g.y(), dz = p.z + 0.5 - g.z();
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    // ───────────── Znaczniki na ekranie ─────────────

    public static void render(Gfx g, Game game, int w, int h) {
        if (!Modules.WAYPOINTS.isEnabled() || !hasPos) return;
        List<Point> list = here(game);
        if (list.isEmpty()) return;

        float t = Math.min(1f, (System.nanoTime() - tickAt) / 50_000_000f);
        double ex = px + (cx - px) * t, ey = py + (cy - py) * t, ez = pz + (cz - pz) * t;

        float yaw = Freelook.active() ? Freelook.yaw() : game.playerYaw();
        float pitch = Freelook.active() ? Freelook.pitch() : game.playerPitch();
        int persp = game.perspective();
        double[] f = forward(yaw, pitch);
        // kamera w trzeciej osobie stoi ~4 bloki za (albo przed) postacią
        if (persp == 1) { ex -= f[0] * 4; ey -= f[1] * 4; ez -= f[2] * 4; }
        if (persp == 2) {
            ex += f[0] * 4; ey += f[1] * 4; ez += f[2] * 4;
            yaw += 180f;
            pitch = -pitch;
            f = forward(yaw, pitch);
        }
        double yr = Math.toRadians(yaw);
        double[] r = {-Math.cos(yr), 0, -Math.sin(yr)};           // w prawo od kamery
        double[] u = {r[1] * f[2] - r[2] * f[1], r[2] * f[0] - r[0] * f[2], r[0] * f[1] - r[1] * f[0]}; // w górę
        double fov = Math.max(10, Math.min(170, game.fov()));
        double scale = (h / 2.0) / Math.tan(Math.toRadians(fov) / 2);

        long now = System.currentTimeMillis();
        for (Point p : list) {
            if (!p.visible) continue;
            double dx = p.x + 0.5 - ex, dy = p.y + 1.0 - ey, dz = p.z + 0.5 - ez;
            double zc = dx * f[0] + dy * f[1] + dz * f[2];
            if (zc < 0.1) continue; // za kamerą
            double xc = dx * r[0] + dy * r[1] + dz * r[2];
            double yc = dx * u[0] + dy * u[1] + dz * u[2];
            int sx = (int) Math.round(w / 2.0 + xc / zc * scale);
            int sy = (int) Math.round(h / 2.0 - yc / zc * scale);
            if (sx < -40 || sx > w + 40 || sy < -40 || sy > h + 40) continue;
            int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            marker(g, sx, sy, p, dist, now, w);
        }
    }

    private static double[] forward(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new double[]{-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    private static void marker(Gfx g, int x, int y, Point p, int dist, long now, int screenW) {
        int c = p.argb();
        int glow = (c & 0x00FFFFFF) | 0x50000000;
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 300.0 + p.x);
        // świecący romb
        int s = 5;
        for (int i = -s - 2; i <= s + 2; i++) {
            int half = s + 2 - Math.abs(i);
            g.fill(x - half, y + i, x + half + 1, y + i + 1, (int) (0x30 + 0x30 * pulse) << 24 | (c & 0x00FFFFFF));
        }
        for (int i = -s; i <= s; i++) {
            int half = s - Math.abs(i);
            g.fill(x - half, y + i, x + half + 1, y + i + 1, i < 0 ? c : darker(c));
        }
        g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
        // pionowa linia w dół jak promień
        g.fill(x, y + s + 2, x + 1, y + s + 14, glow);

        String label = p.name + "  " + dist + " m";
        int tw = g.width(label);
        int lx = Math.max(2, Math.min(screenW - tw - 10, x - tw / 2 - 4)), ly = Math.max(2, y - s - 16);
        g.fill(lx, ly, lx + tw + 8, ly + 12, 0xB0100820);
        g.fill(lx, ly + 11, lx + tw + 8, ly + 12, c);
        g.text(p.name, lx + 4, ly + 2, 0xFFFFFFFF, true);
        g.text(dist + " m", lx + 4 + g.width(p.name + "  "), ly + 2, c, true);
    }

    private static int darker(int c) {
        int r = (c >> 16 & 0xFF) * 3 / 4, gg = (c >> 8 & 0xFF) * 3 / 4, b = (c & 0xFF) * 3 / 4;
        return 0xFF000000 | r << 16 | gg << 8 | b;
    }
}
