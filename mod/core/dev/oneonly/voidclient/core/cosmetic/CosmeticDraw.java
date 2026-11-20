package dev.oneonly.voidclient.core.cosmetic;

import dev.oneonly.voidclient.core.Cosmetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rysowanie kosmetyków 3D (skrzydła, czapki, zwierzaki, efekty, rekwizyty emotek) – wspólne dla wszystkich wersji.
 * Liczy położenie każdej części w danej chwili i oddaje trójkąty do MeshSink.
 */
public final class CosmeticDraw {
    private CosmeticDraw() {}

    /** Pozycja postaci w tej klatce. Kąty w radianach (z modelu gracza). */
    public static final class Pose {
        public float headPitch, headYaw, headRoll;
        public boolean sneaking;
        public float age; // sekundy
    }

    private static final String WHITE = "voidclient:textures/cosmetics/white.png";

    public static void render(UUID player, Pose pose, MeshSink sink) {
        if (player == null) return;
        float[] em = Emotes.modelAngles(player);
        if (em != null) { pose.headPitch = em[0]; pose.headYaw = em[1]; pose.headRoll = em[2]; }
        Emotes.Playing e = Emotes.playing(player);
        if (e != null && e.prop != null) draw(e.prop, pose, sink, e);
        String[] worn = Cosmetics.worn(player);
        if (worn == null) return;
        String[] cats = Cosmetics.CATS3D;
        for (int i = 0; i < cats.length; i++) {
            CosmeticModel m = CosmeticModel.get(cats[i], worn[i]);
            if (m != null) draw(m, pose, sink, null);
        }
    }

    // ── macierze 4×4 (kolumnowo) ──

    static float[] ident() { return new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }

    static float[] mul(float[] a, float[] b) {
        float[] r = new float[16];
        for (int c = 0; c < 4; c++) for (int rr = 0; rr < 4; rr++) {
            float s = 0;
            for (int k = 0; k < 4; k++) s += a[k * 4 + rr] * b[c * 4 + k];
            r[c * 4 + rr] = s;
        }
        return r;
    }

    /** Macierz z pozycji, kwaternionu i skali (jak three.js compose). */
    static float[] compose(float[] t) {
        float x = t[3], y = t[4], z = t[5], w = t[6];
        float x2 = x + x, y2 = y + y, z2 = z + z, xx = x * x2, xy = x * y2, xz = x * z2, yy = y * y2, yz = y * z2, zz = z * z2, wx = w * x2, wy = w * y2, wz = w * z2;
        float sx = t[7], sy = t[8], sz = t[9];
        return new float[]{
                (1 - (yy + zz)) * sx, (xy + wz) * sx, (xz - wy) * sx, 0,
                (xy - wz) * sy, (1 - (xx + zz)) * sy, (yz + wx) * sy, 0,
                (xz + wy) * sz, (yz - wx) * sz, (1 - (xx + yy)) * sz, 0,
                t[0], t[1], t[2], 1};
    }

