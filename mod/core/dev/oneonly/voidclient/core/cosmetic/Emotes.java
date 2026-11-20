package dev.oneonly.voidclient.core.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emotki: wypieczone ruchy części postaci (głowa, ręce, nogi, całe ciało) i rekwizyty (hulajnoga).
 * Grają u Ciebie (koło pod G); trwają do ruchu (chyba że emotka działa w ruchu) albo do drugiego wciśnięcia.
 */
public final class Emotes {
    private Emotes() {}

    public static final class Data {
        public float[][] f; // klatki: rootPos(3), rootRot(3), head(3), armR(3), armL(3), legR(3), legL(3)
        public boolean move;
        public CosmeticModel prop;
    }

    public static final class Playing {
        public final String id;
        public final Data data;
        public final CosmeticModel prop;
        final long start;
        Playing(String id, Data data) { this(id, data, 0); }
        Playing(String id, Data data, long ageMs) { this.id = id; this.data = data; this.prop = data.prop; this.start = System.currentTimeMillis() - ageMs; }
        public float time() { return 1f + (System.currentTimeMillis() - start) / 1000f; }

        /** Obroty i przesunięcie w tej chwili (21 liczb). */
        public float[] pose() {
            float[][] f = data.f;
            float p = ((System.currentTimeMillis() - start) / 1000f * CosmeticModel.FPS) % f.length;
            int a = (int) p, b = (a + 1) % f.length;
            float k = p - a;
            float[] r = new float[f[a].length];
            for (int i = 0; i < r.length; i++) r[i] = f[a][i] + (f[b][i] - f[a][i]) * k;
            return r;
        }
    }

    private static Map<String, Data> all;
    private static final Map<UUID, Playing> PLAYING = new ConcurrentHashMap<>();

    public static synchronized Map<String, Data> all() {
        if (all != null) return all;
        all = new HashMap<>();
        try (InputStream in = Emotes.class.getResourceAsStream("/assets/voidclient/cosmetics/emotes.json")) {
            if (in == null) return all;
            JsonObject o = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String id : o.keySet()) {
                JsonObject e = o.getAsJsonObject(id);
                Data d = new Data();
                JsonArray fr = e.getAsJsonArray("f");
                d.f = new float[fr.size()][];
                for (int i = 0; i < d.f.length; i++) d.f[i] = CosmeticModel.floats(fr.get(i).getAsJsonArray());
                d.move = e.get("move").getAsInt() == 1;
                if (e.has("prop") && e.get("prop").isJsonObject()) {
                    JsonObject p = e.getAsJsonObject("prop");
                    p.addProperty("anchor", "spin");
                    d.prop = CosmeticModel.parse(p);
                }
                all.put(id, d);
            }
        } catch (Throwable ignored) {
        }
        return all;
    }

    private static boolean self(UUID player) {
        try { return player != null && player.equals(dev.oneonly.voidclient.core.VoidCore.game().selfUuid()); } catch (Throwable t) { return false; }
    }

    public static void play(UUID player, String id) {
        if (player == null) return;
        Data d = id == null ? null : all().get(id);
        Playing cur = PLAYING.get(player);
        if (d == null || cur != null && cur.id.equals(id)) { stop(player); return; } // drugi raz – stop
        PLAYING.put(player, new Playing(id, d));
        if (self(player)) dev.oneonly.voidclient.core.VoidNet.sendEmote(id);
    }

    public static void stop(UUID player) {
        if (player != null && PLAYING.remove(player) != null && self(player)) dev.oneonly.voidclient.core.VoidNet.sendEmote(null);
    }

    /** Emotka innego gracza Void z serwera (null – nic nie gra). */
    public static void setRemote(UUID player, String id, long ageMs) {
        if (player == null || self(player)) return;
        Data d = id == null ? null : all().get(id);
        if (d == null || ageMs > 10 * 60_000) { PLAYING.remove(player); return; }
        Playing cur = PLAYING.get(player);
        if (cur != null && cur.id.equals(id)) return;
        PLAYING.put(player, new Playing(id, d, ageMs));
    }

    public static Playing playing(UUID player) { return player == null ? null : PLAYING.get(player); }

    /**
     * Kąty części modelu Minecrafta (radiany): głowa, prawa ręka, lewa ręka, prawa noga, lewa noga – po 3 (pitch, yaw, roll).
     * Ostatnia liczba: 1 – nadpisać też nogi (emotka bez ruchu). Null – brak emotki.
     */
    public static float[] modelAngles(UUID player) {
        Playing p = playing(player);
        if (p == null) return null;
        float[] e = p.pose();
        float[] r = new float[16];
        for (int part = 0; part < 5; part++) {
            int s = 6 + part * 3;
            r[part * 3] = e[s];          // obrót wokół x – bez zmian
            r[part * 3 + 1] = -e[s + 1]; // model Minecrafta ma odwrócone osie y i z
            r[part * 3 + 2] = -e[s + 2];
        }
        r[15] = p.data.move ? 0 : 1;
        return r;
    }

    /** Przesunięcie (bloki) i obrót całej postaci (kwaternion x, y, z, w) w układzie po obrocie ciała. Null – brak. */
    public static float[] rootTransform(UUID player) {
        Playing p = playing(player);
        if (p == null) return null;
        float[] e = p.pose();
        float tx = -e[0] / 16f, ty = e[1] / 16f, tz = -e[2] / 16f;
        float ax = -e[3], ay = e[4], az = -e[5];
        if (Math.abs(tx) + Math.abs(ty) + Math.abs(tz) + Math.abs(ax) + Math.abs(ay) + Math.abs(az) < 1e-4f) return null;
        // kwaternion z kątów Eulera XYZ (Rx·Ry·Rz)
        double cx = Math.cos(ax / 2), sx = Math.sin(ax / 2), cy = Math.cos(ay / 2), sy = Math.sin(ay / 2), cz = Math.cos(az / 2), sz = Math.sin(az / 2);
        float qx = (float) (sx * cy * cz + cx * sy * sz);
        float qy = (float) (cx * sy * cz - sx * cy * sz);
        float qz = (float) (cx * cy * sz + sx * sy * cz);
        float qw = (float) (cx * cy * cz - sx * sy * sz);
        return new float[]{tx, ty, tz, qx, qy, qz, qw};
    }

    /** Wywoływane co tick: emotka bez ruchu kończy się, gdy gracz idzie. */
    public static void moved(UUID player) {
        Playing p = playing(player);
        if (p != null && !p.data.move) stop(player);
    }
}
