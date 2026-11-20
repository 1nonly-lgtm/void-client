package dev.oneonly.voidclient.core.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Model kosmetyku wypieczony z podglądu w launcherze (tools: bake). Ładowany raz z zasobów moda. */
public final class CosmeticModel {
    public static final int FPS = 12, FRAMES = 48;

    static final class Mat {
        boolean basic, add;
    }

    static final class Node {
        int parent, geom = -1;
        boolean sprite;
        Mat[] mats;
        /** Klatki: pos(3), quat(4), scale(3), visible(1); materiały: kolor, przezroczystość, tekstura, obrót. */
        float[][] tr;
        int[][] color;
        float[][] alpha;
        String[][] tex;
    }

    static final class Geom {
        float[] p, n, u;
        int[] m;
    }

    String anchor;
    Node[] nodes;
    Geom[] geoms;

    private static final Map<String, CosmeticModel> CACHE = new ConcurrentHashMap<>();
    private static final CosmeticModel MISSING = new CosmeticModel();

    /** Model kategorii/id albo null, gdy nie ma takiego. */
    public static CosmeticModel get(String cat, String id) {
        if (id == null) return null;
        CosmeticModel m = CACHE.computeIfAbsent(cat + "/" + id, k -> load("/assets/voidclient/cosmetics/" + k + ".json"));
        return m == MISSING ? null : m;
    }

    static CosmeticModel load(String path) {
        try (InputStream in = CosmeticModel.class.getResourceAsStream(path)) {
            if (in == null) return MISSING;
            return parse(JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Throwable t) {
            return MISSING;
        }
    }

    static CosmeticModel parse(JsonObject o) {
        CosmeticModel cm = new CosmeticModel();
        cm.anchor = o.has("anchor") ? o.get("anchor").getAsString() : "spin";
        JsonArray gs = o.getAsJsonArray("geoms");
        cm.geoms = new Geom[gs.size()];
        for (int i = 0; i < gs.size(); i++) {
            JsonObject g = gs.get(i).getAsJsonObject();
            Geom ge = new Geom();
            ge.p = floats(g.getAsJsonArray("p"));
            ge.n = floats(g.getAsJsonArray("n"));
            ge.u = floats(g.getAsJsonArray("u"));
            JsonArray m = g.getAsJsonArray("m");
            ge.m = new int[m.size()];
            for (int k = 0; k < ge.m.length; k++) ge.m[k] = m.get(k).getAsInt();
            cm.geoms[i] = ge;
        }
        JsonArray ns = o.getAsJsonArray("nodes");
        cm.nodes = new Node[ns.size()];
        for (int i = 0; i < ns.size(); i++) {
            JsonObject n = ns.get(i).getAsJsonObject();
            Node nd = new Node();
            nd.parent = n.get("parent").getAsInt();
            if (n.has("geom")) nd.geom = n.get("geom").getAsInt();
            nd.sprite = n.has("sprite");
            if (n.has("mat")) {
                JsonArray ms = n.getAsJsonArray("mat");
                nd.mats = new Mat[ms.size()];
                for (int k = 0; k < ms.size(); k++) {
                    JsonObject mo = ms.get(k).getAsJsonObject();
                    Mat mt = new Mat();
                    mt.basic = mo.get("basic").getAsBoolean();
                    mt.add = mo.get("add").getAsBoolean();
                    nd.mats[k] = mt;
                }
            }
            JsonArray fs = n.getAsJsonArray("f");
            int fc = fs.size();
            nd.tr = new float[fc][11];
            if (nd.mats != null) {
                nd.color = new int[fc][nd.mats.length];
                nd.alpha = new float[fc][nd.mats.length];
                nd.tex = new String[fc][nd.mats.length];
            }
            for (int f = 0; f < fc; f++) {
                JsonArray fr = fs.get(f).getAsJsonArray();
                for (int k = 0; k < 11; k++) nd.tr[f][k] = fr.get(k).getAsFloat();
                if (nd.mats != null && fr.size() > 11) {
                    JsonArray mm = fr.get(11).getAsJsonArray();
                    for (int k = 0; k < nd.mats.length && k < mm.size(); k++) {
                        JsonArray e = mm.get(k).getAsJsonArray();
                        nd.color[f][k] = e.get(0).getAsInt();
                        nd.alpha[f][k] = e.get(1).getAsFloat();
                        JsonElement t = e.get(2);
                        nd.tex[f][k] = t == null || t.isJsonNull() ? null : t.getAsString();
                    }
                }
            }
            cm.nodes[i] = nd;
        }
        return cm;
    }

    static float[] floats(JsonArray a) {
        float[] r = new float[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
        return r;
    }
}