    static float[] rotX(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return new float[]{1, 0, 0, 0, 0, c, s, 0, 0, -s, c, 0, 0, 0, 0, 1}; }
    static float[] rotY(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return new float[]{c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 0, 0, 0, 1}; }
    static float[] rotZ(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return new float[]{c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }
    static float[] trans(float x, float y, float z) { float[] m = ident(); m[12] = x; m[13] = y; m[14] = z; return m; }
    static float[] scale(float x, float y, float z) { return new float[]{x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1}; }

    /** Z przestrzeni podglądu (stopy w 0, y w górę, przód +z) do modelu Minecrafta (szyja w 0, y w dół, przód -z). */
    static float[] anchorMatrix(String anchor, Pose p) {
        float[] conv = scale(1, -1, -1);
        if ("head".equals(anchor)) {
            float[] head = mul(trans(0, p.sneaking ? 4.2f : 0, 0), mul(rotZ(p.headRoll), mul(rotY(p.headYaw), rotX(p.headPitch))));
            return mul(head, conv);
        }
        float[] base = mul(trans(0, 24, 0), conv);
        if ("root".equals(anchor) && p.sneaking) return mul(trans(0, 3.2f, 0), mul(rotX(0.5f), base));
        return base;
    }

    private static float[] lerpFrame(float[][] fr, float pos) {
        if (fr.length == 1) return fr[0];
        int a = (int) Math.floor(pos) % fr.length, b = (a + 1) % fr.length;
        float k = pos - (float) Math.floor(pos);
        float[] x = fr[a], y = fr[b], r = new float[11];
        for (int i = 0; i < 11; i++) r[i] = x[i] + (y[i] - x[i]) * k;
        // kwaternion: najkrótsza droga + normalizacja
        float dot = x[3] * y[3] + x[4] * y[4] + x[5] * y[5] + x[6] * y[6];
        if (dot < 0) for (int i = 3; i < 7; i++) r[i] = x[i] - (y[i] + x[i]) * k;
        float len = (float) Math.sqrt(r[3] * r[3] + r[4] * r[4] + r[5] * r[5] + r[6] * r[6]);
        if (len > 1e-6f) for (int i = 3; i < 7; i++) r[i] /= len;
        r[10] = k < 0.5f ? x[10] : y[10];
        return r;
    }

    private static final class Batch {
        final String tex;
        final int mode;
        final List<float[]> quads = new ArrayList<>();
        Batch(String tex, int mode) { this.tex = tex; this.mode = mode; }
    }

    static void draw(CosmeticModel m, Pose pose, MeshSink sink, Emotes.Playing emote) {
        float t = emote != null ? emote.time() - 1f : pose.age;
        float pos = (t * CosmeticModel.FPS) % CosmeticModel.FRAMES;
        int fi = (int) pos;
        float[] anchor = anchorMatrix(emote != null ? "spin" : m.anchor, pose);
        float[][] world = new float[m.nodes.length][];
        boolean[] vis = new boolean[m.nodes.length];
        Map<String, Batch> batches = new HashMap<>();
        for (int i = 0; i < m.nodes.length; i++) {
            CosmeticModel.Node n = m.nodes[i];
            float[] tr = lerpFrame(n.tr, pos);
            float[] local = compose(tr);
            world[i] = n.parent < 0 ? mul(anchor, local) : mul(world[n.parent], local);
            vis[i] = tr[10] > 0.5f && (n.parent < 0 || vis[n.parent]);
            if (!vis[i] || n.mats == null) continue;
            int f = n.color.length == 1 ? 0 : fi % n.color.length;
            if (n.sprite) {
                sprite(world[i], n, f, batches);
                continue;
            }
            if (n.geom < 0) continue;
            CosmeticModel.Geom g = m.geoms[n.geom];
            float[] w = world[i];
            int tris = g.m.length;
            for (int k = 0; k < tris; k++) {
                int mi = Math.min(g.m[k], n.mats.length - 1);
                CosmeticModel.Mat mat = n.mats[mi];
                float alpha = n.alpha[f][mi];
                if (alpha <= 0.01f) continue;
                String tex = n.tex[f][mi] == null ? WHITE : "voidclient:textures/cosmetics/" + n.tex[f][mi] + ".png";
                int mode = mat.add ? 2 : mat.basic ? 1 : 0;
                Batch b = batches.computeIfAbsent(tex + "|" + mode, key -> new Batch(tex, mode));
                int col = ((int) (alpha * 255) & 0xFF) << 24 | (n.color[f][mi] & 0xFFFFFF);
                float[] q = new float[4 * 9];
                for (int v = 0; v < 3; v++) {
                    int idx = k * 3 + v;
                    float px = g.p[idx * 3], py = g.p[idx * 3 + 1], pz = g.p[idx * 3 + 2];
                    float nx = g.n[idx * 3], ny = g.n[idx * 3 + 1], nz = g.n[idx * 3 + 2];
                    q[v * 9] = w[0] * px + w[4] * py + w[8] * pz + w[12];
                    q[v * 9 + 1] = w[1] * px + w[5] * py + w[9] * pz + w[13];
                    q[v * 9 + 2] = w[2] * px + w[6] * py + w[10] * pz + w[14];
                    q[v * 9 + 3] = g.u[idx * 2];
                    q[v * 9 + 4] = g.u[idx * 2 + 1];
                    float tnx = w[0] * nx + w[4] * ny + w[8] * nz, tny = w[1] * nx + w[5] * ny + w[9] * nz, tnz = w[2] * nx + w[6] * ny + w[10] * nz;
                    float l = (float) Math.sqrt(tnx * tnx + tny * tny + tnz * tnz);
                    if (l < 1e-6f) l = 1;
                    q[v * 9 + 5] = tnx / l;
                    q[v * 9 + 6] = tny / l;
                    q[v * 9 + 7] = tnz / l;
                    q[v * 9 + 8] = Float.intBitsToFloat(col);
                }
                System.arraycopy(q, 18, q, 27, 9); // trójkąt jako czworokąt (ostatni wierzchołek dwa razy)
                b.quads.add(q);
            }
        }
        for (Batch b : batches.values()) {
            sink.begin(b.tex, b.mode);
            for (float[] q : b.quads) for (int v = 0; v < 4; v++) {
                sink.vertex(q[v * 9], q[v * 9 + 1], q[v * 9 + 2], q[v * 9 + 3], q[v * 9 + 4], Float.floatToRawIntBits(q[v * 9 + 8]), q[v * 9 + 5], q[v * 9 + 6], q[v * 9 + 7]);
            }
            sink.end();
        }
    }

    /** Duszek (cząsteczka): dwie skrzyżowane płaszczyzny w środku węzła. */
    private static void sprite(float[] w, CosmeticModel.Node n, int f, Map<String, Batch> batches) {
        float alpha = n.alpha[f][0];
        if (alpha <= 0.01f || n.tex[f][0] == null) return;
        String tex = "voidclient:textures/cosmetics/" + n.tex[f][0] + ".png";
        int mode = n.mats[0].add ? 2 : 1;
        Batch b = batches.computeIfAbsent(tex + "|" + mode, key -> new Batch(tex, mode));
        int col = ((int) (alpha * 255) & 0xFF) << 24 | (n.color[f][0] & 0xFFFFFF);
        float cx = w[12], cy = w[13], cz = w[14];
        float sx = (float) Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2]) / 2, sy = (float) Math.sqrt(w[4] * w[4] + w[5] * w[5] + w[6] * w[6]) / 2;
        float[][] axes = {{1, 0, 0}, {0, 0, 1}};
        for (float[] ax : axes) {
            float[] q = new float[36];
            float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
            for (int v = 0; v < 4; v++) {
                float[] c = corners[v];
                q[v * 9] = cx + ax[0] * c[0] * sx;
                q[v * 9 + 1] = cy - c[1] * sy;
                q[v * 9 + 2] = cz + ax[2] * c[0] * sx;
                q[v * 9 + 3] = c[2];
                q[v * 9 + 4] = c[3];
                q[v * 9 + 5] = ax[2];
                q[v * 9 + 6] = 0;
                q[v * 9 + 7] = -ax[0];
                q[v * 9 + 8] = Float.intBitsToFloat(col);
            }
            b.quads.add(q);
        }
    }
}
